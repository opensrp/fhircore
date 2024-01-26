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

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.fhir.sync.SyncJobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlin.time.Duration
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.geowidget.GeoWidgetConfiguration
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_REMOTE
import org.smartregister.fhircore.engine.configuration.navigation.NavigationConfiguration
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfiguration
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.task.FhirCarePlanGenerator
import org.smartregister.fhircore.engine.task.FhirCompleteCarePlanWorker
import org.smartregister.fhircore.engine.task.FhirResourceExpireWorker
import org.smartregister.fhircore.engine.task.FhirTaskStatusUpdateWorker
import org.smartregister.fhircore.engine.ui.bottomsheet.RegisterBottomSheetFragment
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.decodeToBitmap
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.fetchLanguages
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.engine.util.extension.isDeviceOnline
import org.smartregister.fhircore.engine.util.extension.refresh
import org.smartregister.fhircore.engine.util.extension.setAppLocale
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.engine.util.extension.tryParse
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.ui.report.measure.worker.MeasureReportMonthPeriodWorker
import org.smartregister.fhircore.quest.ui.shared.QuestionnaireHandler
import org.smartregister.fhircore.quest.ui.shared.models.QuestionnaireSubmission
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent
import org.smartregister.fhircore.quest.util.extensions.schedulePeriodically

