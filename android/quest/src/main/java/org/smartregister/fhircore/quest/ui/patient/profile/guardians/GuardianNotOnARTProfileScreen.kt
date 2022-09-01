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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.smartregister.fhircore.engine.ui.components.FormButton
import org.smartregister.fhircore.engine.ui.theme.PatientProfileSectionsBackgroundColor
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.patient.profile.PatientProfileEvent
import org.smartregister.fhircore.quest.ui.patient.profile.components.PersonalData
import org.smartregister.fhircore.quest.ui.patient.profile.components.ProfileActionableItem
import org.smartregister.fhircore.quest.ui.patient.profile.components.ProfileCard
import org.smartregister.fhircore.quest.ui.shared.models.PatientProfileViewSection

@Composable
fun GuardianRelatedPersonProfileScreen(
  refreshDataState: MutableState<Boolean>,
  onBackPress: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: GuardianNotOnARTProfileViewModel = hiltViewModel()
) {
  val context = LocalContext.current
  val profileViewData = viewModel.profileViewData.value
  var showOverflowMenu by remember { mutableStateOf(false) }
  val viewState = viewModel.profileMenuUiState.value
  val refreshDataStateValue by remember { refreshDataState }

  LaunchedEffect(Unit) { viewModel.getProfileData() }

  SideEffect {
    // Refresh family profile data on resume
    if (refreshDataStateValue) {
      viewModel.getProfileData()
      refreshDataState.value = false
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {},
        navigationIcon = {
          IconButton(onClick = { onBackPress() }) { Icon(Icons.Filled.ArrowBack, null) }
        },
        actions = {
          IconButton(onClick = { showOverflowMenu = !showOverflowMenu }) {
            Icon(
              imageVector = Icons.Outlined.MoreVert,
              contentDescription = null,
              tint = Color.White
            )
          }
          DropdownMenu(
            expanded = showOverflowMenu,
            onDismissRequest = { showOverflowMenu = false }
          ) {
            viewState.visibleOverflowMenuItems().forEach {
              DropdownMenuItem(
                onClick = {
                  showOverflowMenu = false
                  viewModel.onMenuItemClick(context, it.id, profileViewData)
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier =
                  modifier
                    .fillMaxWidth()
                    .background(
                      color =
                        if (it.confirmAction) it.titleColor.copy(alpha = 0.1f)
                        else Color.Transparent
                    )
              ) {
                when (it.id) {
                  R.id.view_children -> {
                    Text(text = profileViewData.viewChildText, color = it.titleColor)
                  }
                  R.id.view_guardians -> {
                    Text(
                      text = stringResource(it.titleResource, profileViewData.guardians.size),
                      color = it.titleColor
                    )
                  }
                  else -> {
                    Text(text = stringResource(id = it.titleResource), color = it.titleColor)
                  }
                }
              }
            }
          }
        }
      )
    }
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      Column(
        modifier =
          modifier
            .verticalScroll(rememberScrollState())
            .background(PatientProfileSectionsBackgroundColor)
      ) {
        // Personal Data: e.g. sex, age, dob
        PersonalData(profileViewData)

        // Patient tasks: List of tasks for the patients
        if (profileViewData.tasks.isNotEmpty()) {
          ProfileCard(
            title =
              stringResource(org.smartregister.fhircore.engine.R.string.clinic_visits).uppercase(),
            onActionClick = {},
            showSeeAll = profileViewData.showListsHighlights,
            profileViewSection = PatientProfileViewSection.TASKS
          ) {
            profileViewData.tasks.forEach {
              ProfileActionableItem(
                it,
                onActionClick = { taskFormId, taskId ->
                  viewModel.onEvent(
                    PatientProfileEvent.OpenTaskForm(
                      context = context,
                      taskFormId = taskFormId,
                      taskId = taskId,
                      patientId = profileViewData.logicalId,
                      carePlans = profileViewData.carePlans,
                      patientConditions = profileViewData.conditions
                    )
                  )
                }
              )
            }
          }
        }

        // Forms: Loaded for quest app
        if (profileViewData.forms.isNotEmpty()) {
          ProfileCard(
            title = stringResource(org.smartregister.fhircore.engine.R.string.forms),
            onActionClick = { viewModel.onEvent(PatientProfileEvent.SeeAll(it)) },
            profileViewSection = PatientProfileViewSection.FORMS
          ) {
            profileViewData.forms.forEach {
              FormButton(
                formButtonData = it,
                onFormClick = { questionnaireId, _ ->
                  viewModel.onEvent(PatientProfileEvent.LoadQuestionnaire(questionnaireId, context))
                }
              )
            }
          }
        }

        // Medical History: Show medication history for the patient
        // TODO add handled events for all items action click
        if (profileViewData.medicalHistoryData.isNotEmpty()) {
          ProfileCard(
            title = stringResource(org.smartregister.fhircore.engine.R.string.medical_history),
            onActionClick = { viewModel.onEvent(PatientProfileEvent.SeeAll(it)) },
            profileViewSection = PatientProfileViewSection.MEDICAL_HISTORY
          ) {
            profileViewData.medicalHistoryData.forEach {
              ProfileActionableItem(it, onActionClick = { _, _ -> })
            }
          }
        }

        // Upcoming Services: Display upcoming services (or tasks) for the patient
        if (profileViewData.upcomingServices.isNotEmpty()) {
          ProfileCard(
            title = stringResource(org.smartregister.fhircore.engine.R.string.upcoming_services),
            onActionClick = { viewModel.onEvent(PatientProfileEvent.SeeAll(it)) },
            profileViewSection = PatientProfileViewSection.UPCOMING_SERVICES
          ) {
            profileViewData.upcomingServices.forEach {
              ProfileActionableItem(it, onActionClick = { _, _ -> })
            }
          }
        }

        // Service Card: Display other vital information for ANC/PNC
        if (profileViewData.ancCardData.isNotEmpty()) {
          ProfileCard(
            title = stringResource(org.smartregister.fhircore.engine.R.string.service_card),
            onActionClick = { viewModel.onEvent(PatientProfileEvent.SeeAll(it)) },
            profileViewSection = PatientProfileViewSection.SERVICE_CARD
          ) {
            profileViewData.ancCardData.forEach {
              ProfileActionableItem(it, onActionClick = { _, _ -> })
            }
          }
        }
      }
    }
  }
}
