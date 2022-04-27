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
import org.smartregister.fhircore.engine.appfeature.AppFeature
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.data.local.register.PatientRegisterRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaire
import org.smartregister.fhircore.quest.navigation.NavigationScreen
import org.smartregister.fhircore.quest.navigation.OverflowMenuFactory
import org.smartregister.fhircore.quest.navigation.OverflowMenuHost
import org.smartregister.fhircore.quest.ui.family.profile.model.FamilyMemberViewState
import org.smartregister.fhircore.quest.ui.patient.profile.model.ProfileViewData
import org.smartregister.fhircore.quest.util.mappers.ProfileViewDataMapper

@HiltViewModel
class FamilyProfileViewModel
@Inject
constructor(
  val overflowMenuFactory: OverflowMenuFactory,
  val patientRegisterRepository: PatientRegisterRepository,
  val profileViewDataMapper: ProfileViewDataMapper,
  val dispatcherProvider: DefaultDispatcherProvider
) : ViewModel() {

  val familyProfileUiState: MutableState<FamilyProfileUiState> =
    mutableStateOf(
      FamilyProfileUiState(
        overflowMenuItems =
          overflowMenuFactory.overflowMenuMap.getValue(OverflowMenuHost.FAMILY_PROFILE)
      )
    )

  val familyMemberProfileData: MutableState<ProfileViewData.FamilyProfileViewData> =
    mutableStateOf(ProfileViewData.FamilyProfileViewData())

  fun onEvent(event: FamilyProfileEvent) {
    when (event) {
      is FamilyProfileEvent.AddMember ->
        event.context.launchQuestionnaire<QuestionnaireActivity>(
          questionnaireId = FAMILY_MEMBER_REGISTER_FORM
        )
      is FamilyProfileEvent.FetchFamilyProfileData -> fetchFamilyProfileData(event.familyHeadId)
      is FamilyProfileEvent.OpenMemberProfile -> {
        val urlParams =
          "?feature=${AppFeature.PatientManagement.name}&healthModule=${HealthModule.DEFAULT.name}&patientId=${event.patientId}"
        event.navController.navigate(route = NavigationScreen.PatientProfile.route + urlParams)
      }
      is FamilyProfileEvent.OpenTaskForm ->
        event.context.launchQuestionnaire<QuestionnaireActivity>(event.taskFormId)
      is FamilyProfileEvent.OverflowMenuClick -> {}
      is FamilyProfileEvent.FetchMemberTasks -> TODO()
      FamilyProfileEvent.RoutineVisit -> TODO()
    }
  }

  private fun fetchFamilyProfileData(patientId: String?) {
    viewModelScope.launch(dispatcherProvider.io()) {
      if (!patientId.isNullOrEmpty()) {
        patientRegisterRepository.loadPatientProfileData(
            AppFeature.HouseholdManagement.name,
            HealthModule.FAMILY,
            patientId
          )
          ?.let {
            familyMemberProfileData.value =
              profileViewDataMapper.transformInputToOutputModel(it) as
                ProfileViewData.FamilyProfileViewData
          }
      }
    }
  }

  fun filterEligibleFamilyMember(
    profileViewData: ProfileViewData.FamilyProfileViewData
  ): ChangeFamilyMembersHolder {
    val listOfFamilies =
      profileViewData.familyMemberViewStates.filter {
        if (it.age.contains("y")) (it.age.split(" ")[0].replace("y", "").toInt() > 15) else false
      }
    val listOfChangeFamilyMembersModel = mutableListOf<ChangeFamilyMembersModel>()
    listOfFamilies.forEach { listOfChangeFamilyMembersModel.add(ChangeFamilyMembersModel(it)) }

    return ChangeFamilyMembersHolder(listOfChangeFamilyMembersModel)
  }

  suspend fun changeFamilyHead(newFamilyHead: String, oldFamilyHead: String) {
    withContext(dispatcherProvider.io()) {
      patientRegisterRepository.registerDaoFactory.familyRegisterDao.changeFamilyHead(
        newFamilyHead = newFamilyHead,
        oldFamilyHead = oldFamilyHead
      )
    }
  }

  data class ChangeFamilyMembersHolder(
    val list: List<ChangeFamilyMembersModel>,
    var reselect: Boolean = false
  )

  data class ChangeFamilyMembersModel(
    val familyMember: FamilyMemberViewState,
    var selected: Boolean = false
  )

  companion object {
    const val FAMILY_MEMBER_REGISTER_FORM = "family-member-registration"
  }
}
