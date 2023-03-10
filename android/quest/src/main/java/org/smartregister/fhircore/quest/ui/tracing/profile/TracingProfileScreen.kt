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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.theme.LoginButtonColor
import org.smartregister.fhircore.engine.ui.theme.LoginFieldBackgroundColor
import org.smartregister.fhircore.engine.ui.theme.PatientProfileSectionsBackgroundColor
import org.smartregister.fhircore.engine.ui.theme.StatusTextColor
import org.smartregister.fhircore.engine.ui.theme.SuccessColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.quest.R as R2
import org.smartregister.fhircore.quest.ui.shared.models.ProfileViewData
import org.smartregister.fhircore.quest.ui.tracing.components.InfoBoxItem

@Composable
fun TracingProfileScreen(
  navController: NavHostController,
  modifier: Modifier = Modifier,
  viewModel: TracingProfileViewModel = hiltViewModel()
) {

  TracingProfilePage(
    navController,
    modifier = modifier,
    tracingProfileViewModel = viewModel,
    onBackPress = { navController.popBackStack() }
  )
}

@Composable
fun TracingProfilePage(
  navController: NavHostController,
  modifier: Modifier = Modifier,
  onBackPress: () -> Unit,
  tracingProfileViewModel: TracingProfileViewModel,
) {

  val context = LocalContext.current
  val profileViewDataState = tracingProfileViewModel.patientProfileViewData.collectAsState()
  val profileViewData by remember { profileViewDataState }
  var showOverflowMenu by remember { mutableStateOf(false) }
  val viewState = tracingProfileViewModel.patientTracingProfileUiState.value

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(id = R2.string.patient_details)) },
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
                  tracingProfileViewModel.onEvent(
                    TracingProfileEvent.OverflowMenuClick(
                      navController = navController,
                      context,
                      it.id
                    )
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
              ) { Text(text = stringResource(id = it.titleResource), color = it.titleColor) }
            }
          }
        }
      )
    },
    bottomBar = {
      Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxWidth()) {
        Button(
          colors =
            ButtonDefaults.buttonColors(
              backgroundColor = LoginButtonColor,
              LoginFieldBackgroundColor
            ),
          onClick = {
            tracingProfileViewModel.onEvent(TracingProfileEvent.LoadOutComesForm(context))
          },
          modifier = modifier.fillMaxWidth()
        ) {
          Text(
            color = Color.White,
            text = stringResource(id = R2.string.tracing_outcomes),
            modifier = modifier.padding(8.dp)
          )
        }
      }
    }
  ) { innerPadding ->
    TracingProfilePageView(innerPadding = innerPadding, profileViewData = profileViewData)
  }
}

@Composable
fun TracingProfilePageView(
  modifier: Modifier = Modifier,
  innerPadding: PaddingValues = PaddingValues(all = 0.dp),
  profileViewData: ProfileViewData.TracingProfileData = ProfileViewData.TracingProfileData()
) {
  Column(modifier = modifier.fillMaxHeight().fillMaxWidth().padding(innerPadding)) {
    Box(modifier = Modifier.padding(5.dp).weight(2.0f)) {
      Column(
        modifier =
          modifier
            .verticalScroll(rememberScrollState())
            .background(PatientProfileSectionsBackgroundColor)
      ) {
        // Personal Data: e.g. sex, age, dob
        PatientInfo(profileViewData)
        Spacer(modifier = modifier.height(20.dp))
        // Tracing Visit Due // pull tracingTask -> executionPeriod -> end
        TracingVisitDue(profileViewData.dueDate)
        // TracingVisitDue(profileViewData.tracingTask.executionPeriod.end.asDdMmYyyy())
        Spacer(modifier = modifier.height(20.dp))
        // Tracing Reason
        repeat(profileViewData.tracingTasks.size) {
          TracingReasonBox(profileViewData.tracingTasks[it], displayForHomeTrace = true)
          Spacer(modifier = modifier.height(20.dp))
        }
        // Tracing Patient address/contact
        TracingContactAddress(profileViewData, displayForHomeTrace = false)
        Spacer(modifier = modifier.height(20.dp))
        TracingGuardianAddress(profileViewData.guardiansRelatedPersonResource)
      }
    }
  }
}

@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
@Composable
fun TracingScreenPreview() {
  TracingProfilePageView(modifier = Modifier)
}

