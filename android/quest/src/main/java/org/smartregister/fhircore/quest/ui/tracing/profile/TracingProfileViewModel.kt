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

package org.smartregister.fhircore.quest.ui.tracing.profile

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.android.fhir.sync.State
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.data.local.register.AppRegisterRepository
import org.smartregister.fhircore.engine.domain.model.HealthStatus
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.isGuardianVisit
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaire
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaireForResult
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.data.patient.model.PatientPagingSourceState
import org.smartregister.fhircore.quest.data.register.RegisterPagingSource
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.navigation.OverflowMenuFactory
import org.smartregister.fhircore.quest.navigation.OverflowMenuHost
import org.smartregister.fhircore.quest.ui.patient.profile.childcontact.ChildContactPagingSource
import org.smartregister.fhircore.quest.ui.shared.models.ProfileViewData
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData
import org.smartregister.fhircore.quest.util.mappers.ProfileViewDataMapper
import org.smartregister.fhircore.quest.util.mappers.RegisterViewDataMapper

@HiltViewModel
class TracingProfileViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  syncBroadcaster: SyncBroadcaster,
  val overflowMenuFactory: OverflowMenuFactory,
  val registerRepository: AppRegisterRepository,
  val configurationRegistry: ConfigurationRegistry,
  val profileViewDataMapper: ProfileViewDataMapper,
  val registerViewDataMapper: RegisterViewDataMapper
) : ViewModel() {

  val appFeatureName = savedStateHandle.get<String>(NavigationArg.FEATURE)
  val healthModule =
    savedStateHandle.get<HealthModule>(NavigationArg.HEALTH_MODULE) ?: HealthModule.DEFAULT
  val patientId = savedStateHandle.get<String>(NavigationArg.PATIENT_ID) ?: ""
  val familyId = savedStateHandle.get<String>(NavigationArg.FAMILY_ID)

  var patientTracingProfileUiState: MutableState<TracingProfileUiState> =
    mutableStateOf(
      TracingProfileUiState(
        overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.TRACING_PROFILE)
      )
    )

  private val _patientProfileViewDataFlow =
    MutableStateFlow(ProfileViewData.PatientProfileViewData())
  val patientProfileViewData: StateFlow<ProfileViewData.PatientProfileViewData>
    get() = _patientProfileViewDataFlow.asStateFlow()

  var patientProfileData: ProfileData? = null

  val applicationConfiguration: ApplicationConfiguration
    get() = configurationRegistry.retrieveConfiguration(AppConfigClassification.APPLICATION)

  private val isClientVisit: MutableState<Boolean> = mutableStateOf(true)

  private val _showTracingOutcomes = MutableLiveData<Boolean>()
  val showTracingOutcomes: LiveData<Boolean>
    get() = _showTracingOutcomes

  init {
    syncBroadcaster.registerSyncListener(
      object : OnSyncListener {
        override fun onSync(state: State) {
          when (state) {
            is State.Finished, is State.Failed -> {
              fetchPatientProfileDataWithChildren()
            }
            else -> {}
          }
        }
      },
      viewModelScope
    )

    fetchPatientProfileDataWithChildren()
  }

  fun getOverflowMenuHostByPatientType(healthStatus: HealthStatus): OverflowMenuHost {
    return when (healthStatus) {
      HealthStatus.NEWLY_DIAGNOSED_CLIENT -> OverflowMenuHost.NEWLY_DIAGNOSED_PROFILE
      HealthStatus.CLIENT_ALREADY_ON_ART -> OverflowMenuHost.ART_CLIENT_PROFILE
      HealthStatus.EXPOSED_INFANT -> OverflowMenuHost.EXPOSED_INFANT_PROFILE
      HealthStatus.CHILD_CONTACT -> OverflowMenuHost.CHILD_CONTACT_PROFILE
      HealthStatus.SEXUAL_CONTACT -> OverflowMenuHost.SEXUAL_CONTACT_PROFILE
      HealthStatus.COMMUNITY_POSITIVE -> OverflowMenuHost.COMMUNITY_POSITIVE_PROFILE
      else -> OverflowMenuHost.PATIENT_PROFILE
    }
  }

  fun filterGuardianVisitTasks() {
    if (patientProfileData != null) {
      val hivPatientProfileData = patientProfileData as ProfileData.HivProfileData
      val newProfileData =
        hivPatientProfileData.copy(
          tasks =
            hivPatientProfileData.tasks.filter {
              it.isGuardianVisit(applicationConfiguration.patientTypeFilterTagViaMetaCodingSystem)
            }
        )
      _patientProfileViewDataFlow.value =
        profileViewDataMapper.transformInputToOutputModel(newProfileData) as
          ProfileViewData.PatientProfileViewData
    }
  }

  fun undoGuardianVisitTasksFilter() {
    if (patientProfileData != null) {
      _patientProfileViewDataFlow.value =
        profileViewDataMapper.transformInputToOutputModel(patientProfileData!!) as
          ProfileViewData.PatientProfileViewData
    }
  }

  fun showTracingOutcomes() {
    _showTracingOutcomes.value = true
  }

  fun onEvent(event: TracingProfileEvent) {
    val profile = patientProfileViewData.value

    when (event) {
      is TracingProfileEvent.LoadQuestionnaire ->
        event.context.launchQuestionnaire<QuestionnaireActivity>(
          event.questionnaireId,
          clientIdentifier = patientId,
          populationResources = profile.populationResources
        )
      is TracingProfileEvent.OverflowMenuClick -> {
        when (event.menuId) {
          R.id.edit_profile ->
            event.context.launchQuestionnaire<QuestionnaireActivity>(
              questionnaireId = EDIT_PROFILE_FORM,
              clientIdentifier = patientId,
              questionnaireType = QuestionnaireType.EDIT
            )
          R.id.tracing_history -> event.context.showToast("//todo Tracing History action here")
          else -> {}
        }
      }
      is TracingProfileEvent.OpenTaskForm ->
        event.context.launchQuestionnaireForResult<QuestionnaireActivity>(
          questionnaireId = event.taskFormId,
          clientIdentifier = patientId,
          backReference = event.taskId.asReference(ResourceType.Task).reference,
          populationResources = profile.populationResources
        )
    }
  }

  fun handleVisitType(isClientVisit: Boolean) {
    if (isClientVisit) {
      val updatedMenuItems =
        patientTracingProfileUiState.value.overflowMenuItems.map {
          when (it.id) {
            R.id.guardian_visit -> it.apply { hidden = false }
            R.id.client_visit -> it.apply { hidden = true }
            else -> it
          }
        }
      patientTracingProfileUiState.value =
        patientTracingProfileUiState.value.copy(overflowMenuItems = updatedMenuItems)
      undoGuardianVisitTasksFilter()
    } else {
      val updatedMenuItems =
        patientTracingProfileUiState.value.overflowMenuItems.map {
          when (it.id) {
            R.id.guardian_visit -> it.apply { hidden = true }
            R.id.client_visit -> it.apply { hidden = false }
            else -> it
          }
        }
      patientTracingProfileUiState.value =
        patientTracingProfileUiState.value.copy(overflowMenuItems = updatedMenuItems)
    }
  }

  fun fetchPatientProfileDataWithChildren() {
    if (patientId.isNotEmpty()) {
      viewModelScope.launch {
        registerRepository.loadPatientProfileData(appFeatureName, healthModule, patientId)?.let {
          patientProfileData = it
          _patientProfileViewDataFlow.value =
            profileViewDataMapper.transformInputToOutputModel(it) as
              ProfileViewData.PatientProfileViewData
          // refreshOverFlowMenu(healthModule = healthModule, patientProfile = it)
          paginateChildrenRegisterData(true)
          handleVisitType(isClientVisit.value)
        }
      }
    }
  }

  val paginatedChildrenRegisterData: MutableStateFlow<Flow<PagingData<RegisterViewData>>> =
    MutableStateFlow(emptyFlow())

  fun paginateChildrenRegisterData(loadAll: Boolean = true) {
    paginatedChildrenRegisterData.value =
      getPager(appFeatureName, healthModule, loadAll).flow.cachedIn(viewModelScope)
  }

  private fun getPager(
    appFeatureName: String?,
    healthModule: HealthModule,
    loadAll: Boolean = true
  ): Pager<Int, RegisterViewData> =
    Pager(
      config =
        PagingConfig(
          pageSize = RegisterPagingSource.DEFAULT_PAGE_SIZE,
          initialLoadSize = RegisterPagingSource.DEFAULT_INITIAL_LOAD_SIZE
        ),
      pagingSourceFactory = {
        ChildContactPagingSource(
            patientProfileViewData.value.otherPatients,
            registerRepository,
            registerViewDataMapper
          )
          .apply {
            setPatientPagingSourceState(
              PatientPagingSourceState(
                appFeatureName = appFeatureName,
                healthModule = healthModule,
                loadAll = loadAll,
                currentPage = 0
              )
            )
          }
      }
    )

  companion object {
    const val EDIT_PROFILE_FORM = "edit-patient-profile"
  }
}
