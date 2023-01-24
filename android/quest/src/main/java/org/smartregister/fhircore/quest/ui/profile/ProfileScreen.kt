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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.flow.SharedFlow
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.ProfileBackgroundColor
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.fhircore.quest.ui.shared.components.ExtendedFab
import org.smartregister.fhircore.quest.ui.shared.components.SnackBarMessage
import org.smartregister.fhircore.quest.ui.shared.components.ViewRenderer
import org.smartregister.fhircore.quest.util.extensions.hookSnackBar

const val DROPDOWN_MENU_TEST_TAG = "dropDownMenuTestTag"
const val FAB_BUTTON_TEST_TAG = "fabButtonTestTag"
const val PROFILE_TOP_BAR_TEST_TAG = "profileTopBarTestTag"
const val PROFILE_TOP_BAR_ICON_TEST_TAG = "profileTopBarIconTestTag"

@Composable
fun ProfileScreen(
  modifier: Modifier = Modifier,
  navController: NavController,
  profileUiState: ProfileUiState,
  snackStateFlow: SharedFlow<SnackBarMessageConfig>,
  onEvent: (ProfileEvent) -> Unit
) {
  val scaffoldState = rememberScaffoldState()
  var showOverflowMenu by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    snackStateFlow.hookSnackBar(scaffoldState, profileUiState.resourceData, navController)
  }

  Scaffold(
    scaffoldState = scaffoldState,
    topBar = {
      TopAppBar(
        modifier = modifier.testTag(PROFILE_TOP_BAR_TEST_TAG),
        title = {},
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(
              Icons.Filled.ArrowBack,
              null,
              modifier = modifier.testTag(PROFILE_TOP_BAR_ICON_TEST_TAG)
            )
          }
        },
        actions = {
          IconButton(
            onClick = { showOverflowMenu = !showOverflowMenu },
            modifier = modifier.testTag(DROPDOWN_MENU_TEST_TAG)
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
              if (!it.visible
                  .interpolate(profileUiState.resourceData?.computedValuesMap ?: emptyMap())
                  .toBoolean()
              )
                return@forEach
              val enabled =
                it.enabled
                  .interpolate(profileUiState.resourceData?.computedValuesMap ?: emptyMap())
                  .toBoolean()
              if (it.showSeparator) Divider(color = DividerColor, thickness = 1.dp)
              DropdownMenuItem(
                enabled = enabled,
                onClick = {
                  showOverflowMenu = false
                  onEvent(
                    ProfileEvent.OverflowMenuClick(
                      navController = navController,
                      resourceData = profileUiState.resourceData,
                      overflowMenuItemConfig = it,
                      managingEntity =
                        it.actions
                          .find { actionConfig ->
                            actionConfig.managingEntity != null &&
                              actionConfig.workflow == ApplicationWorkflow.CHANGE_MANAGING_ENTITY
                          }
                          ?.managingEntity
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
              ) {
                Text(
                  text = it.title,
                  color = if (enabled) it.titleColor.parseColor() else DefaultColor
                )
              }
            }
          }
        },
        elevation = 0.dp
      )
    },
    floatingActionButton = {
      val fabActions = profileUiState.profileConfiguration?.fabActions

      if (!fabActions.isNullOrEmpty() && fabActions.first().visible) {
        ExtendedFab(
          modifier = Modifier.testTag(FAB_BUTTON_TEST_TAG),
          fabActions = fabActions,
          resourceData = profileUiState.resourceData,
          navController = navController
        )
      }
    },
    snackbarHost = { snackBarHostState ->
      SnackBarMessage(
        snackBarHostState = snackBarHostState,
        backgroundColorHex = profileUiState.snackBarTheme.backgroundColor,
        actionColorHex = profileUiState.snackBarTheme.actionTextColor,
        contentColorHex = profileUiState.snackBarTheme.messageTextColor
      )
    }
  ) { innerPadding ->
    Box(
      modifier = modifier.background(ProfileBackgroundColor).fillMaxHeight().padding(innerPadding)
    ) {
      Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        ViewRenderer(
          viewProperties = profileUiState.profileConfiguration?.views ?: emptyList(),
          resourceData = profileUiState.resourceData
              ?: ResourceData("", ResourceType.Patient, emptyMap(), emptyMap()),
          navController = navController
        )
      }
    }
  }
}
