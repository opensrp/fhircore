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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.PatientProfileSectionsBackgroundColor
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.fhircore.quest.ui.shared.components.ViewRenderer

const val ICON_BUTTON_TEST_TAG = "iconButton"

@Composable
fun ProfileScreen(
  modifier: Modifier = Modifier,
  navController: NavHostController,
  profileUiState: ProfileUiState,
  onEvent: (ProfileEvent) -> Unit
) {
  var showOverflowMenu by remember { mutableStateOf(false) }
  val mutableInteractionSource = remember { MutableInteractionSource() }
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
          IconButton(
            onClick = { showOverflowMenu = !showOverflowMenu },
            modifier = Modifier.testTag(ICON_BUTTON_TEST_TAG)
          ) {
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
              if (!it.visible.toBoolean()) return@forEach
              if (it.showSeparator.toBoolean()) Divider(color = DividerColor, thickness = 1.dp)
              DropdownMenuItem(
                onClick = {
                  showOverflowMenu = false
                  onEvent(
                    ProfileEvent.OverflowMenuClick(
                      navController = navController,
                      context = context,
                      resourceData = profileUiState.resourceData,
                      overflowMenuItemConfig = it
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
            }
          }
        }
      )
    },
    floatingActionButton = {
      val fabAction = profileUiState.profileConfiguration?.fabActions?.first()
      ExtendedFloatingActionButton(
        contentColor = Color.White,
        text = { fabAction?.display?.let { Text(text = it.uppercase()) } },
        onClick = {
          val clickAction = fabAction?.actions?.find { it.trigger == ActionTrigger.ON_CLICK }
          when (clickAction?.workflow) {
            ApplicationWorkflow.LAUNCH_QUESTIONNAIRE -> {
              clickAction.questionnaire?.id?.let { questionnaireId ->
                onEvent(ProfileEvent.LoadQuestionnaire(questionnaireId, context))
              }
            }
          }
        },
        backgroundColor = MaterialTheme.colors.primary,
        icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
        interactionSource = mutableInteractionSource
      )
    }
  ) { innerPadding ->
    Box(modifier = modifier.fillMaxHeight().padding(innerPadding)) {
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
