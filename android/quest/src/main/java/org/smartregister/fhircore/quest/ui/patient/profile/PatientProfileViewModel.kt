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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.data.local.register.PatientRegisterRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaire
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.navigation.OverflowMenuFactory
import org.smartregister.fhircore.quest.navigation.OverflowMenuHost
import org.smartregister.fhircore.quest.ui.family.remove.member.RemoveMemberProfileQuestionnaireActivity
import org.smartregister.fhircore.quest.ui.patient.profile.model.ProfileViewData
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
        overflowMenuItems =
          overflowMenuFactory.overflowMenuMap.getValue(OverflowMenuHost.PATIENT_PROFILE)
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
          R.id.remove_family_member -> {
             event.context.launchQuestionnaire<RemoveMemberProfileQuestionnaireActivity>(
               questionnaireId = REMOVE_FAMILY_FORM,
               clientIdentifier = event.patientId
             )
          }
          else -> {}
        }
      }
    }

  companion object {
    const val REMOVE_FAMILY_FORM = "remove-family"
  }
}
