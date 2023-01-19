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

package org.smartregister.fhircore.quest.ui.patient.profile

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.android.fhir.sync.SyncJobStatus
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
import org.smartregister.fhircore.engine.appfeature.AppFeature
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
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.isGuardianVisit
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaire
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaireForResult
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.data.patient.model.PatientPagingSourceState
import org.smartregister.fhircore.quest.data.register.RegisterPagingSource
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.navigation.OverflowMenuFactory
import org.smartregister.fhircore.quest.navigation.OverflowMenuHost
import org.smartregister.fhircore.quest.ui.family.remove.member.RemoveFamilyMemberQuestionnaireActivity
import org.smartregister.fhircore.quest.ui.patient.profile.childcontact.ChildContactPagingSource
import org.smartregister.fhircore.quest.ui.patient.remove.HivPatientQuestionnaireActivity
import org.smartregister.fhircore.quest.ui.shared.models.ProfileViewData
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData
import org.smartregister.fhircore.quest.util.mappers.ProfileViewDataMapper
import org.smartregister.fhircore.quest.util.mappers.RegisterViewDataMapper

@HiltViewModel
class PatientProfileViewModel
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

  var patientProfileUiState: MutableState<PatientProfileUiState> =
    mutableStateOf(
      PatientProfileUiState(
        overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.PATIENT_PROFILE)
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

  fun completedTask(value: String) {
    patientProfileData?.let { data ->
      if (data is ProfileData.HivProfileData) {
        val patientData =
          data.copy(
            tasks =
              data.tasks.map { task ->
                if (task.reasonReference.extractId() == value) {
                  task.status = Task.TaskStatus.COMPLETED
                  task
                } else task
              }
          )
        _patientProfileViewDataFlow.value =
          profileViewDataMapper.transformInputToOutputModel(patientData) as
            ProfileViewData.PatientProfileViewData
        patientProfileData = patientData
      }
    }
  }

  init {
    syncBroadcaster.registerSyncListener(
      object : OnSyncListener {
        override fun onSync(state: SyncJobStatus) {
          when (state) {
            is SyncJobStatus.Finished, is SyncJobStatus.Failed -> {
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

  fun refreshOverFlowMenu(healthModule: HealthModule, patientProfile: ProfileData) {
    if (healthModule == HealthModule.HIV) {
      patientProfileUiState.value =
        PatientProfileUiState(
          overflowMenuFactory.retrieveOverflowMenuItems(
            getOverflowMenuHostByPatientType(
              (patientProfile as ProfileData.HivProfileData).healthStatus
            )
          )
        )
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

  fun onEvent(event: PatientProfileEvent) {
    val profile = patientProfileViewData.value

    when (event) {
      is PatientProfileEvent.LoadQuestionnaire ->
        event.context.launchQuestionnaire<QuestionnaireActivity>(
          event.questionnaireId,
          clientIdentifier = patientId,
          populationResources = profile.populationResources
        )
      is PatientProfileEvent.SeeAll -> {
        /* TODO(View all records in this category e.g. all medical history, tasks etc) */
      }
      is PatientProfileEvent.OverflowMenuClick -> {
        when (event.menuId) {
          R.id.individual_details ->
            event.context.launchQuestionnaire<QuestionnaireActivity>(
              questionnaireId = FAMILY_MEMBER_REGISTER_FORM,
              clientIdentifier = patientId,
              questionnaireType = QuestionnaireType.EDIT
            )
          R.id.guardian_visit -> {
            isClientVisit.value = false
            handleVisitType(false)
          }
          R.id.client_visit -> {
            isClientVisit.value = true
            handleVisitType(true)
          }
          222 -> {
            val urlParams =
              NavigationArg.bindArgumentsOf(
                Pair(NavigationArg.FEATURE, AppFeature.PatientManagement.name),
                Pair(NavigationArg.HEALTH_MODULE, healthModule.name),
                Pair(NavigationArg.PATIENT_ID, patientId)
              )

            event.navController.navigate(route = "tracing_tests$urlParams")
          }
          R.id.view_guardians -> {
            val commonParams =
              NavigationArg.bindArgumentsOf(
                Pair(NavigationArg.FEATURE, AppFeature.PatientManagement.name),
                Pair(NavigationArg.HEALTH_MODULE, HealthModule.HIV)
              )

            event.navController.navigate(
              route = "${MainNavigationScreen.PatientGuardians.route}/$patientId$commonParams"
            ) { launchSingleTop = true }
          }
          R.id.view_family -> {
            familyId?.let {
              val urlParams =
                NavigationArg.bindArgumentsOf(
                  Pair(NavigationArg.FEATURE, AppFeature.HouseholdManagement.name),
                  Pair(NavigationArg.HEALTH_MODULE, HealthModule.FAMILY),
                  Pair(NavigationArg.PATIENT_ID, it)
                )
              event.navController.navigate(
                route = MainNavigationScreen.FamilyProfile.route + urlParams
              )
            }
          }
          R.id.view_children -> {
            patientId.let {
              val urlParams =
                NavigationArg.bindArgumentsOf(
                  Pair(NavigationArg.FEATURE, AppFeature.PatientManagement.name),
                  Pair(NavigationArg.HEALTH_MODULE, HealthModule.HIV),
                  Pair(NavigationArg.PATIENT_ID, it)
                )
              event.navController.navigate(
                route = MainNavigationScreen.ViewChildContacts.route + urlParams
              )
            }
          }
          R.id.remove_family_member ->
            event.context.launchQuestionnaire<RemoveFamilyMemberQuestionnaireActivity>(
              questionnaireId = REMOVE_FAMILY_FORM,
              clientIdentifier = patientId,
              intentBundle = bundleOf(Pair(NavigationArg.FAMILY_ID, familyId))
            )
          R.id.record_as_anc ->
            event.context.launchQuestionnaire<QuestionnaireActivity>(
              questionnaireId = ANC_ENROLLMENT_FORM,
              clientIdentifier = patientId,
              questionnaireType = QuestionnaireType.DEFAULT
            )
          R.id.edit_profile ->
            event.context.launchQuestionnaire<QuestionnaireActivity>(
              questionnaireId = EDIT_PROFILE_FORM,
              clientIdentifier = patientId,
              questionnaireType = QuestionnaireType.EDIT
            )
          R.id.viral_load_results ->
            event.context.launchQuestionnaire<QuestionnaireActivity>(
              questionnaireId = VIRAL_LOAD_RESULTS_FORM,
              clientIdentifier = patientId,
              questionnaireType = QuestionnaireType.DEFAULT,
              populationResources = profile.populationResources
            )
          R.id.hiv_test_and_results ->
            event.context.launchQuestionnaire<QuestionnaireActivity>(
              questionnaireId = HIV_TEST_AND_RESULTS_FORM,
              clientIdentifier = patientId,
              questionnaireType = QuestionnaireType.DEFAULT,
              populationResources = profile.populationResources
            )
          R.id.hiv_test_and_next_appointment ->
            event.context.launchQuestionnaire<QuestionnaireActivity>(
              questionnaireId = HIV_TEST_AND_NEXT_APPOINTMENT_FORM,
              clientIdentifier = patientId,
              questionnaireType = QuestionnaireType.DEFAULT,
              populationResources = profile.populationResources
            )
          R.id.remove_hiv_patient ->
            event.context.launchQuestionnaire<HivPatientQuestionnaireActivity>(
              questionnaireId = REMOVE_HIV_PATIENT_FORM,
              clientIdentifier = patientId,
              populationResources = profile.populationResources
            )
          else -> {}
        }
      }
      is PatientProfileEvent.OpenTaskForm ->
        event.context.launchQuestionnaireForResult<QuestionnaireActivity>(
          questionnaireId = event.taskFormId,
          clientIdentifier = patientId,
          backReference = event.taskId.asReference(ResourceType.Task).reference,
          populationResources = profile.populationResources
        )
      is PatientProfileEvent.OpenChildProfile -> {
        val urlParams =
          NavigationArg.bindArgumentsOf(
            Pair(NavigationArg.FEATURE, AppFeature.PatientManagement.name),
            Pair(NavigationArg.HEALTH_MODULE, healthModule),
            Pair(NavigationArg.PATIENT_ID, event.patientId)
          )
        if (healthModule == HealthModule.FAMILY)
          event.navController.navigate(route = MainNavigationScreen.FamilyProfile.route + urlParams)
        else
          event.navController.navigate(
            route = MainNavigationScreen.PatientProfile.route + urlParams
          )
      }
    }
  }

  fun handleVisitType(isClientVisit: Boolean) {
    if (isClientVisit) {
      val updatedMenuItems =
        patientProfileUiState.value.overflowMenuItems.map {
          when (it.id) {
            R.id.guardian_visit -> it.apply { hidden = false }
            R.id.client_visit -> it.apply { hidden = true }
            else -> it
          }
        }
      patientProfileUiState.value =
        patientProfileUiState.value.copy(overflowMenuItems = updatedMenuItems)
      undoGuardianVisitTasksFilter()
    } else {
      val updatedMenuItems =
        patientProfileUiState.value.overflowMenuItems.map {
          when (it.id) {
            R.id.guardian_visit -> it.apply { hidden = true }
            R.id.client_visit -> it.apply { hidden = false }
            else -> it
          }
        }
      patientProfileUiState.value =
        patientProfileUiState.value.copy(overflowMenuItems = updatedMenuItems)
      filterGuardianVisitTasks()
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
          refreshOverFlowMenu(healthModule = healthModule, patientProfile = it)
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
    const val REMOVE_FAMILY_FORM = "remove-family"
    const val FAMILY_MEMBER_REGISTER_FORM = "family-member-registration"
    const val ANC_ENROLLMENT_FORM = "anc-patient-registration"
    const val EDIT_PROFILE_FORM = "edit-patient-profile"
    const val VIRAL_LOAD_RESULTS_FORM = "art-client-viral-load-test-results"
    const val HIV_TEST_AND_RESULTS_FORM = "exposed-infant-hiv-test-and-results"
    const val HIV_TEST_AND_NEXT_APPOINTMENT_FORM =
      "contact-and-community-positive-hiv-test-and-next-appointment"
    const val REMOVE_HIV_PATIENT_FORM = "remove-person"
    const val PATIENT_FINISH_VISIT = "patient-finish-visit"
  }
}