@Composable
fun PatientInfo(
  patientProfileViewData: ProfileViewData.TracingProfileData,
  modifier: Modifier = Modifier,
) {
  Card(elevation = 3.dp, modifier = modifier.fillMaxWidth()) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
      InfoBoxItem(title = stringResource(R2.string.name), value = patientProfileViewData.name)
      InfoBoxItem(title = stringResource(R.string.age), value = patientProfileViewData.age)
      InfoBoxItem(title = stringResource(R.string.sex), value = patientProfileViewData.sex)
      if (patientProfileViewData.identifier != null) {
        var idKeyValue: String
        if (patientProfileViewData.showIdentifierInProfile) {
          idKeyValue =
            stringResource(
              R.string.idKeyValue,
              patientProfileViewData.identifierKey,
              patientProfileViewData.identifier.ifEmpty {
                stringResource(R.string.identifier_unassigned)
              }
            )
          Text(
            text = idKeyValue,
            color = StatusTextColor,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

@Composable
private fun TracingReasonItem(
  title: String,
  value: String,
  verticalRenderOrientation: Boolean = false,
  modifier: Modifier = Modifier
) {
  if (verticalRenderOrientation) {
    Column(modifier = modifier.padding(4.dp)) {
      Row(modifier = modifier.padding(bottom = 4.dp)) {
        Text(text = title, modifier)
        Text(
          text = ":",
          modifier.padding(end = 4.dp),
        )
      }
      Text(text = value, color = StatusTextColor)
    }
  } else {
    Row(modifier = modifier.padding(4.dp)) {
      Text(text = title, modifier)
      Text(text = ":", modifier.padding(end = 4.dp), color = StatusTextColor)
      Text(text = value, color = StatusTextColor)
    }
  }
}

@Composable
private fun TracingVisitDue(dueDate: String, modifier: Modifier = Modifier) {
  Card(
    elevation = 3.dp,
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(width = 2.dp, color = StatusTextColor)
  ) {
    Row(
      modifier = modifier.padding(6.dp, 8.dp).fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = stringResource(R2.string.tracing_visit_due),
        modifier.padding(bottom = 4.dp),
        color = StatusTextColor,
        fontSize = 18.sp
      )
      Text(text = dueDate, fontSize = 18.sp)
      Icon(imageVector = Icons.Filled.Sync, "", tint = StatusTextColor)
    }
  }
}

@Composable
private fun TracingReasonBox(
  tracingTask: Task,
  modifier: Modifier = Modifier,
  displayForHomeTrace: Boolean = false,
) {
  Card(
    elevation = 3.dp,
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(width = 2.dp, color = StatusTextColor)
  ) {
    Column(modifier = modifier.padding(horizontal = 4.dp)) {
      TracingReasonItem(
        title = stringResource(R2.string.reason_for_trace),
        value = tracingTask.reasonCode?.codingFirstRep?.display ?: ""
      )
      if (displayForHomeTrace)
        TracingReasonItem(
          title = stringResource(R2.string.last_home_trace_outcome),
          value = "Todo display reason text here",
          verticalRenderOrientation = true
        )
      else
        TracingReasonItem(
          title = stringResource(R2.string.last_phone_trace_outcome),
          value = "Todo display reason text here",
          verticalRenderOrientation = true
        )
      TracingReasonItem(
        title = stringResource(R2.string.date_of_last_attempt),
        value = "todo DD/MM/yyyy"
      )
      TracingReasonItem(title = stringResource(R2.string.number_of_attempts), value = "X")
    }
  }
}

@Composable
private fun TracingContactAddress(
  patientProfileViewData: ProfileViewData.TracingProfileData,
  displayForHomeTrace: Boolean = false,
  modifier: Modifier = Modifier
) {
  Card(
    elevation = 3.dp,
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(width = 2.dp, color = StatusTextColor)
  ) {
    Column(modifier = modifier.padding(horizontal = 4.dp).fillMaxWidth()) {
      if (displayForHomeTrace) {
        TracingReasonItem(
          title = stringResource(R2.string.patient_district),
          value = patientProfileViewData.addressDistrict
        )
        TracingReasonItem(
          title = stringResource(R2.string.patient_tracing_catchment),
          value = patientProfileViewData.addressTracingCatchment
        )
        TracingReasonItem(
          title = stringResource(R2.string.patient_physcal_locator),
          value = patientProfileViewData.addressPhysicalLocator
        )
      } else {
        TracingReasonItem(
          title = stringResource(R2.string.patient_phone_number_1),
          value = "" // patientProfileViewData.phoneContacts[0] ?: ""
        )
        TracingReasonItem(
          title = stringResource(R2.string.patient_phone_owner_1),
          value = "Patient"
        )
        Text(
          text = "CALL",
          textAlign = TextAlign.End,
          fontSize = 14.sp,
          color = SuccessColor,
          modifier = modifier.fillMaxWidth().padding(end = 16.dp, bottom = 8.dp)
        )
      }
    }
  }
}

@Composable
private fun TracingGuardianAddress(
  guardiansRelatedPersonResource: List<RelatedPerson>,
  modifier: Modifier = Modifier
) {
  if (guardiansRelatedPersonResource.isNotEmpty()) {
    Card(
      elevation = 3.dp,
      modifier = modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(width = 2.dp, color = StatusTextColor)
    ) {
      Column(modifier = modifier.padding(horizontal = 4.dp)) {
        TracingReasonItem(
          title = stringResource(R2.string.guardian_relation),
          value =
            "" // guardiansRelatedPersonResource[0].relationshipFirstRep.codingFirstRep.display
        )
        TracingReasonItem(
          title = stringResource(R2.string.guardian_phone_number_1),
          value = "" // guardiansRelatedPersonResource[0].telecomFirstRep.value
        )
        TracingReasonItem(
          title = stringResource(R2.string.guardian_phone_owner_1),
          value = "Guardian 1"
        )
        Text(
          text = "CALL",
          textAlign = TextAlign.End,
          fontSize = 14.sp,
          color = SuccessColor,
          modifier = modifier.fillMaxWidth().padding(end = 16.dp, bottom = 8.dp)
        )
      }
    }
    if (guardiansRelatedPersonResource.size > 1) {
      Card(
        elevation = 3.dp,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(width = 2.dp, color = StatusTextColor)
      ) {
        Column(modifier = modifier.padding(horizontal = 4.dp)) {
          TracingReasonItem(
            title = stringResource(R2.string.guardian_phone_number_2),
            value =
              "" // guardiansRelatedPersonResource[1].relationshipFirstRep.codingFirstRep.display
          )
          TracingReasonItem(
            title = stringResource(R2.string.guardian_phone_number_2),
            value = "" // guardiansRelatedPersonResource[1].telecomFirstRep.value
          )
          TracingReasonItem(
            title = stringResource(R2.string.guardian_phone_owner_2),
            value = "Guardian 2"
          )
          Text(
            text = "CALL",
            textAlign = TextAlign.End,
            fontSize = 14.sp,
            color = SuccessColor,
            modifier = modifier.fillMaxWidth().padding(end = 16.dp, bottom = 8.dp)
          )
        }
      }
    }
  }
}
