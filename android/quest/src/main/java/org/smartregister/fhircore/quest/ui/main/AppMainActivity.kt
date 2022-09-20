/*
 * Copyright 2021 Ona Systems, Inc
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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.material.ExperimentalMaterialApi
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.google.android.fhir.sync.State
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.sync.SyncListenerManager
import org.smartregister.fhircore.engine.task.FhirCarePlanGenerator
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.geowidget.model.GeoWidgetEvent
import org.smartregister.fhircore.geowidget.screens.GeoWidgetViewModel
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity
import timber.log.Timber

@AndroidEntryPoint
@ExperimentalMaterialApi
open class AppMainActivity : BaseMultiLanguageActivity(), OnSyncListener {

  @Inject lateinit var fhirCarePlanGenerator: FhirCarePlanGenerator

  @Inject lateinit var configService: ConfigService

  @Inject lateinit var dispatcherProvider: DefaultDispatcherProvider

  @Inject lateinit var syncListenerManager: SyncListenerManager

  @Inject lateinit var syncBroadcaster: SyncBroadcaster

  val appMainViewModel by viewModels<AppMainViewModel>()

  val geoWidgetViewModel by viewModels<GeoWidgetViewModel>()

  lateinit var navHostFragment: NavHostFragment

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(FragmentContainerView(this).apply { id = R.id.nav_host })
    val topMenuConfig = appMainViewModel.navigationConfiguration.clientRegisters.first()
    val topMenuConfigId =
      topMenuConfig.actions?.find { it.trigger == ActionTrigger.ON_CLICK }?.id ?: topMenuConfig.id

    navHostFragment =
      NavHostFragment.create(
        R.navigation.application_nav_graph,
        bundleOf(
          NavigationArg.SCREEN_TITLE to topMenuConfig.display,
          NavigationArg.REGISTER_ID to topMenuConfigId
        )
      )

    supportFragmentManager
      .beginTransaction()
      .replace(R.id.nav_host, navHostFragment)
      .setPrimaryNavigationFragment(navHostFragment)
      .commit()

    geoWidgetViewModel.geoWidgetEventLiveData.observe(this) { geoWidgetEvent ->
      when (geoWidgetEvent) {
        is GeoWidgetEvent.OpenProfile -> {
          appMainViewModel.launchProfileFromGeoWidget(
            navHostFragment.navController,
            geoWidgetEvent.geoWidgetConfiguration.id,
            geoWidgetEvent.data
          )
        }
        is GeoWidgetEvent.RegisterClient ->
          appMainViewModel.launchFamilyRegistrationWithLocationId(
            context = this,
            locationId = geoWidgetEvent.data,
            questionnaireConfig = geoWidgetEvent.questionnaire
          )
      }
    }

    // Register sync listener then run sync in that order
    syncListenerManager.registerSyncListener(this, lifecycle)
    syncBroadcaster.runSync()

    configService.scheduleFhirTaskPlanWorker(this)
  }

  @Suppress("DEPRECATION")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (resultCode == Activity.RESULT_OK)
      data?.getStringExtra(QuestionnaireActivity.QUESTIONNAIRE_BACK_REFERENCE_KEY)?.let {
        lifecycleScope.launch(dispatcherProvider.io()) {
          when {
            it.startsWith(ResourceType.Task.name) ->
              fhirCarePlanGenerator.completeTask(it.extractLogicalIdUuid())
          }
        }
      }
  }

  override fun onSync(state: State) {
    Timber.i("Sync state received is $state")
    when (state) {
      is State.Started -> {
        showToast(getString(R.string.syncing))
        appMainViewModel.onEvent(
          AppMainEvent.UpdateSyncState(state, getString(R.string.syncing_initiated))
        )
      }
      is State.InProgress -> {
        Timber.d("Syncing in progress: Resource type ${state.resourceType?.name}")
        appMainViewModel.onEvent(
          AppMainEvent.UpdateSyncState(state, getString(R.string.syncing_in_progress))
        )
      }
      is State.Glitch -> {
        appMainViewModel.onEvent(
          AppMainEvent.UpdateSyncState(state, appMainViewModel.retrieveLastSyncTimestamp())
        )
        Timber.w(state.exceptions.joinToString { it.exception.message.toString() })
      }
      is State.Failed -> {
        showToast(getString(R.string.sync_failed))
        appMainViewModel.onEvent(
          AppMainEvent.UpdateSyncState(
            state,
            if (!appMainViewModel.retrieveLastSyncTimestamp().isNullOrEmpty())
              appMainViewModel.retrieveLastSyncTimestamp()
            else getString(R.string.syncing_failed)
          )
        )
        Timber.e(state.result.exceptions.joinToString { it.exception.message.toString() })
      }
      is State.Finished -> {
        showToast(getString(org.smartregister.fhircore.engine.R.string.sync_completed))
        appMainViewModel.run {
          onEvent(
            AppMainEvent.UpdateSyncState(state, formatLastSyncTimestamp(state.result.timestamp))
          )
        }
      }
    }
  }
}
