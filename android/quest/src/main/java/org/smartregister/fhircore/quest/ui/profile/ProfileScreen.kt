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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
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
import androidx.navigation.NavHostController
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.ui.theme.PatientProfileSectionsBackgroundColor
import org.smartregister.fhircore.quest.ui.shared.components.ViewRenderer

@Composable
fun ProfileScreen(
  modifier: Modifier = Modifier,
  navController: NavHostController,
  profileUiState: ProfileUiState,
  onEvent: (ProfileEvent) -> Unit
) {
  var showOverflowMenu by remember { mutableStateOf(false) }

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
          ) {}
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
