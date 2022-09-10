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

package org.smartregister.fhircore.quest.ui.patient.profile.guardians

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.RelatedPerson
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaire
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.data.patient.HivPatientGuardianRepository
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.navigation.OverflowMenuFactory
import org.smartregister.fhircore.quest.navigation.OverflowMenuHost
import org.smartregister.fhircore.quest.ui.patient.profile.PatientProfileEvent
import org.smartregister.fhircore.quest.ui.patient.profile.PatientProfileUiState
import org.smartregister.fhircore.quest.ui.shared.models.ProfileViewData
import org.smartregister.fhircore.quest.util.mappers.ProfileViewDataMapper

@HiltViewModel
class GuardianNotOnARTProfileViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  val overflowMenuFactory: OverflowMenuFactory,
  val profileViewDataMapper: ProfileViewDataMapper,
  val repository: HivPatientGuardianRepository
) : ViewModel() {
  // Get your argument from the SavedStateHandle
  private val guardianId: String = savedStateHandle[NavigationArg.PATIENT_ID]!!
  private val appFeatureName: String? = savedStateHandle[NavigationArg.FEATURE]

  val profileMenuUiState: MutableState<PatientProfileUiState> =
    mutableStateOf(PatientProfileUiState(OverflowMenuHost.NOT_ON_ART.overflowMenuItems))
  val profileViewData: MutableState<ProfileViewData.PatientProfileViewData> =
    mutableStateOf(ProfileViewData.PatientProfileViewData())

  private var guardianResource: RelatedPerson? = null
  private var patientId: String? = null

  fun getProfileData() {
    viewModelScope.launch {
      repository.loadGuardianNotOnARTProfile(guardianId).let {
        profileViewData.value =
          profileViewDataMapper.transformInputToOutputModel(it) as
            ProfileViewData.PatientProfileViewData
      }
      guardianResource = repository.loadRelatedPerson(guardianId)
      patientId = IdType(guardianResource?.patient?.reference).idPart
    }
  }

  fun onMenuItemClick(context: Context, menuId: Int, data: ProfileViewData) {
    when (menuId) {
      R.id.edit_profile ->
        context.launchQuestionnaire<QuestionnaireActivity>(
          questionnaireId = FORM.EDIT_PROFILE,
          clientIdentifier = patientId,
          populationResources =
            if (guardianResource != null) arrayListOf(guardianResource!!) else null
        )
      R.id.remove_hiv_patient ->
        context.launchQuestionnaire<QuestionnaireActivity>(
          questionnaireId = FORM.REMOVE_PERSON,
          clientIdentifier = patientId,
          populationResources =
            if (guardianResource != null) arrayListOf(guardianResource!!) else null
        )
    }
  }

  fun onEvent(event: PatientProfileEvent) {
    // do nothing
  }

  object FORM {
    const val EDIT_PROFILE = "edit-guardian-profile"
    const val REMOVE_PERSON = "remove-guardian-not-on-art"
  }
}
