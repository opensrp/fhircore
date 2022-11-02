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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import org.hl7.fhir.r4.model.CarePlan
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.theme.PatientProfileSectionsBackgroundColor
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.quest.ui.patient.profile.components.PersonalData
import org.smartregister.fhircore.quest.ui.patient.profile.components.ProfileActionableItem
import org.smartregister.fhircore.quest.ui.patient.profile.components.ProfileCard
import org.smartregister.fhircore.quest.ui.shared.models.PatientProfileViewSection
import java.util.*
import org.smartregister.fhircore.quest.R as R2


@Composable
fun TracingProfileScreen(
        navController: NavHostController,
        modifier: Modifier = Modifier,
        patientProfileViewModel: TracingProfileViewModel = hiltViewModel()
) {

    TracingProfilePage(modifier = modifier, onBackPress = { navController.popBackStack() },  patientProfileViewModel= patientProfileViewModel)
}

@Composable
fun TracingProfilePage(
        modifier: Modifier = Modifier,
        onBackPress: () -> Unit,
        patientProfileViewModel: TracingProfileViewModel = hiltViewModel(),

) {

  val context = LocalContext.current
  val profileViewDataState = patientProfileViewModel.patientProfileViewData.collectAsState()
  val profileViewData by remember { profileViewDataState }
  var showOverflowMenu by remember { mutableStateOf(false) }
  val viewState = patientProfileViewModel.patientTracingProfileUiState.value

  Scaffold(
          topBar = {
            TopAppBar(
                    title = { Text(stringResource(R.string.profile)) },
                    navigationIcon = {
                      IconButton(onClick = { onBackPress() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                      }
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
                                    patientProfileViewModel.onEvent(
                                            TracingProfileEvent.OverflowMenuClick(context, it.id)
                                    )
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
                              R2.id.view_children -> {
                                Text(text = profileViewData.viewChildText, color = it.titleColor)
                              }
                              R2.id.view_guardians -> {
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
    Column(modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth()) {
      Box(modifier = Modifier
              .padding(innerPadding)
              .weight(2.0f)) {
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
            val appointmentDate =
                    profileViewData.carePlans
                            .singleOrNull { it.status == CarePlan.CarePlanStatus.ACTIVE }
                            ?.period
                            ?.end
            ProfileCard(
                    title = {
                      Row(
                              modifier = Modifier.weight(1f),
                              horizontalArrangement = Arrangement.SpaceBetween
                      ) {
                        Text(text = stringResource(R.string.clinic_visit).uppercase(Locale.getDefault()))
                        if (appointmentDate != null) Text(text = appointmentDate.asDdMmmYyyy())
                      }
                    },
                    onActionClick = {},
                    showSeeAll = profileViewData.showListsHighlights,
                    profileViewSection = PatientProfileViewSection.TASKS
            ) {
              profileViewData.tasks.forEach {
                ProfileActionableItem(
                        it,
                        onActionClick = { taskFormId, taskId ->
                          patientProfileViewModel.onEvent(
                                  TracingProfileEvent.OpenTaskForm(
                                          context = context,
                                          taskFormId = taskFormId,
                                          taskId = taskId
                                  )
                          )
                        }
                )
              }
            }
          }

        }
      }

      //  Finish visit
      if (profileViewData.carePlans.isNotEmpty() && profileViewData.tasks.isNotEmpty()) {
        Button(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp),
                shape = RectangleShape,
                onClick = {
                  patientProfileViewModel.onEvent(
                          TracingProfileEvent.LoadQuestionnaire(
                                  TracingProfileViewModel.PATIENT_FINISH_VISIT,
                                  context
                          )
                  )
                },
                enabled = profileViewData.tasksCompleted
        ) {
          Text(
                  modifier = Modifier.padding(10.dp),
                  text = stringResource(id = R.string.finish).uppercase(),
                  textAlign = TextAlign.Center,
                  fontSize = 18.sp,
                  fontFamily = FontFamily.SansSerif,
                  fontWeight = FontWeight.Medium
          )
        }
      }
    }
  }
}


//@Preview(showBackground = true)
//@ExcludeFromJacocoGeneratedReport
//@Composable
//fun TracingScreenPreview() {
//    TracingProfileScreen(navController = NavHostController(LocalContext.current))
//}
