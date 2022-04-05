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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.family.profile.components.FamilyProfileRow
import org.smartregister.fhircore.quest.ui.family.profile.components.FamilyProfileTopBar

@Composable
fun FamilyProfileScreen(
  patientId: String?,
  navController: NavHostController,
  modifier: Modifier = Modifier,
  familyProfileViewModel: FamilyProfileViewModel = hiltViewModel()
) {

  LaunchedEffect(Unit) { familyProfileViewModel.fetchFamilyProfileData(patientId) }

  val viewState = familyProfileViewModel.familyProfileUiState.value

  var showOverflowMenu = remember { false }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { FamilyProfileTopBar(viewState, modifier) },
        backgroundColor = MaterialTheme.colors.primary,
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null)
          }
        },
        elevation = 3.dp,
        actions = {
          IconButton(
            onClick = { showOverflowMenu = !showOverflowMenu },
          ) { Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = null) }
          DropdownMenu(
            expanded = showOverflowMenu,
            onDismissRequest = { showOverflowMenu = false },
          ) {
            DropdownMenuItem(onClick = { showOverflowMenu = false }) {
              viewState.overflowMenuItems.forEach {
                Text(
                  text = stringResource(it.titleResource),
                  color = it.titleColor,
                  modifier =
                    modifier
                      .padding(8.dp)
                      .background(
                        color =
                          if (it.confirmAction) it.titleColor.copy(alpha = 0.3f)
                          else Color.Transparent
                      )
                      .clickable {
                        familyProfileViewModel.onEvent(FamilyProfileEvent.OverflowMenuClick(it.id))
                      }
                )
              }
            }
          }
        }
      )
    }
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      Column(modifier = modifier.padding(horizontal = 16.dp)) {
        // Household visit section
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = modifier.padding(vertical = 16.dp)
        ) {
          Text(text = stringResource(R.string.household))
          OutlinedButton(
            onClick = { familyProfileViewModel.onEvent(FamilyProfileEvent.RoutineVisit) },
            colors =
              ButtonDefaults.buttonColors(
                backgroundColor = InfoColor.copy(alpha = 0.2f),
                contentColor = InfoColor,
              )
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(imageVector = Icons.Filled.Add, contentDescription = null)
              Text(text = stringResource(R.string.routine_visit))
            }
          }
        }

        Divider()

        // Family members section
        viewState.familyMemberViewStates.forEach { memberViewState ->
          FamilyProfileRow(
            familyMemberViewState = memberViewState,
            onFamilyMemberClick = {
              familyProfileViewModel.onEvent(
                FamilyProfileEvent.MemberClick(memberViewState.patientId)
              )
            },
            onTaskClick = { taskFormId ->
              familyProfileViewModel.onEvent(FamilyProfileEvent.OpenTaskForm(taskFormId))
            }
          )
          Divider()
        }
      }
    }
  }
}
