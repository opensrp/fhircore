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

import android.accounts.AccountManager
import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.work.WorkManager
import com.google.android.fhir.sync.SyncJobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.geowidget.GeoWidgetConfiguration
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_REMOTE
import org.smartregister.fhircore.engine.configuration.navigation.NavigationConfiguration
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.task.FhirCarePlanGenerator
import org.smartregister.fhircore.engine.task.FhirTaskExpireWorker
import org.smartregister.fhircore.engine.ui.bottomsheet.RegisterBottomSheetFragment
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.decodeToBitmap
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.fetchLanguages
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.engine.util.extension.refresh
import org.smartregister.fhircore.engine.util.extension.setAppLocale
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.quest.ui.shared.QuestionnaireHandler
import org.smartregister.fhircore.quest.ui.shared.models.QuestionnaireSubmission
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent

@HiltViewModel
class AppMainViewModel
@Inject
constructor(
  val accountAuthenticator: AccountAuthenticator,
  val secureSharedPreference: SecureSharedPreference,
  val syncBroadcaster: SyncBroadcaster,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val configurationRegistry: ConfigurationRegistry,
  val registerRepository: RegisterRepository,
  val dispatcherProvider: DispatcherProvider,
  val workManager: WorkManager,
  val fhirCarePlanGenerator: FhirCarePlanGenerator
) : ViewModel() {

  val syncSharedFlow = MutableSharedFlow<SyncJobStatus>()

  val questionnaireSubmissionLiveData: MutableLiveData<QuestionnaireSubmission?> = MutableLiveData()

  val appMainUiState: MutableState<AppMainUiState> =
    mutableStateOf(
      appMainUiStateOf(
        navigationConfiguration =
          NavigationConfiguration(
            sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, "")!!
          )
      )
    )

  private val simpleDateFormat = SimpleDateFormat(SYNC_TIMESTAMP_OUTPUT_FORMAT, Locale.getDefault())

  val applicationConfiguration: ApplicationConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(ConfigType.Application)
  }

  val navigationConfiguration: NavigationConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(ConfigType.Navigation)
  }

  fun retrieveIconsAsBitmap() {
    navigationConfiguration.clientRegisters
      .filter { it.menuIconConfig != null && it.menuIconConfig?.type == ICON_TYPE_REMOTE }
      .forEach {
        val resourceId = it.menuIconConfig!!.reference!!.extractLogicalIdUuid()
        viewModelScope.launch(dispatcherProvider.io()) {
          registerRepository.loadResource<Binary>(resourceId)?.let { binary ->
            it.menuIconConfig!!.decodedBitmap = binary.data.decodeToBitmap()
          }
        }
      }
  }

  fun retrieveAppMainUiState() {
    appMainUiState.value =
      appMainUiStateOf(
        appTitle = applicationConfiguration.appTitle,
        currentLanguage = loadCurrentLanguage(),
        username = secureSharedPreference.retrieveSessionUsername() ?: "",
        lastSyncTime = retrieveLastSyncTimestamp() ?: "",
        languages = configurationRegistry.fetchLanguages(),
        navigationConfiguration = navigationConfiguration,
        registerCountMap = retrieveRegisterCountMap()
      )
  }

  fun onEvent(event: AppMainEvent) {
    when (event) {
      AppMainEvent.Logout -> accountAuthenticator.logout()
      is AppMainEvent.SwitchLanguage -> {
        sharedPreferencesHelper.write(SharedPreferenceKey.LANG.name, event.language.tag)
        event.context.run {
          setAppLocale(event.language.tag)
          getActivity()?.refresh()
        }
      }
      AppMainEvent.SyncData -> syncBroadcaster.runSync(syncSharedFlow)
      is AppMainEvent.RefreshAuthToken -> {
        viewModelScope.launch {
          accountAuthenticator.refreshSessionAuthToken().let { bundle ->
            if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
              // syncBroadcaster.runSync()
              return@let
            }
            accountAuthenticator.logout()
          }
        }
      }
      is AppMainEvent.OpenRegistersBottomSheet -> displayRegisterBottomSheet(event)
      is AppMainEvent.UpdateSyncState -> {
        when (event.state) {
          is SyncJobStatus.Finished, is SyncJobStatus.Failed -> {
            if (event.state is SyncJobStatus.Finished) {
              sharedPreferencesHelper.write(
                SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name,
                formatLastSyncTimestamp(event.state.timestamp)
              )
            }
            retrieveAppMainUiState()
          }
          else ->
            appMainUiState.value =
              appMainUiState.value.copy(lastSyncTime = event.lastSyncTime ?: "")
        }
      }
      is AppMainEvent.TriggerWorkflow ->
        event.navMenu.actions?.handleClickEvent(
          navController = event.navController,
          resourceData = null,
          navMenu = event.navMenu
        )
      is AppMainEvent.OpenProfile -> {
        val args =
          bundleOf(
            NavigationArg.PROFILE_ID to event.profileId,
            NavigationArg.RESOURCE_ID to event.resourceId,
            NavigationArg.RESOURCE_CONFIG to event.resourceConfig
          )
        event.navController.navigate(MainNavigationScreen.Profile.route, args)
      }
    }
  }

  private fun displayRegisterBottomSheet(event: AppMainEvent.OpenRegistersBottomSheet) {
    (event.navController.context.getActivity())?.let { activity ->
      RegisterBottomSheetFragment(
          navigationMenuConfigs = event.registersList,
          registerCountMap = appMainUiState.value.registerCountMap,
          menuClickListener = {
            onEvent(AppMainEvent.TriggerWorkflow(navController = event.navController, navMenu = it))
          }
        )
        .run { show(activity.supportFragmentManager, RegisterBottomSheetFragment.TAG) }
    }
  }

  private fun retrieveRegisterCountMap(): Map<String, Long> {
    val countsMap = mutableStateMapOf<String, Long>()
    viewModelScope.launch(dispatcherProvider.io()) {
      with(navigationConfiguration) {
        clientRegisters.setRegisterCount(countsMap)
        bottomSheetRegisters?.registers?.setRegisterCount(countsMap)
      }
    }
    return countsMap
  }

  fun launchFamilyRegistrationWithLocationId(
    context: Context,
    locationId: String,
    questionnaireConfig: QuestionnaireConfig
  ) {
    viewModelScope.launch {
      val location = registerRepository.loadResource<Location>(locationId)?.encodeResourceToString()
      if (context is QuestionnaireHandler)
        context.launchQuestionnaire<Any>(
          context = context,
          intentBundle =
            bundleOf(
              Pair(QuestionnaireActivity.QUESTIONNAIRE_POPULATION_RESOURCES, arrayListOf(location))
            ),
          questionnaireConfig = questionnaireConfig,
          computedValuesMap = null
        )
    }
  }

  private suspend fun List<NavigationMenuConfig>.setRegisterCount(
    countsMap: SnapshotStateMap<String, Long>
  ) {
    // Set count for registerId against its value. Use action Id; otherwise default to menu id
    this.filter { it.showCount }.forEach { menuConfig ->
      val countAction =
        menuConfig.actions?.find { actionConfig -> actionConfig.trigger == ActionTrigger.ON_COUNT }
      if (countAction != null) {
        countsMap[countAction.id ?: menuConfig.id] =
          registerRepository.countRegisterData(menuConfig.id)
      }
    }
  }

  private fun loadCurrentLanguage() =
    Locale.forLanguageTag(
        sharedPreferencesHelper.read(SharedPreferenceKey.LANG.name, Locale.ENGLISH.toLanguageTag())
          ?: Locale.ENGLISH.toLanguageTag()
      )
      .displayName

  fun formatLastSyncTimestamp(timestamp: OffsetDateTime): String {
    val syncTimestampFormatter =
      SimpleDateFormat(SYNC_TIMESTAMP_INPUT_FORMAT, Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
      }
    val parse: Date? = syncTimestampFormatter.parse(timestamp.toString())
    return if (parse == null) "" else simpleDateFormat.format(parse)
  }

  fun retrieveLastSyncTimestamp(): String? =
    sharedPreferencesHelper.read(SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name, null)

  fun launchProfileFromGeoWidget(
    navController: NavController,
    geoWidgetConfigId: String,
    resourceId: String
  ) {
    val geoWidgetConfiguration =
      configurationRegistry.retrieveConfiguration<GeoWidgetConfiguration>(
        ConfigType.GeoWidget,
        geoWidgetConfigId
      )
    onEvent(
      AppMainEvent.OpenProfile(
        navController = navController,
        profileId = geoWidgetConfiguration.profileId,
        resourceId = resourceId,
        resourceConfig = geoWidgetConfiguration.resourceConfig
      )
    )
  }

  fun updateLastSyncTimestamp(timestamp: OffsetDateTime) {
    sharedPreferencesHelper.write(
      SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name,
      formatLastSyncTimestamp(timestamp)
    )
  }

  /** This function is used to schedule tasks that are intended to run periodically */
  fun schedulePeriodicJobs() {

    // TODO Measure report generation is very expensive; affects app performance. Fix and revert.
    /* // Schedule job for generating measure report in the background
     MeasureReportWorker.scheduleMeasureReportWorker(workManager)
    */

    FhirTaskExpireWorker.schedule(
      workManager,
      sharedPreferencesHelper,
      applicationConfiguration.taskExpireJobRepeatIntervalMinutes
    )
  }

  suspend fun onQuestionnaireSubmit(questionnaireSubmission: QuestionnaireSubmission) {
    questionnaireSubmission.questionnaireConfig.taskId?.let { taskId ->
      val status: Task.TaskStatus =
        when (questionnaireSubmission.questionnaireResponse.status) {
          QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS -> Task.TaskStatus.INPROGRESS
          QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED -> Task.TaskStatus.COMPLETED
          else -> Task.TaskStatus.COMPLETED
        }
      withContext(dispatcherProvider.io()) {
        fhirCarePlanGenerator.transitionTaskTo(taskId.extractLogicalIdUuid(), status)
      }
    }
  }

  companion object {
    const val SYNC_TIMESTAMP_INPUT_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    const val SYNC_TIMESTAMP_OUTPUT_FORMAT = "MMM d, hh:mm aa"
  }
}
