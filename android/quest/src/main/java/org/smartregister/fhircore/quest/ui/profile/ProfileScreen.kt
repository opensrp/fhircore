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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.PatientProfileSectionsBackgroundColor
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.fhircore.quest.ui.shared.components.ViewRenderer

@Composable
fun ProfileScreen(
  modifier: Modifier = Modifier,
  navController: NavHostController,
  profileUiState: ProfileUiState,
  onEvent: (ProfileEvent) -> Unit,
  profileViewModel: ProfileViewModel = hiltViewModel()
) {
  var showOverflowMenu by remember { mutableStateOf(false) }
  val context = LocalContext.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = {},
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
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
            profileUiState.profileConfiguration?.overFlowMenuItems?.forEach {
              DropdownMenuItem(
                onClick = {
                  showOverflowMenu = false
                  profileViewModel.onEvent(
                    ProfileEvent.OverflowMenuClick(
                      navController = navController,
                      context = context,
                      menuId = it.id,
                      resourceData = profileUiState.resourceData,
                      profileConfiguration = profileUiState.profileConfiguration
                    )
                  )
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier =
                  modifier
                    .fillMaxWidth()
                    .background(
                      color =
                        if (it.confirmAction) it.backgroundColor.parseColor().copy(alpha = 0.1f)
                        else Color.Transparent
                    )
              ) { Text(text = it.title, color = it.titleColor.parseColor()) }
              if (it.visible.toBoolean()) Divider(color = DividerColor, thickness = 1.dp)
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
        ViewRenderer(
          viewProperties = profileUiState.profileConfiguration?.views ?: emptyList(),
          resourceData = profileUiState.resourceData ?: ResourceData(Patient()),
          onViewComponentClick = {
            /** TODO provide click events */
          }
        )
      }
    }
  }
}
