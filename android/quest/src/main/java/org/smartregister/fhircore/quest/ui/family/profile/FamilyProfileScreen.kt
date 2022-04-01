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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import org.smartregister.fhircore.quest.ui.family.profile.components.FamilyProfileTopBar

@Composable
fun FamilyProfileScreen(
  navController: NavHostController,
  modifier: Modifier = Modifier,
  familyProfileViewModel: FamilyProfileViewModel = hiltViewModel()
) {

  val viewState = familyProfileViewModel.familyProfileViewState.value

  var showOverflowMenu = remember { false }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { FamilyProfileTopBar(viewState, modifier) },
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
                        familyProfileViewModel.onEvent(FamilyProfileEvent.ClickOverflowMenu(it.id))
                      }
                )
              }
            }
          }
        }
      )
    }
  ) { innerPadding -> Box(modifier = modifier.padding(innerPadding)) {} }
}
