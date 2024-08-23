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

import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.CurrentSyncJobStatus
import com.google.android.fhir.sync.SyncJobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlin.time.Duration
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_REMOTE
import org.smartregister.fhircore.engine.configuration.navigation.NavigationConfiguration
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfiguration
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.sync.CustomSyncWorker
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
import org.smartregister.fhircore.engine.util.extension.countUnSyncedResources
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
import org.smartregister.fhircore.quest.ui.shared.models.AppDrawerUIState
import org.smartregister.fhircore.quest.ui.shared.models.QuestionnaireSubmission
import org.smartregister.fhircore.quest.util.extensions.decodeBinaryResourcesToBitmap
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
  val fhirEngine: FhirEngine,
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

  val appDrawerUiState = mutableStateOf(AppDrawerUIState())

  val resetRegisterFilters = MutableLiveData(false)

  val unSyncedResourcesCount = mutableIntStateOf(0)

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
      .decodeBinaryResourcesToBitmap(
        viewModelScope,
        registerRepository,
        configurationRegistry.decodedImageMap,
      )
  }

  fun retrieveAppMainUiState(refreshAll: Boolean = true) {
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

    countRegisterData()
  }

  fun countRegisterData() {
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
      is AppMainEvent.CancelSyncData -> {
        viewModelScope.launch {
          workManager.cancelUniqueWork(
            "org.smartregister.fhircore.engine.sync.AppSyncWorker-oneTimeSync",
          )
          updateAppDrawerUIState(currentSyncJobStatus = CurrentSyncJobStatus.Cancelled)
        }
      }
      is AppMainEvent.OpenRegistersBottomSheet -> displayRegisterBottomSheet(event)
      is AppMainEvent.UpdateSyncState -> {
        if (event.state is CurrentSyncJobStatus.Succeeded) {
          sharedPreferencesHelper.write(
            SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name,
            formatLastSyncTimestamp(event.state.timestamp),
          )
          retrieveAppMainUiState()
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

      schedulePeriodically<CustomSyncWorker>(
        workId = CustomSyncWorker.WORK_ID,
        repeatInterval = applicationConfiguration.syncInterval,
        initialDelay = 0,
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

  fun schedulePeriodicSync() {
    viewModelScope.launch {
      syncBroadcaster.schedulePeriodicSync(applicationConfiguration.syncInterval)
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

  fun calculatePercentageProgress(
    progressSyncJobStatus: SyncJobStatus.InProgress,
  ): Int {
    val totalRecordsOverall =
      sharedPreferencesHelper.read(
        SharedPreferencesHelper.PREFS_SYNC_PROGRESS_TOTAL +
          progressSyncJobStatus.syncOperation.name,
        1L,
      )
    val isProgressTotalLess = progressSyncJobStatus.total <= totalRecordsOverall
    val currentProgress: Int
    val currentTotalRecords =
      if (isProgressTotalLess) {
        currentProgress =
          totalRecordsOverall.toInt() - progressSyncJobStatus.total +
            progressSyncJobStatus.completed
        totalRecordsOverall.toInt()
      } else {
        sharedPreferencesHelper.write(
          SharedPreferencesHelper.PREFS_SYNC_PROGRESS_TOTAL +
            progressSyncJobStatus.syncOperation.name,
          progressSyncJobStatus.total.toLong(),
        )
        currentProgress = progressSyncJobStatus.completed
        progressSyncJobStatus.total
      }

    return getSyncProgress(currentProgress, currentTotalRecords)
  }

  fun updateAppDrawerUIState(
    isSyncUpload: Boolean? = null,
    currentSyncJobStatus: CurrentSyncJobStatus?,
    percentageProgress: Int? = null,
  ) {
    appDrawerUiState.value =
      AppDrawerUIState(
        isSyncUpload = isSyncUpload,
        currentSyncJobStatus = currentSyncJobStatus,
        percentageProgress = percentageProgress,
      )
  }

  fun updateUnSyncedResourcesCount() {
    viewModelScope.launch {
      unSyncedResourcesCount.intValue = async { fhirEngine.countUnSyncedResources() }.await().size
    }
  }

  private fun getSyncProgress(completed: Int, total: Int) =
    completed * 100 / if (total > 0) total else 1

  companion object {
    const val SYNC_TIMESTAMP_INPUT_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    const val SYNC_TIMESTAMP_OUTPUT_FORMAT = "MMM d, hh:mm aa"
  }
}
