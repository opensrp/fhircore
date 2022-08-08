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

package org.smartregister.fhircore.quest.ui.profile

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaire
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaireForResult
import org.smartregister.fhircore.engine.util.extension.monthsPassed
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.ui.family.remove.member.RemoveFamilyMemberQuestionnaireActivity

@HiltViewModel
class ProfileViewModel
@Inject
constructor(
  val registerRepository: RegisterRepository,
  val configurationRegistry: ConfigurationRegistry
) : ViewModel() {

  val profileUiState: MutableState<ProfileUiState> = mutableStateOf(ProfileUiState())

  private lateinit var profileConfiguration: ProfileConfiguration

  fun retrieveProfileUiState(profileId: String, resourceId: String) {
    if (resourceId.isNotEmpty()) {
      viewModelScope.launch {
        profileUiState.value =
          ProfileUiState(
            resourceData = registerRepository.loadProfileData(profileId, resourceId),
            profileConfiguration = retrieveProfileConfiguration(profileId)
          )
      }
    }
  }

  private fun retrieveProfileConfiguration(profileId: String): ProfileConfiguration {
    // Ensures profile configuration is initialized once
    if (!::profileConfiguration.isInitialized) {
      profileConfiguration =
        configurationRegistry.retrieveConfiguration(ConfigType.Profile, profileId)
    }
    return profileConfiguration
  }

  fun onEvent(event: ProfileEvent) =
    when (event) {
      is ProfileEvent.LoadQuestionnaire ->
        event.context.launchQuestionnaire<QuestionnaireActivity>(event.questionnaireId)
      is ProfileEvent.SeeAll -> {
        /* TODO(View all records in this category e.g. all medical history, tasks etc) */
      }
      is ProfileEvent.OverflowMenuClick -> {
        // TODO use navigation items from config and handle these actions dynamically
        // https://github.com/opensrp/fhircore/issues/1371
        when (event.menuId) {
          R.id.individual_details ->
            event.context.launchQuestionnaire<QuestionnaireActivity>(
              questionnaireId = FAMILY_MEMBER_REGISTER_FORM,
              clientIdentifier = event.patientId,
              questionnaireType = QuestionnaireType.EDIT
            )
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
          R.id.pregnancy_outcome ->
            event.context.launchQuestionnaire<QuestionnaireActivity>(
              questionnaireId = PREGNANCY_OUTCOME_FORM,
              clientIdentifier = event.patientId,
              questionnaireType = QuestionnaireType.DEFAULT
            )
          R.id.record_sick_child ->
            event.context.launchQuestionnaire<QuestionnaireActivity>(
              questionnaireId =
                if (event.patient.dob!!.monthsPassed() < 2) SICK_CHILD_UNDER_2M_FORM
                else SICK_CHILD_ABOVE_2M_FORM,
              clientIdentifier = event.patientId,
              questionnaireType = QuestionnaireType.DEFAULT
            )
          else -> {}
        }
      }
      is ProfileEvent.OpenTaskForm ->
        event.context.launchQuestionnaireForResult<QuestionnaireActivity>(
          questionnaireId = event.taskFormId,
          clientIdentifier = event.patientId,
          backReference = event.taskId?.asReference(ResourceType.Task)?.reference
        )
      else -> {}
    }

  companion object {
    const val REMOVE_FAMILY_FORM = "remove-family"
    const val FAMILY_MEMBER_REGISTER_FORM = "family-member-registration"
    const val ANC_ENROLLMENT_FORM = "anc-patient-registration"
    const val PREGNANCY_OUTCOME_FORM = "pregnancy-outcome"
    const val SICK_CHILD_UNDER_2M_FORM = "sick-child-under-2m"
    const val SICK_CHILD_ABOVE_2M_FORM = "sick-child-above-2m"
  }
}
