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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.smartregister.fhircore.engine.configuration.navigation.MenuIconConfig
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent

const val FAB_BUTTON_TEST_TAG = "fabButtonTestTag"
const val FAB_BUTTON_ROW_TEST_TAG = "fabButtonRowTestTag"
const val FAB_BUTTON_ROW_TEXT_TEST_TAG = "fabButtonRowTextTestTag"
const val FAB_BUTTON_ROW_ICON_TEST_TAG = "fabButtonRowIconTestTag"

@Composable
fun ExtendedFab(
  modifier: Modifier = Modifier,
  fabActions: List<NavigationMenuConfig>,
  resourceData: ResourceData? = null,
  navController: NavController,
) {
  val firstFabAction = remember { fabActions.first() }
  val firstFabEnabled =
    firstFabAction.enabled.interpolate(resourceData?.computedValuesMap ?: emptyMap()).toBoolean()

  FloatingActionButton(
    contentColor = if (firstFabEnabled) Color.White else DefaultColor,
    shape = CircleShape,
    onClick = {
      if (firstFabEnabled) {
        firstFabAction.actions?.handleClickEvent(
          navController = navController,
          resourceData = resourceData
        )
      }
    },
    backgroundColor =
      if (firstFabEnabled) MaterialTheme.colors.primary else DefaultColor.copy(alpha = 0.25f),
    modifier = modifier.testTag(FAB_BUTTON_TEST_TAG),
  ) {
    val text = remember { firstFabAction.display.uppercase() }
    val firstMenuIconConfig = remember { firstFabAction.menuIconConfig }

    Row(
      modifier =
        modifier.padding(horizontal = 16.dp, vertical = 8.dp).testTag(FAB_BUTTON_ROW_TEST_TAG),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      if (firstMenuIconConfig != null) {
        MenuIcon(
          menuIconConfig = firstMenuIconConfig,
          color = if (firstFabEnabled) Color.White else DefaultColor,
          paddingEnd = if (text.isNotEmpty()) 8 else 0
        )
      }
      if (text.isNotEmpty()) {
        Text(
          text = firstFabAction.display.uppercase(),
          modifier.testTag(FAB_BUTTON_ROW_TEXT_TEST_TAG)
        )
      }
    }
  }
}

@Composable
@Preview(showBackground = true)
fun PreviewDisabledExtendedFab() {
  ExtendedFab(
    fabActions =
      listOf(
        NavigationMenuConfig(
          id = "test",
          display = "Fab Button",
          menuIconConfig = MenuIconConfig(type = "local", reference = "ic_add"),
          enabled = "false"
        )
      ),
    navController = rememberNavController()
  )
}

@Composable
@Preview(showBackground = true)
fun PreviewExtendedFab() {
  ExtendedFab(
    fabActions =
      listOf(
        NavigationMenuConfig(
          id = "test",
          display = "Fab Button",
          menuIconConfig = MenuIconConfig(type = "local", reference = "ic_add")
        )
      ),
    navController = rememberNavController()
  )
}

@Composable
@Preview(showBackground = true)
fun PreviewExtendedFabJustIcon() {
  ExtendedFab(
    fabActions =
      listOf(
        NavigationMenuConfig(
          id = "test",
          display = "",
          menuIconConfig = MenuIconConfig(type = "local", reference = "ic_add")
        )
      ),
    navController = rememberNavController()
  )
}
