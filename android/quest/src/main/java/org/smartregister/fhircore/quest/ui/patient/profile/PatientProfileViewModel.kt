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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.appfeature.AppFeature
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.data.local.register.PatientRegisterRepository
import org.smartregister.fhircore.engine.domain.model.HealthStatus
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.isGuardianVisit
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaire
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaireForResult
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.data.patient.PatientRegisterPagingSource
import org.smartregister.fhircore.quest.data.patient.model.PatientPagingSourceState
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
  val overflowMenuFactory: OverflowMenuFactory,
  val patientRegisterRepository: PatientRegisterRepository,
  val configurationRegistry: ConfigurationRegistry,
  val profileViewDataMapper: ProfileViewDataMapper,
  val registerViewDataMapper: RegisterViewDataMapper
) : ViewModel() {

  var patientProfileUiState: MutableState<PatientProfileUiState> =
    mutableStateOf(
      PatientProfileUiState(
        overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.PATIENT_PROFILE)
      )
    )

  val patientProfileViewData: MutableState<ProfileViewData.PatientProfileViewData> =
    mutableStateOf(ProfileViewData.PatientProfileViewData())

  var patientProfileData: ProfileData? = null

  val applicationConfiguration: ApplicationConfiguration
    get() = configurationRegistry.retrieveConfiguration(AppConfigClassification.APPLICATION)

  fun fetchPatientProfileData(
    appFeatureName: String?,
    healthModule: HealthModule,
    patientId: String
  ) {
    if (patientId.isNotEmpty()) {
      viewModelScope.launch {
        patientRegisterRepository.loadPatientProfileData(appFeatureName, healthModule, patientId)
          ?.let {
            patientProfileData = it
            patientProfileViewData.value =
              profileViewDataMapper.transformInputToOutputModel(it) as
                ProfileViewData.PatientProfileViewData
            // TODO only display some overflow menu items when certain conditions are met
            refreshOverFlowMenu(healthModule = healthModule, patientProfile = it)
          }
      }
    }
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
      patientProfileUiState =
        mutableStateOf(
          PatientProfileUiState(
            overflowMenuFactory.retrieveOverflowMenuItems(
              getOverflowMenuHostByPatientType(
                (patientProfile as ProfileData.HivProfileData).healthStatus
              )
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
      patientProfileViewData.value =
        profileViewDataMapper.transformInputToOutputModel(newProfileData) as
          ProfileViewData.PatientProfileViewData
    }
  }

  fun undoGuardianVisitTasksFilter() {
    if (patientProfileData != null) {
      patientProfileViewData.value =
        profileViewDataMapper.transformInputToOutputModel(patientProfileData!!) as
          ProfileViewData.PatientProfileViewData
    }
  }

  fun onEvent(event: PatientProfileEvent) =
    when (event) {
      is PatientProfileEvent.LoadQuestionnaire ->
        event.context.launchQuestionnaire<QuestionnaireActivity>(event.questionnaireId)
      is PatientProfileEvent.SeeAll -> {
        /* TODO(View all records in this category e.g. all medical history, tasks etc) */
      }
      is PatientProfileEvent.OverflowMenuClick -> {
        when (event.menuId) {
          R.id.individual_details ->
            event.context.launchQuestionnaire<QuestionnaireActivity>(
              questionnaireId = FAMILY_MEMBER_REGISTER_FORM,
              clientIdentifier = event.patientId,
              questionnaireType = QuestionnaireType.EDIT
            )
          R.id.guardian_visit -> {
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
          R.id.client_visit -> {
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
          }
          R.id.view_family -> {
            event.familyId?.let { familyId ->
              val urlParams =
                NavigationArg.bindArgumentsOf(
                  Pair(NavigationArg.FEATURE, AppFeature.HouseholdManagement.name),
                  Pair(NavigationArg.HEALTH_MODULE, HealthModule.FAMILY.name),
                  Pair(NavigationArg.PATIENT_ID, familyId)
                )
              event.navController.navigate(
                route = MainNavigationScreen.FamilyProfile.route + urlParams
              )
            }
          }
          R.id.view_children -> {
            event.patientId.let { patientId ->
              val urlParams =
                NavigationArg.bindArgumentsOf(
                  Pair(NavigationArg.FEATURE, AppFeature.HouseholdManagement.name),
                  Pair(NavigationArg.HEALTH_MODULE, HealthModule.HIV.name),
                  Pair(NavigationArg.PATIENT_ID, patientId)
                )
              event.navController.navigate(
                route = MainNavigationScreen.ViewChildContacts.route + urlParams
              )
            }
          }
          R.id.remove_family_member ->
            event.context.launchQuestionnaire<RemoveFamilyMemberQuestionnaireActivity>(
              questionnaireId = REMOVE_FAMILY_FORM,
              clientIdentifier = event.patientId,
              intentBundle = bundleOf(Pair(NavigationArg.FAMILY_ID, event.familyId))
            )
          R.id.record_as_anc ->
            event.context.launchQuestionnaire<QuestionnaireActivity>(
              questionnaireId = ANC_ENROLLMENT_FORM,
              clientIdentifier = event.patientId,
              questionnaireType = QuestionnaireType.DEFAULT
            )
          R.id.viral_load_results ->
            event.context.launchQuestionnaire<QuestionnaireActivity>(
              questionnaireId = VIRAL_LOAD_RESULTS_FORM,
              clientIdentifier = event.patientId,
              questionnaireType = QuestionnaireType.DEFAULT,
              populationResources = event.getActivePopulationResources()
            )
          R.id.hiv_test_and_results ->
            event.context.launchQuestionnaire<QuestionnaireActivity>(
              questionnaireId = HIV_TEST_AND_RESULTS_FORM,
              clientIdentifier = event.patientId,
              questionnaireType = QuestionnaireType.DEFAULT,
              populationResources = event.getActivePopulationResources()
            )
          R.id.hiv_test_and_next_appointment ->
            event.context.launchQuestionnaire<QuestionnaireActivity>(
              questionnaireId = HIV_TEST_AND_NEXT_APPOINTMENT_FORM,
              clientIdentifier = event.patientId,
              questionnaireType = QuestionnaireType.DEFAULT,
              populationResources = event.getActivePopulationResources()
            )
          R.id.remove_hiv_patient ->
            event.context.launchQuestionnaire<HivPatientQuestionnaireActivity>(
              questionnaireId = REMOVE_HIV_PATIENT_FORM,
              clientIdentifier = event.patientId
            )
          else -> {}
        }
      }
      is PatientProfileEvent.OpenTaskForm ->
        event.context.launchQuestionnaireForResult<QuestionnaireActivity>(
          questionnaireId = event.taskFormId,
          clientIdentifier = event.patientId,
          backReference = event.taskId.asReference(ResourceType.Task).reference,
          populationResources = event.getActivePopulationResources()
        )
      is PatientProfileEvent.OpenChildProfile -> {
        // todo: open child profile
      }
    }

  fun fetchChildPatientProfileData(
    appFeatureName: String?,
    healthModule: HealthModule,
    patientId: String
  ) {
    if (patientId.isNotEmpty()) {
      viewModelScope.launch {
        patientRegisterRepository.loadPatientProfileData(appFeatureName, healthModule, patientId)
          ?.let {
            patientProfileData = it
            patientProfileViewData.value =
              profileViewDataMapper.transformInputToOutputModel(it) as
                ProfileViewData.PatientProfileViewData
            paginateRegisterData(appFeatureName, healthModule, true)
          }
      }
    }
  }

  val paginatedChildRegisterData: MutableStateFlow<Flow<PagingData<RegisterViewData>>> =
    MutableStateFlow(emptyFlow())

  private fun getPager(
    appFeatureName: String?,
    healthModule: HealthModule,
    loadAll: Boolean = true
  ): Pager<Int, RegisterViewData> =
    Pager(
      config =
        PagingConfig(
          pageSize = PatientRegisterPagingSource.DEFAULT_PAGE_SIZE,
          initialLoadSize = PatientRegisterPagingSource.DEFAULT_INITIAL_LOAD_SIZE
        ),
      pagingSourceFactory = {
        ChildContactPagingSource(
            patientProfileViewData.value.otherPatients,
            patientRegisterRepository,
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

  fun paginateRegisterData(
    appFeatureName: String?,
    healthModule: HealthModule,
    loadAll: Boolean = true
  ) {
    paginatedChildRegisterData.value = getPager(appFeatureName, healthModule, loadAll).flow
  }

  companion object {
    const val REMOVE_FAMILY_FORM = "remove-family"
    const val FAMILY_MEMBER_REGISTER_FORM = "family-member-registration"
    const val ANC_ENROLLMENT_FORM = "anc-patient-registration"
    const val VIRAL_LOAD_RESULTS_FORM = "art-client-viral-load-test-results"
    const val HIV_TEST_AND_RESULTS_FORM = "exposed-infant-hiv-test-and-results"
    const val HIV_TEST_AND_NEXT_APPOINTMENT_FORM =
      "contact-and-community-positive-hiv-test-and-next-appointment"
    const val REMOVE_HIV_PATIENT_FORM = "remove-person"
  }
}
