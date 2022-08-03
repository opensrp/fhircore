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

package org.smartregister.fhircore.quest.ui.family.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.ui.theme.DangerColor
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.engine.ui.theme.SuccessColor
import org.smartregister.fhircore.engine.ui.theme.WarningColor
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.family.profile.model.FamilyMemberTask
import org.smartregister.fhircore.quest.ui.family.profile.model.FamilyMemberViewState

@Composable
fun FamilyProfileRow(
  familyMemberViewState: FamilyMemberViewState,
  onFamilyMemberClick: (String) -> Unit,
  onTaskClick: (String, String) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      modifier
        .clickable { onFamilyMemberClick(familyMemberViewState.patientId) }
        .wrapContentHeight()
        .fillMaxWidth()
        .padding(16.dp)
  ) {
    // Only display title and no tasks if tasks is empty
    if (familyMemberViewState.memberTasks.isEmpty()) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
      ) {
        FamilyProfileTitleText(
          familyMemberViewState = familyMemberViewState,
          modifier = modifier.weight(0.6f)
        )
      }
    } else {
      Column(modifier = modifier.weight(0.6f), verticalArrangement = Arrangement.SpaceAround) {
        FamilyProfileTitleText(familyMemberViewState = familyMemberViewState, modifier = modifier)
        Text(
          text = familyMemberViewState.statuses.joinToString(", "),
          fontSize = 16.sp,
          color = Color.DarkGray.copy(alpha = 0.7f),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = modifier.padding(bottom = 4.dp)
        )
        if (familyMemberViewState.showAtRisk) {
          Text(
            text = stringResource(R.string.at_risk),
            fontSize = 16.sp,
            color = WarningColor,
            modifier = modifier.padding(bottom = 4.dp),
          )
        }
      }
      if (familyMemberViewState.memberIcon != null) {
        Icon(
          modifier = modifier.size(20.dp).padding(2.4.dp),
          painter = painterResource(id = familyMemberViewState.memberIcon),
          contentDescription = null,
          tint = Color.Unspecified
        )
      }

      // Display family members tasks
      Column(modifier = modifier.weight(0.4f)) {
        familyMemberViewState
          .memberTasks
          .filter { it.taskStatus == Task.TaskStatus.READY }
          .take(3)
          .forEach {
            OutlinedButton(
              onClick = { it.taskFormId?.let { taskFormId -> onTaskClick(taskFormId, it.taskId) } },
              colors =
                ButtonDefaults.buttonColors(
                  backgroundColor = it.colorCode.copy(alpha = 0.1f),
                  contentColor = it.colorCode.copy(alpha = 0.8f),
                ),
              modifier = modifier.padding(vertical = 2.2.dp).fillMaxWidth()
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
              ) {
                Icon(
                  imageVector =
                    if (it.taskStatus == Task.TaskStatus.COMPLETED) Icons.Filled.Check
                    else Icons.Filled.Add,
                  contentDescription = null,
                  tint =
                    if (it.taskStatus == Task.TaskStatus.COMPLETED) SuccessColor else it.colorCode
                )
                Text(text = it.task, color = it.colorCode.copy(alpha = 0.9f))
              }
            }
          }
      }
    }
  }
}

@Composable
private fun FamilyProfileTitleText(
  familyMemberViewState: FamilyMemberViewState,
  modifier: Modifier
) {
  Text(
    text =
      listOf(
          familyMemberViewState.name,
          familyMemberViewState.gender.uppercase().first().toString(),
          familyMemberViewState.age
        )
        .joinToString(", "),
    fontSize = 18.sp,
    color = Color.Black.copy(alpha = 0.7f),
    modifier = modifier.padding(bottom = 4.dp)
  )
}

@Preview(showBackground = true)
@Composable
fun FamilyProfileRowPreviewWithAtRisk() {
  FamilyProfileRow(
    familyMemberViewState =
      FamilyMemberViewState(
        patientId = "192192",
        name = "John Jared Juma",
        gender = "Male",
        age = 37.toString(),
        statuses =
          listOf(
            "Family Head, Malaria, Family Planning, Another Condition that I cannot explain here right now"
          ),
        showAtRisk = true,
        memberIcon = R.drawable.ic_pregnant,
        memberTasks = membersTasks()
      ),
    onFamilyMemberClick = {},
    onTaskClick = { _, _ -> }
  )
}

@Preview(showBackground = true)
@Composable
fun FamilyProfileRowPreviewWithoutAtRisk() {
  FamilyProfileRow(
    familyMemberViewState =
      FamilyMemberViewState(
        patientId = "192192",
        name = "John Jared Juma",
        gender = "Male",
        age = 37.toString(),
        statuses = listOf("Family Head"),
        showAtRisk = false,
        memberIcon = R.drawable.ic_pregnant,
        memberTasks = membersTasks().take(1)
      ),
    onFamilyMemberClick = {},
    onTaskClick = { _, _ -> }
  )
}

@Preview(showBackground = true)
@Composable
fun FamilyProfileRowPreviewWithNoTasks() {
  FamilyProfileRow(
    familyMemberViewState =
      FamilyMemberViewState(
        patientId = "192192",
        name = "John Jared Juma",
        gender = "Male",
        age = 37.toString(),
        statuses = emptyList(),
        memberTasks = emptyList()
      ),
    onFamilyMemberClick = {},
    onTaskClick = { _, _ -> }
  )
}

@Composable
private fun membersTasks() =
  listOf(
    FamilyMemberTask(
      taskId = "1123",
      taskFormId = "t12991",
      task = "Malaria Follow-up",
      taskStatus = Task.TaskStatus.COMPLETED,
      colorCode = DefaultColor
    ),
    FamilyMemberTask(
      taskId = "1124",
      taskFormId = "t12991",
      task = "Bednet",
      taskStatus = Task.TaskStatus.FAILED,
      colorCode = DangerColor
    ),
    FamilyMemberTask(
      taskId = "1125",
      taskFormId = "t12991",
      task = "Family Planning",
      taskStatus = Task.TaskStatus.READY,
      colorCode = InfoColor
    )
  )
