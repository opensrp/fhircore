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

package org.smartregister.fhircore.quest.ui.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent

const val FAB_BUTTON_TEST_TAG = "fabButtonTestTag"
const val FAB_BUTTON_ROW_TEST_TAG = "fabButtonRowTestTag"
const val FAB_BUTTON_ROW_TEXT_TEST_TAG = "fabButtonRowTextTestTag"
const val FAB_BUTTON_ROW_ICON_TEST_TAG = "fabButtonRowIconTestTag"
private val ExtendedFabIconPadding = 12.dp
private val ExtendedFabTextPadding = 20.dp

@Composable
fun ExtendedFab(
  modifier: Modifier = Modifier,
  fabActions: List<NavigationMenuConfig>,
  resourceData: ResourceData? = null,
  navController: NavController,
  icon: ImageVector = Icons.Filled.Add,
) {
  FloatingActionButton(
    contentColor = Color.White,
    shape = CircleShape,
    onClick = {
      fabActions
        .first()
        .actions
        ?.handleClickEvent(navController = navController, resourceData = resourceData)
    },
    backgroundColor = MaterialTheme.colors.primary,
    modifier = modifier.testTag(FAB_BUTTON_TEST_TAG)
  ) {
    val text = fabActions.first().display.uppercase()
    val iconComposable =
      @Composable
      {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier.testTag(FAB_BUTTON_ROW_ICON_TEST_TAG)
        )
      }
    val padding = if (text.isBlank()) ExtendedFabIconPadding else ExtendedFabTextPadding
    val isTextOnly = fabActions.first().menuIconConfig == null

    Row(
      modifier =
        Modifier.padding(
            start = padding,
            end = if (text.isNotBlank()) ExtendedFabTextPadding else padding
          )
          .testTag(FAB_BUTTON_ROW_TEST_TAG),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      if (isTextOnly.not()) {
        iconComposable()
      }
      if (text.isNotBlank()) {
        if (isTextOnly.not()) Spacer(Modifier.width(ExtendedFabIconPadding))
        Text(
          text = fabActions.first().display.uppercase(),
          modifier.testTag(FAB_BUTTON_ROW_TEXT_TEST_TAG)
        )
      }
    }
  }
}

@Composable
@Preview(showBackground = true)
fun PreviewExtendedFab() {
  ExtendedFab(
    fabActions = listOf(NavigationMenuConfig(id = "test", display = "Fab Button")),
    navController = rememberNavController()
  )
}
