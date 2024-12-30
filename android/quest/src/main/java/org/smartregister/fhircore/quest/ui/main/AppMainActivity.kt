/*
 * Copyright 2021-2024 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.quest.ui.main

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material.ExperimentalMaterialApi
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.fhir.sync.CurrentSyncJobStatus
import com.google.android.fhir.sync.SyncJobStatus
import com.google.android.fhir.sync.SyncOperation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import io.sentry.android.navigation.SentryNavigationListener
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.app.LocationLogOptions
import org.smartregister.fhircore.engine.configuration.app.SyncStrategy
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.datastore.ProtoDataStore
import org.smartregister.fhircore.engine.datastore.syncLocationIdsProtoStore
import org.smartregister.fhircore.engine.domain.model.LauncherType
import org.smartregister.fhircore.engine.rulesengine.services.LocationCoordinate
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncListenerManager
import org.smartregister.fhircore.engine.ui.base.AlertDialogButton
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.util.extension.isDeviceOnline
import org.smartregister.fhircore.engine.util.extension.parcelable
import org.smartregister.fhircore.engine.util.extension.serializable
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.engine.util.location.LocationUtils
import org.smartregister.fhircore.engine.util.location.PermissionUtils
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.event.AppEvent
import org.smartregister.fhircore.quest.event.EventBus
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.quest.ui.shared.QuestionnaireHandler
import org.smartregister.fhircore.quest.ui.shared.models.QuestionnaireSubmission
import timber.log.Timber

@AndroidEntryPoint
@ExperimentalMaterialApi
open class AppMainActivity : BaseMultiLanguageActivity(), QuestionnaireHandler, OnSyncListener {

  @Inject lateinit var syncListenerManager: SyncListenerManager

  @Inject lateinit var protoDataStore: ProtoDataStore

  @Inject lateinit var eventBus: EventBus
  val appMainViewModel by viewModels<AppMainViewModel>()
  private val sentryNavListener =
    SentryNavigationListener(enableNavigationBreadcrumbs = true, enableNavigationTracing = true)
  private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>
  private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
  private lateinit var fusedLocationClient: FusedLocationProviderClient

  override val startForResult =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
      if (activityResult.resultCode == Activity.RESULT_OK) {
        lifecycleScope.launch { onSubmitQuestionnaire(activityResult) }
      }
    }

  /**
   * When the NavHostFragment is inflated using FragmentContainerView, if you attempt to use
   * findNavController in the onCreate() of the Activity, the nav controller cannot be found. This
   * is because when the fragment is inflated in the constructor of FragmentContainerView, the
   * fragmentManager is in the INITIALIZING state, and therefore the added fragment only goes up to
   * initializing. For the nav controller to be properly set, the fragment view needs to be created
   * and onViewCreated() needs to be dispatched, which does not happen until the ACTIVITY_CREATED
   * state. As a workaround retrieve the navController from the [NavHostFragment]
   */
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setupLocationServices()
    setContentView(R.layout.activity_main)

    val startDestinationConfig =
      appMainViewModel.applicationConfiguration.navigationStartDestination
    val startDestinationArgs =
      when (startDestinationConfig.launcherType) {
        LauncherType.REGISTER -> {
          val topMenuConfig = appMainViewModel.navigationConfiguration.clientRegisters.first()
          val clickAction = topMenuConfig.actions?.find { it.trigger == ActionTrigger.ON_CLICK }
          bundleOf(
            NavigationArg.SCREEN_TITLE to
              if (startDestinationConfig.screenTitle.isNullOrEmpty()) {
                topMenuConfig.display
              } else startDestinationConfig.screenTitle,
            NavigationArg.REGISTER_ID to
              if (startDestinationConfig.id.isNullOrEmpty()) {
                clickAction?.id ?: topMenuConfig.id
              } else startDestinationConfig.id,
          )
        }
      }

    // Retrieve the navController directly from the NavHostFragment
    val navController =
      (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment).navController

    val graph =
      navController.navInflater.inflate(R.navigation.application_nav_graph).apply {
        val startDestination =
          when (appMainViewModel.applicationConfiguration.navigationStartDestination.launcherType) {
            LauncherType.REGISTER -> R.id.registerFragment
          }
        setStartDestination(startDestination)
      }

    navController.setGraph(graph, startDestinationArgs)

    // Register sync listener then run sync in that order
    syncListenerManager.registerSyncListener(this, lifecycle)

    // Setup the drawer and schedule jobs
    appMainViewModel.run {
      retrieveAppMainUiState()
      if (isDeviceOnline()) {
        // Do not schedule sync until location selected when strategy is RelatedEntityLocation
        // Use applicationConfiguration.usePractitionerAssignedLocationOnSync to identify
        // if we need to trigger sync based on assigned locations or not
        if (applicationConfiguration.syncStrategy.contains(SyncStrategy.RelatedEntityLocation)) {
          if (
            applicationConfiguration.usePractitionerAssignedLocationOnSync ||
              runBlocking { syncLocationIdsProtoStore.data.firstOrNull() }?.isNotEmpty() == true
          ) {
            schedulePeriodicSync()
          }
        } else {
          schedulePeriodicSync()
        }
      } else {
        showToast(
          getString(org.smartregister.fhircore.engine.R.string.sync_failed),
          Toast.LENGTH_LONG,
        )
      }
      schedulePeriodicJobs()
    }
  }

  override fun onResume() {
    super.onResume()
    findNavController(R.id.nav_host).addOnDestinationChangedListener(sentryNavListener)
  }

  override fun onPause() {
    super.onPause()
    findNavController(R.id.nav_host).removeOnDestinationChangedListener(sentryNavListener)
  }

  override fun onQuestionnaireLaunched(questionnaireConfig: QuestionnaireConfig) {
    // Data filter QRs are not persisted; reset filters when questionnaire is launched
    if (!questionnaireConfig.saveQuestionnaireResponse) {
      appMainViewModel.resetRegisterFilters.value = true
    }
  }

  override suspend fun onSubmitQuestionnaire(activityResult: ActivityResult) {
    if (activityResult.resultCode == RESULT_OK) {
      val questionnaireResponse: QuestionnaireResponse? =
        activityResult.data?.serializable(QuestionnaireActivity.QUESTIONNAIRE_RESPONSE)
          as QuestionnaireResponse?
      val extractedResourceIds =
        activityResult.data?.serializable(
          QuestionnaireActivity.QUESTIONNAIRE_SUBMISSION_EXTRACTED_RESOURCE_IDS,
        ) as List<IdType>? ?: emptyList()
      val questionnaireConfig =
        activityResult.data?.parcelable(QuestionnaireActivity.QUESTIONNAIRE_CONFIG)
          as QuestionnaireConfig?

      if (questionnaireConfig != null && questionnaireResponse != null) {
        eventBus.triggerEvent(
          AppEvent.OnSubmitQuestionnaire(
            QuestionnaireSubmission(
              questionnaireConfig = questionnaireConfig,
              questionnaireResponse = questionnaireResponse,
              extractedResourceIds = extractedResourceIds,
            ),
          ),
        )
      } else Timber.e("QuestionnaireConfig & QuestionnaireResponse are both null")
    }
  }

  private fun setupLocationServices() {
    if (
      appMainViewModel.applicationConfiguration.logGpsLocation.contains(
        LocationLogOptions.CALCULATE_DISTANCE_RULE_EXECUTOR,
      )
    ) {
      fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

      if (!LocationUtils.isLocationEnabled(this)) {
        openLocationServicesSettings()
      }

      if (!hasLocationPermissions()) {
        launchLocationPermissionsDialog()
      }

      if (LocationUtils.isLocationEnabled(this) && hasLocationPermissions()) {
        fetchLocation()
      }
    }
  }

  fun hasLocationPermissions(): Boolean {
    return PermissionUtils.checkPermissions(
      this,
      listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
      ),
    )
  }

  private fun openLocationServicesSettings() {
    activityResultLauncher =
      PermissionUtils.getStartActivityForResultLauncher(this) { resultCode, _ ->
        if (resultCode == RESULT_OK || hasLocationPermissions()) {
          Timber.d("Location or permissions successfully enabled")
        }
      }

    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    showLocationSettingsDialog(intent)
  }

  private fun showLocationSettingsDialog(intent: Intent) {
    AlertDialog.Builder(this)
      .setMessage(getString(R.string.location_services_disabled))
      .setCancelable(true)
      .setPositiveButton(getString(R.string.yes)) { _, _ -> activityResultLauncher.launch(intent) }
      .setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.cancel() }
      .show()
  }

  fun launchLocationPermissionsDialog() {
    locationPermissionLauncher =
      PermissionUtils.getLocationPermissionLauncher(
        this,
        onFineLocationPermissionGranted = { fetchLocation() },
        onCoarseLocationPermissionGranted = { fetchLocation() },
        onLocationPermissionDenied = {
          Toast.makeText(
              this,
              getString(R.string.location_permissions_denied),
              Toast.LENGTH_SHORT,
            )
            .show()
        },
      )

    locationPermissionLauncher.launch(
      arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
      ),
    )
  }

  private fun fetchLocation() {
    val context = this
    lifecycleScope.launch {
      val retrievedLocation =
        if (PermissionUtils.hasFineLocationPermissions(context)) {
          LocationUtils.getAccurateLocation(fusedLocationClient)
        } else if (PermissionUtils.hasCoarseLocationPermissions(context)) {
          LocationUtils.getApproximateLocation(fusedLocationClient)
        } else {
          null
        }
      retrievedLocation?.let {
        protoDataStore.writeLocationCoordinates(
          LocationCoordinate(it.latitude, it.longitude, it.altitude, Instant.now()),
        )
      }
      if (retrievedLocation == null) {
        this@AppMainActivity.showToast("Failed to get GPS location", Toast.LENGTH_LONG)
      }
    }
  }

  override fun onSync(syncJobStatus: CurrentSyncJobStatus) {
    when (syncJobStatus) {
      is CurrentSyncJobStatus.Succeeded ->
        appMainViewModel.run {
          onEvent(
            AppMainEvent.UpdateSyncState(
              state = syncJobStatus,
              lastSyncTime = formatLastSyncTimestamp(syncJobStatus.timestamp),
            ),
          )
          appMainViewModel.updateAppDrawerUIState(currentSyncJobStatus = syncJobStatus)
        }
      is CurrentSyncJobStatus.Failed ->
        appMainViewModel.run {
          onEvent(
            AppMainEvent.UpdateSyncState(
              state = syncJobStatus,
              lastSyncTime = formatLastSyncTimestamp(syncJobStatus.timestamp),
            ),
          )
          updateAppDrawerUIState(currentSyncJobStatus = syncJobStatus)
        }
      else -> {
        // Do Nothing
      }
    }
  }

  private fun overrideOnBackPressListener() {
    onBackPressedDispatcher.addCallback(
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment)
          if (navHostFragment.childFragmentManager.backStackEntryCount == 0) {
            AlertDialogue.showAlert(
              this@AppMainActivity,
              alertIntent = AlertIntent.CONFIRM,
              title = getString(R.string.exit_app),
              message = getString(R.string.exit_app_message),
              cancellable = false,
              confirmButton =
                AlertDialogButton(
                  listener = { finish() },
                ),
              neutralButton =
                AlertDialogButton(
                  listener = { dialog -> dialog.dismiss() },
                ),
            )
          } else {
            navHostFragment.navController.navigateUp()
          }
        }
      },
    )
  }
}
