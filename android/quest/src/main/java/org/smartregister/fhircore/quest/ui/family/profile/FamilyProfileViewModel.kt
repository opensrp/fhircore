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

package org.smartregister.fhircore.quest.ui.family.profile

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaire
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaireForResult
import org.smartregister.fhircore.engine.util.extension.yearsPassed
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.navigation.OverflowMenuFactory
import org.smartregister.fhircore.quest.navigation.OverflowMenuHost
import org.smartregister.fhircore.quest.ui.family.profile.model.EligibleFamilyHeadMember
import org.smartregister.fhircore.quest.ui.family.profile.model.EligibleFamilyHeadMemberViewState
import org.smartregister.fhircore.quest.ui.family.remove.family.RemoveFamilyQuestionnaireActivity
import org.smartregister.fhircore.quest.ui.shared.models.ProfileViewData
import org.smartregister.fhircore.quest.util.mappers.ProfileViewDataMapper

@HiltViewModel
class FamilyProfileViewModel
@Inject
constructor(
  val overflowMenuFactory: OverflowMenuFactory,
  val registerRepository: RegisterRepository,
  val profileViewDataMapper: ProfileViewDataMapper,
  val dispatcherProvider: DefaultDispatcherProvider
) : ViewModel() {

  val familyProfileUiState: MutableState<FamilyProfileUiState> =
    mutableStateOf(
      FamilyProfileUiState(
        overflowMenuItems =
          overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.FAMILY_PROFILE)
      )
    )

  val familyMemberProfileData: MutableState<ProfileViewData.FamilyProfileViewData> =
    mutableStateOf(ProfileViewData.FamilyProfileViewData())

  fun onEvent(event: FamilyProfileEvent) {
    when (event) {
      is FamilyProfileEvent.AddMember ->
        event.context.launchQuestionnaire<QuestionnaireActivity>(
          questionnaireId = FAMILY_MEMBER_REGISTER_FORM,
          groupIdentifier = event.familyId
        )
      is FamilyProfileEvent.OpenMemberProfile -> {
        val urlParams =
          NavigationArg.bindArgumentsOf(
            // TODO depending on client type, use relevant health module to load the correct content
            Pair(NavigationArg.RESOURCE_ID, event.patientId),
            Pair(NavigationArg.FAMILY_ID, event.familyId)
          )
        event.navController.navigate(route = MainNavigationScreen.Profile.route + urlParams)
      }
      is FamilyProfileEvent.OpenTaskForm ->
        event.context.launchQuestionnaireForResult<QuestionnaireActivity>(
          questionnaireId = event.taskFormId,
          clientIdentifier = event.patientId,
          backReference = event.taskId.asReference(ResourceType.Task).reference
        )
      is FamilyProfileEvent.OverflowMenuClick -> {
        when (event.menuId) {
          R.id.family_details ->
            event.context.launchQuestionnaire<QuestionnaireActivity>(
              questionnaireId = FAMILY_REGISTRATION_FORM,
              clientIdentifier = event.familyId,
              questionnaireType = QuestionnaireType.EDIT
            )
          R.id.remove_family ->
            event.context.launchQuestionnaire<RemoveFamilyQuestionnaireActivity>(
              questionnaireId = REMOVE_FAMILY_FORM,
              clientIdentifier = event.familyId
            )
        }
      }
      is FamilyProfileEvent.FetchMemberTasks -> {
        /*TODO fetch tasks for this member*/
      }
      FamilyProfileEvent.RoutineVisit -> {
        /*TODO Implement family routine visit*/
      }
    }
  }

  fun fetchFamilyProfileData(familyId: String?) {
    viewModelScope.launch(dispatcherProvider.io()) {
      if (!familyId.isNullOrEmpty()) {
        // TODO load family profile data
        /* patientRegisterRepository.loadPatientProfileData(
          profileId = profileId,
          patientId = familyId
        )
        ?.let {
          familyMemberProfileData.value =
            profileViewDataMapper.transformInputToOutputModel(it) as
              ProfileViewData.FamilyProfileViewData
        }*/
      }
    }
  }

  fun filterEligibleFamilyHeadMembers(
    profileViewData: ProfileViewData.FamilyProfileViewData
  ): EligibleFamilyHeadMember {
    val listOfFamilies =
      profileViewData.familyMemberViewStates.filter { it.birthDate!!.yearsPassed() > 15 }
    return EligibleFamilyHeadMember(listOfFamilies.map { EligibleFamilyHeadMemberViewState(it) })
  }

  suspend fun changeFamilyHead(newFamilyHead: String, oldFamilyHead: String) {
    withContext(dispatcherProvider.io()) {
      // TODO handle change family head logic
      //      patientRegisterRepository.registerDaoFactory.familyRegisterDao.changeFamilyHead(
      //        newFamilyHead = newFamilyHead,
      //        oldFamilyHead = oldFamilyHead
      //      )
    }
  }

  companion object {
    const val FAMILY_MEMBER_REGISTER_FORM = "family-member-registration"
    const val REMOVE_FAMILY_FORM = "remove-family"
    const val FAMILY_REGISTRATION_FORM = "family-registration"
  }
}
