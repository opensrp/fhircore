/*
 * Copyright 2021-2024 Ona Systems, Inc
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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_LOCAL
import org.smartregister.fhircore.engine.configuration.navigation.ImageConfig
import org.smartregister.fhircore.engine.configuration.navigation.NavigationConfiguration
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.configuration.view.ImageProperties
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.ui.theme.AppTitleColor
import org.smartregister.fhircore.engine.ui.theme.MenuActionButtonTextColor
import org.smartregister.fhircore.engine.ui.theme.MenuItemColor
import org.smartregister.fhircore.engine.ui.theme.SideMenuBottomItemDarkColor
import org.smartregister.fhircore.engine.ui.theme.SideMenuDarkColor
import org.smartregister.fhircore.engine.ui.theme.SideMenuTopItemDarkColor
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.engine.util.extension.appVersion
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.main.AppMainEvent
import org.smartregister.fhircore.quest.ui.main.AppMainUiState
import org.smartregister.fhircore.quest.ui.main.appMainUiStateOf
import org.smartregister.fhircore.quest.ui.shared.components.Image
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent

const val SIDE_MENU_ICON = "sideMenuIcon"
const val NAV_TOP_SECTION_TEST_TAG = "navTopSectionTestTag"
const val MENU_BUTTON_TEST_TAG = "menuButtonTestTag"
const val MENU_BUTTON_ICON_TEST_TAG = "menuButtonIconTestTag"
const val MENU_BUTTON_TEXT_TEST_TAG = "menuButtonTextTestTag"
const val SIDE_MENU_ITEM_MAIN_ROW_TEST_TAG = "sideMenuItemMainRowTestTag"
const val SIDE_MENU_ITEM_INNER_ROW_TEST_TAG = "sideMenuItemInnerRowTestTag"
const val SIDE_MENU_ITEM_END_ICON_TEST_TAG = "sideMenuItemEndIconTestTag"
const val SIDE_MENU_ITEM_TEXT_TEST_TAG = "sideMenuItemTextTestTag"
const val NAV_BOTTOM_SECTION_SIDE_MENU_ITEM_TEST_TAG = "navBottomSectionSideMenuItemTestTag"
const val NAV_BOTTOM_SECTION_MAIN_BOX_TEST_TAG = "navBottomSectionMainBoxTestTag"
private val DividerColor = MenuItemColor.copy(alpha = 0.2f)

@Composable
fun AppDrawer(
  modifier: Modifier = Modifier,
  appUiState: AppMainUiState,
  navController: NavController,
  openDrawer: (Boolean) -> Unit,
  onSideMenuClick: (AppMainEvent) -> Unit,
  appVersionPair: Pair<Int, String>? = null,
) {
  val context = LocalContext.current
  val (versionCode, versionName) = remember { appVersionPair ?: context.appVersion() }

  val navigationConfiguration = appUiState.navigationConfiguration
  Scaffold(
    topBar = {
      Column(modifier = modifier.background(SideMenuDarkColor)) {
        // Display the app name and version
        NavTopSection(modifier, appUiState, versionCode, versionName)

        // Display menu action button
        MenuActionButton(
          modifier = modifier,
          navigationConfiguration = navigationConfiguration,
          navController = navController,
        )

        Divider(color = DividerColor)
      }
    },
    bottomBar = { // Display bottom section of the nav (sync)
      NavBottomSection(modifier, appUiState, onSideMenuClick, openDrawer)
    },
    backgroundColor = SideMenuDarkColor,
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        item {
          Column(modifier = modifier.background(SideMenuDarkColor)) {
            if (navigationConfiguration.clientRegisters.isNotEmpty()) {
              Text(
                text = stringResource(id = R.string.registers).uppercase(),
                fontSize = 14.sp,
                color = MenuItemColor,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
              )
            }
          }
        }

        // Display list of configurable client registers
        items(navigationConfiguration.clientRegisters, { it.id }) { navigationMenu ->
          SideMenuItem(
            imageConfig = navigationMenu.menuIconConfig,
            title = navigationMenu.display,
            endText = appUiState.registerCountMap[navigationMenu.id]?.toString() ?: "",
            showEndText = navigationMenu.showCount,
          ) {
            openDrawer(false)
            onSideMenuClick(
              AppMainEvent.TriggerWorkflow(navController = navController, navMenu = navigationMenu),
            )
          }
        }

        item {
          if (navigationConfiguration.bottomSheetRegisters?.registers?.isNotEmpty() == true) {
            Column {
              OtherPatientsItem(
                navigationConfiguration = navigationConfiguration,
                onSideMenuClick = onSideMenuClick,
                openDrawer = openDrawer,
                navController = navController,
              )
              if (navigationConfiguration.staticMenu.isNotEmpty()) Divider(color = DividerColor)
            }
          }
        }

        // Display list of configurable static menu
        items(navigationConfiguration.staticMenu, { it.id }) { navigationMenu ->
          SideMenuItem(
            imageConfig = navigationMenu.menuIconConfig,
            title = navigationMenu.display,
            endText = appUiState.registerCountMap[navigationMenu.id]?.toString() ?: "",
            showEndText = navigationMenu.showCount,
          ) {
            openDrawer(false)
            onSideMenuClick(
              AppMainEvent.TriggerWorkflow(navController = navController, navMenu = navigationMenu),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun NavBottomSection(
  modifier: Modifier,
  appUiState: AppMainUiState,
  onSideMenuClick: (AppMainEvent) -> Unit,
  openDrawer: (Boolean) -> Unit,
) {
  val context = LocalContext.current
  Box(
    modifier =
      modifier
        .testTag(NAV_BOTTOM_SECTION_MAIN_BOX_TEST_TAG)
        .background(SideMenuBottomItemDarkColor)
        .padding(horizontal = 16.dp, vertical = 4.dp),
  ) {
    SideMenuItem(
      modifier.testTag(NAV_BOTTOM_SECTION_SIDE_MENU_ITEM_TEST_TAG),
      imageConfig = ImageConfig(type = ICON_TYPE_LOCAL, "ic_sync"),
      title = stringResource(org.smartregister.fhircore.engine.R.string.sync),
      endText = appUiState.lastSyncTime,
      showEndText = true,
      endTextColor = SubtitleTextColor,
    ) {
      openDrawer(false)
      onSideMenuClick(AppMainEvent.SyncData(context))
    }
  }
}

@Composable
private fun OtherPatientsItem(
  navigationConfiguration: NavigationConfiguration,
  onSideMenuClick: (AppMainEvent) -> Unit,
  openDrawer: (Boolean) -> Unit,
  navController: NavController,
) {
  val context = LocalContext.current
  SideMenuItem(
    imageConfig = navigationConfiguration.bottomSheetRegisters?.menuIconConfig,
    title =
      navigationConfiguration.bottomSheetRegisters?.display!!.ifEmpty {
        stringResource(org.smartregister.fhircore.engine.R.string.other_patients)
      },
    endText = "",
    showEndText = false,
    endImageVector = Icons.Filled.KeyboardArrowRight,
    endTextColor = SubtitleTextColor,
  ) {
    openDrawer(false)
    onSideMenuClick(
      AppMainEvent.OpenRegistersBottomSheet(
        registersList = navigationConfiguration.bottomSheetRegisters?.registers,
        navController = navController,
        title =
          if (navigationConfiguration.bottomSheetRegisters?.display.isNullOrEmpty()) {
            context.getString(org.smartregister.fhircore.engine.R.string.other_patients)
          } else {
            navigationConfiguration.bottomSheetRegisters?.display
          },
      ),
    )
  }
}

@Composable
private fun NavTopSection(
  modifier: Modifier,
  appUiState: AppMainUiState,
  versionCode: Int,
  versionName: String?,
) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier =
      modifier
        .fillMaxWidth()
        .background(SideMenuTopItemDarkColor)
        .padding(horizontal = 16.dp)
        .testTag(NAV_TOP_SECTION_TEST_TAG),
  ) {
    Text(
      text = appUiState.appTitle,
      fontSize = 22.sp,
      color = AppTitleColor,
      modifier = modifier.padding(top = 16.dp, bottom = 16.dp, end = 8.dp),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = "$versionCode($versionName)",
      fontSize = 22.sp,
      color = AppTitleColor,
      modifier = modifier.padding(vertical = 16.dp),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun MenuActionButton(
  modifier: Modifier = Modifier,
  navigationConfiguration: NavigationConfiguration,
  navController: NavController,
) {
  if (
    navigationConfiguration.menuActionButton != null &&
      navigationConfiguration.menuActionButton?.visible == true
  ) {
    Row(
      modifier =
        modifier
          .fillMaxWidth()
          .clickable {
            navigationConfiguration.menuActionButton?.actions?.handleClickEvent(navController)
          }
          .padding(16.dp)
          .testTag(MENU_BUTTON_TEST_TAG),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier.background(MenuActionButtonTextColor).size(16.dp).clip(RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.Add,
          modifier = modifier.testTag(MENU_BUTTON_ICON_TEST_TAG),
          contentDescription = null,
        )
      }
      Spacer(modifier.width(16.dp))
      Text(
        modifier = modifier.testTag(MENU_BUTTON_TEXT_TEST_TAG),
        text =
          navigationConfiguration.menuActionButton?.display?.uppercase()
            ?: stringResource(id = org.smartregister.fhircore.engine.R.string.register_new_client),
        color = MenuActionButtonTextColor,
        fontSize = 18.sp,
      )
    }
  }
}

@Composable
private fun SideMenuItem(
  modifier: Modifier = Modifier,
  imageConfig: ImageConfig? = null,
  title: String,
  endText: String = "",
  endTextColor: Color = Color.White,
  showEndText: Boolean,
  endImageVector: ImageVector? = null,
  onSideMenuClick: () -> Unit,
) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier =
      modifier
        .fillMaxWidth()
        .clickable { onSideMenuClick() }
        .testTag(SIDE_MENU_ITEM_MAIN_ROW_TEST_TAG),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      modifier = modifier.testTag(SIDE_MENU_ITEM_INNER_ROW_TEST_TAG).padding(vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Image(
        paddingEnd = 10,
        imageProperties = ImageProperties(imageConfig = imageConfig, size = 32),
        tint = MenuItemColor,
      )
      SideMenuItemText(title = title, textColor = Color.White)
    }
    if (showEndText) {
      SideMenuItemText(title = endText, textColor = endTextColor)
    }
    endImageVector?.let { imageVector ->
      Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = MenuItemColor,
        modifier = modifier.padding(0.dp).testTag(SIDE_MENU_ITEM_END_ICON_TEST_TAG),
      )
    }
  }
}

@Composable
private fun SideMenuItemText(title: String, textColor: Color) {
  Text(
    text = title,
    color = textColor,
    fontSize = 18.sp,
    modifier = Modifier.testTag(SIDE_MENU_ITEM_TEXT_TEST_TAG),
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun AppDrawerPreview() {
  AppDrawer(
    appUiState =
      appMainUiStateOf(
        appTitle = "MOH VTS",
        username = "Demo",
        lastSyncTime = "05:30 PM, Mar 3",
        currentLanguage = "English",
        languages = listOf(Language("en", "English"), Language("sw", "Swahili")),
        navigationConfiguration =
          NavigationConfiguration(
            appId = "appId",
            configType = ConfigType.Navigation.name,
            staticMenu = listOf(),
            clientRegisters = listOf(),
            menuActionButton =
              NavigationMenuConfig(id = "id1", visible = true, display = "Register Household"),
          ),
      ),
    navController = rememberNavController(),
    openDrawer = {},
    onSideMenuClick = {},
    appVersionPair = Pair(1, "0.0.1"),
  )
}