@HiltViewModel
class AppMainViewModel
@Inject
constructor(
  val secureSharedPreference: SecureSharedPreference,
  val syncBroadcaster: SyncBroadcaster,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val configurationRegistry: ConfigurationRegistry,
  val registerRepository: RegisterRepository,
  val dispatcherProvider: DispatcherProvider,
  val workManager: WorkManager,
  val fhirCarePlanGenerator: FhirCarePlanGenerator,
) : ViewModel() {

  val appMainUiState: MutableState<AppMainUiState> =
    mutableStateOf(
      appMainUiStateOf(
        navigationConfiguration =
          NavigationConfiguration(
            sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, "")!!,
          ),
      ),
    )

  private val simpleDateFormat = SimpleDateFormat(SYNC_TIMESTAMP_OUTPUT_FORMAT, Locale.getDefault())
  private val registerCountMap: SnapshotStateMap<String, Long> = mutableStateMapOf()

  val applicationConfiguration: ApplicationConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(ConfigType.Application, paramsMap = emptyMap())
  }

  val navigationConfiguration: NavigationConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(ConfigType.Navigation)
  }

  private val measureReportConfigurations: List<MeasureReportConfiguration> by lazy {
    configurationRegistry.retrieveConfigurations(ConfigType.MeasureReport)
  }

  fun retrieveIconsAsBitmap() {
    navigationConfiguration.clientRegisters
      .asSequence()
      .filter {
        it.menuIconConfig != null &&
          it.menuIconConfig?.type == ICON_TYPE_REMOTE &&
          !it.menuIconConfig!!.reference.isNullOrEmpty()
      }
      .forEach {
        val resourceId = it.menuIconConfig!!.reference!!.extractLogicalIdUuid()
        viewModelScope.launch(dispatcherProvider.io()) {
          registerRepository.loadResource<Binary>(resourceId)?.let { binary ->
            it.menuIconConfig!!.decodedBitmap = binary.data.decodeToBitmap()
          }
        }
      }
  }

  suspend fun retrieveAppMainUiState(refreshAll: Boolean = true) {
    if (refreshAll) {
      appMainUiState.value =
        appMainUiStateOf(
          appTitle = applicationConfiguration.appTitle,
          currentLanguage = loadCurrentLanguage(),
          username = secureSharedPreference.retrieveSessionUsername() ?: "",
          lastSyncTime = retrieveLastSyncTimestamp() ?: "",
          languages = configurationRegistry.fetchLanguages(),
          navigationConfiguration = navigationConfiguration,
          registerCountMap = registerCountMap,
        )
    }

    // Count data for configured registers by populating the register count map
    viewModelScope.launch {
      navigationConfiguration.run {
        clientRegisters.countRegisterData()
        bottomSheetRegisters?.registers?.countRegisterData()
      }
    }
  }

  fun onEvent(event: AppMainEvent) {
    when (event) {
      is AppMainEvent.SwitchLanguage -> {
        sharedPreferencesHelper.write(SharedPreferenceKey.LANG.name, event.language.tag)
        event.context.run {
          setAppLocale(event.language.tag)
          getActivity()?.refresh()
        }
      }
      is AppMainEvent.SyncData -> {
        if (event.context.isDeviceOnline()) {
          viewModelScope.launch { syncBroadcaster.runOneTimeSync() }
        } else {
          event.context.showToast(event.context.getString(R.string.sync_failed), Toast.LENGTH_LONG)
        }
      }
      is AppMainEvent.OpenRegistersBottomSheet -> displayRegisterBottomSheet(event)
      is AppMainEvent.UpdateSyncState -> {
        if (event.state is SyncJobStatus.Succeeded) {
          sharedPreferencesHelper.write(
            SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name,
            formatLastSyncTimestamp(event.state.timestamp),
          )
          viewModelScope.launch { retrieveAppMainUiState() }
        }
      }
      is AppMainEvent.TriggerWorkflow ->
        event.navMenu.actions?.handleClickEvent(
          navController = event.navController,
          resourceData = null,
          navMenu = event.navMenu,
        )
      is AppMainEvent.OpenProfile -> {
        val args =
          bundleOf(
            NavigationArg.PROFILE_ID to event.profileId,
            NavigationArg.RESOURCE_ID to event.resourceId,
            NavigationArg.RESOURCE_CONFIG to event.resourceConfig,
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
          },
          title = event.title,
        )
        .run { show(activity.supportFragmentManager, RegisterBottomSheetFragment.TAG) }
    }
  }

  fun launchFamilyRegistrationWithLocationId(
    context: Context,
    locationId: String,
    questionnaireConfig: QuestionnaireConfig,
  ) {
    viewModelScope.launch {
      val prePopulateLocationIdParameter =
        ActionParameter(
          key = "locationId",
          paramType = ActionParameterType.PREPOPULATE,
          dataType = Enumerations.DataType.STRING,
          resourceType = ResourceType.Location,
          value = locationId,
          linkId = "household-location-reference",
        )
      if (context is QuestionnaireHandler) {
        context.launchQuestionnaire(
          context = context,
          questionnaireConfig = questionnaireConfig,
          actionParams = listOf(prePopulateLocationIdParameter),
        )
      }
    }
  }

  private suspend fun List<NavigationMenuConfig>.countRegisterData() {
    // Set count for registerId against its value. Use action Id; otherwise default to menu id
    return this.filter { it.showCount }
      .forEach { menuConfig ->
        val countAction =
          menuConfig.actions?.find { actionConfig ->
            actionConfig.trigger == ActionTrigger.ON_COUNT
          }
        registerCountMap[countAction?.id ?: menuConfig.id] =
          registerRepository.countRegisterData(menuConfig.id)
      }
  }

  private fun loadCurrentLanguage() =
    Locale.forLanguageTag(
        sharedPreferencesHelper.read(SharedPreferenceKey.LANG.name, Locale.ENGLISH.toLanguageTag())
          ?: Locale.ENGLISH.toLanguageTag(),
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
    resourceId: String,
  ) {
    val geoWidgetConfiguration =
      configurationRegistry.retrieveConfiguration<GeoWidgetConfiguration>(
        ConfigType.GeoWidget,
        geoWidgetConfigId,
      )
    onEvent(
      AppMainEvent.OpenProfile(
        navController = navController,
        profileId = geoWidgetConfiguration.profileId,
        resourceId = resourceId,
        resourceConfig = geoWidgetConfiguration.resourceConfig,
      ),
    )
  }

  /** This function is used to schedule tasks that are intended to run periodically */
  fun schedulePeriodicJobs() {
    workManager.run {
      schedulePeriodically<FhirTaskStatusUpdateWorker>(
        workId = FhirTaskStatusUpdateWorker.WORK_ID,
        duration = Duration.tryParse(applicationConfiguration.taskStatusUpdateJobDuration),
        requiresNetwork = false,
      )

      schedulePeriodically<FhirResourceExpireWorker>(
        workId = FhirResourceExpireWorker.WORK_ID,
        duration = Duration.tryParse(applicationConfiguration.taskExpireJobDuration),
        requiresNetwork = false,
      )

      schedulePeriodically<FhirCompleteCarePlanWorker>(
        workId = FhirCompleteCarePlanWorker.WORK_ID,
        duration = Duration.tryParse(applicationConfiguration.taskCompleteCarePlanJobDuration),
        requiresNetwork = false,
      )

      measureReportConfigurations.forEach { measureReportConfig ->
        measureReportConfig.scheduledGenerationDuration?.let { scheduledGenerationDuration ->
          schedulePeriodically<MeasureReportMonthPeriodWorker>(
            workId = "${MeasureReportMonthPeriodWorker.WORK_ID}-${measureReportConfig.id}",
            duration = Duration.tryParse(scheduledGenerationDuration),
            requiresNetwork = false,
            inputData =
              workDataOf(
                MeasureReportMonthPeriodWorker.MEASURE_REPORT_CONFIG_ID to measureReportConfig.id,
              ),
          )
        }
      }
    }
  }

  suspend fun onQuestionnaireSubmission(questionnaireSubmission: QuestionnaireSubmission) {
    questionnaireSubmission.questionnaireConfig.taskId?.let { taskId ->
      val status: Task.TaskStatus =
        when (questionnaireSubmission.questionnaireResponse.status) {
          QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS -> Task.TaskStatus.INPROGRESS
          QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED -> Task.TaskStatus.COMPLETED
          else -> Task.TaskStatus.COMPLETED
        }

      withContext(dispatcherProvider.io()) {
        fhirCarePlanGenerator.updateTaskDetailsByResourceId(
          id = taskId.extractLogicalIdUuid(),
          status = status,
        )
      }
    }
  }

  companion object {
    const val SYNC_TIMESTAMP_INPUT_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    const val SYNC_TIMESTAMP_OUTPUT_FORMAT = "MMM d, hh:mm aa"
  }
}
