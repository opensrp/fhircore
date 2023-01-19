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

package org.smartregister.fhircore.quest.ui.main.components

import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.smartregister.fhircore.engine.ui.theme.BlueTextColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen

const val BOTTOM_NAV_CONTAINER_TEST_TAG = "bottomNavContainerTestTag"
const val BOTTOM_NAV_ITEM_TEST_TAG = "bottomNavItemTestTag"
const val BOTTOM_NAV_ITEM_ICON_TEST_TAG = "bottomNavItemIconTestTag"

@Composable
fun BottomScreenSection(
  navController: NavHostController,
  mainNavigationScreens: List<MainNavigationScreen>
) {
  BottomNavigation(
    backgroundColor = Color.White,
    contentColor = Color.Black,
    modifier = Modifier.testTag(BOTTOM_NAV_CONTAINER_TEST_TAG)
  ) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.id
    mainNavigationScreens.filter { it.showInBottomNav }.forEach { navigationScreen ->
      if (navigationScreen.titleResource != null) {
        BottomNavigationItem(
          icon = {
            navigationScreen.iconResource?.let {
              Icon(
                painter = painterResource(id = it),
                contentDescription = stringResource(navigationScreen.titleResource),
                modifier = Modifier.testTag(BOTTOM_NAV_ITEM_ICON_TEST_TAG)
              )
            }
          },
          label = {
            Text(
              text = stringResource(navigationScreen.titleResource),
              fontSize = 12.sp,
            )
          },
          selectedContentColor = BlueTextColor,
          unselectedContentColor = Color.Black.copy(0.5f),
          alwaysShowLabel = true,
          selected = currentRoute == navigationScreen.route,
          onClick = { navController.navigate(navigationScreen.route) },
          modifier = Modifier.testTag(BOTTOM_NAV_ITEM_TEST_TAG)
        )
      }
    }
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun BottomScreenSectionPreview() {
  val navController = rememberNavController()
  val navigationScreens =
    listOf(MainNavigationScreen.Home, MainNavigationScreen.Reports, MainNavigationScreen.Settings)

  BottomScreenSection(navController = navController, mainNavigationScreens = navigationScreens)
}
