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

import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.appfeature.AppFeature
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.data.local.register.PatientRegisterRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaire
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.navigation.OverflowMenuFactory
import org.smartregister.fhircore.quest.navigation.OverflowMenuHost
import org.smartregister.fhircore.quest.ui.family.remove.member.RemoveFamilyMemberQuestionnaireActivity
import org.smartregister.fhircore.quest.ui.shared.models.ProfileViewData
import org.smartregister.fhircore.quest.util.mappers.ProfileViewDataMapper

@HiltViewModel
class PatientProfileViewModel
@Inject
constructor(
  val overflowMenuFactory: OverflowMenuFactory,
  val patientRegisterRepository: PatientRegisterRepository,
  val profileViewDataMapper: ProfileViewDataMapper
) : ViewModel() {

  val patientProfileUiState: MutableState<PatientProfileUiState> =
    mutableStateOf(
      PatientProfileUiState(
        overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.PATIENT_PROFILE)
      )
    )

  val patientProfileViewData: MutableState<ProfileViewData.PatientProfileViewData> =
    mutableStateOf(ProfileViewData.PatientProfileViewData())

  fun fetchPatientProfileData(
    appFeatureName: String?,
    healthModule: HealthModule,
    patientId: String
  ) {
    if (patientId.isNotEmpty()) {
      viewModelScope.launch {
        patientRegisterRepository.loadPatientProfileData(appFeatureName, healthModule, patientId)
          ?.let {
            patientProfileViewData.value =
              profileViewDataMapper.transformInputToOutputModel(it) as
                ProfileViewData.PatientProfileViewData
            // TODO only display some overflow menu items when certain conditions are met
          }
      }
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
          R.id.view_family ->{
            event.familyId?.let { familyId ->
              val urlParams =
                NavigationArg.bindArgumentsOf(
                  Pair(NavigationArg.FEATURE, AppFeature.HouseholdManagement.name),
                  Pair(NavigationArg.HEALTH_MODULE, HealthModule.FAMILY.name),
                  Pair(NavigationArg.PATIENT_ID, familyId)
                )
              event.navController.navigate(route = MainNavigationScreen.FamilyProfile.route + urlParams)
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
          else -> {}
        }
      }
    }

  companion object {
    const val REMOVE_FAMILY_FORM = "remove-family"
    const val FAMILY_MEMBER_REGISTER_FORM = "family-member-registration"
    const val ANC_ENROLLMENT_FORM = "anc-patient-registration"
  }
}
