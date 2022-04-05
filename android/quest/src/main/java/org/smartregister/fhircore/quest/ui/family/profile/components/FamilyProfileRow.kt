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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.domain.model.TaskStatus
import org.smartregister.fhircore.quest.ui.family.profile.model.FamilyMemberViewState

@Composable
fun FamilyProfileRow(
  familyMemberViewState: FamilyMemberViewState,
  onFamilyMemberClick: (String) -> Unit,
  onTaskClick: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      modifier
        .fillMaxWidth()
        .clickable { onFamilyMemberClick(familyMemberViewState.patientId) }
        .padding(vertical = 16.dp)
  ) {
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = modifier.weight(0.9f).fillMaxWidth()) {
        Text(
          text =
            listOf(
                familyMemberViewState.name,
                familyMemberViewState.gender.uppercase(),
                familyMemberViewState.age
              )
              .joinToString(", "),
          fontSize = 16.sp,
          modifier = modifier.padding(bottom = 4.dp),
          color = Color.Black.copy(alpha = 0.7f)
        )
        Text(
          text = familyMemberViewState.statuses.joinToString(", "),
          fontSize = 14.sp,
          modifier = modifier.padding(bottom = 4.dp),
          color = Color.DarkGray.copy(alpha = 0.7f)
        )
      }
      if (familyMemberViewState.memberIcon != null) {
        Icon(
          modifier = modifier.weight(0.1f),
          painter = painterResource(id = familyMemberViewState.memberIcon),
          contentDescription = null
        )
      }
      Column {
        familyMemberViewState.memberTasks.take(3).forEach {
          OutlinedButton(
            onClick = { it.taskFormId?.let { taskFormId -> onTaskClick(taskFormId) } },
            colors =
              ButtonDefaults.buttonColors(
                backgroundColor = it.colorCode.copy(alpha = 0.2f),
                contentColor = it.colorCode,
              )
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector =
                  if (it.taskStatus == TaskStatus.COMPLETED) Icons.Filled.Check
                  else Icons.Filled.Add,
                contentDescription = null
              )
              Text(text = it.task)
            }
          }
        }
      }
    }
  }
}
