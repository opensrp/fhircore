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

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_LOCAL
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_REMOTE
import org.smartregister.fhircore.engine.configuration.navigation.MenuIconConfig
import org.smartregister.fhircore.engine.configuration.navigation.NavigationConfiguration
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.ui.theme.AppTitleColor
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.MenuActionButtonTextColor
import org.smartregister.fhircore.engine.ui.theme.MenuItemColor
import org.smartregister.fhircore.engine.ui.theme.SideMenuBottomItemDarkColor
import org.smartregister.fhircore.engine.ui.theme.SideMenuDarkColor
import org.smartregister.fhircore.engine.ui.theme.SideMenuTopItemDarkColor
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.engine.util.extension.appVersion
import org.smartregister.fhircore.engine.util.extension.retrieveResourceId
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.main.AppMainEvent
import org.smartregister.fhircore.quest.ui.main.AppMainUiState
import org.smartregister.fhircore.quest.ui.main.appMainUiStateOf
import org.smartregister.fhircore.quest.ui.shared.components.MenuIcon
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent

const val SIDE_MENU_ICON = "sideMenuIcon"
private val DividerColor = MenuItemColor.copy(alpha = 0.2f)
const val NAV_TOP_SECTION_TEST_TAG = "navTopSectionTestTag"
const val MENU_BUTTON_TEST_TAG = "menuButtonTestTag"
const val MENU_BUTTON_ICON_TEST_TAG = "menuButtonIconTestTag"
const val MENU_BUTTON_TEXT_TEST_TAG = "menuButtonTextTestTag"
const val SIDE_MENU_ITEM_MAIN_ROW_TEST_TAG = "sideMenuItemMainRowTestTag"
const val SIDE_MENU_ITEM_INNER_ROW_TEST_TAG = "sideMenuItemInnerRowTestTag"
const val SIDE_MENU_ITEM_LOCAL_ICON_TEST_TAG = "sideMenuItemLocalIconTestTag"
const val SIDE_MENU_ITEM_END_ICON_TEST_TAG = "sideMenuItemEndIconTestTag"
const val SIDE_MENU_ITEM_TEXT_TEST_TAG = "sideMenuItemTextTestTag"
const val NAV_BOTTOM_SECTION_SIDE_MENU_ITEM_TEST_TAG = "navBottomSectionSideMenuItemTestTag"
const val NAV_BOTTOM_SECTION_MAIN_BOX_TEST_TAG = "navBottomSectionMainBoxTestTag"
const val NAV_CLIENT_REGISTER_MENUS_LIST = "navClientRegisterMenusList"

@Composable
fun AppDrawer(
    modifier: Modifier = Modifier,
    appUiState: AppMainUiState,
    navController: NavController,
    openDrawer: (Boolean) -> Unit,
    onSideMenuClick: (AppMainEvent) -> Unit,
    appVersionPair: Pair<Int, String>? = null
) {
  val context = LocalContext.current
  val (versionCode, versionName) = remember { appVersionPair ?: context.appVersion() }

  Scaffold(
      topBar = {
        Column(modifier = modifier.background(SideMenuDarkColor)) {
          // Display the app name and version
          NavTopSection(modifier, appUiState, versionCode, versionName)

          // Display menu action button
          MenuActionButton(
              modifier = modifier,
              navigationConfiguration = appUiState.navigationConfiguration,
              navController = navController)

          Divider(color = DividerColor)
        }
      },
      bottomBar = { // Display bottom section of the nav (sync)
        NavBottomSection(modifier, context, appUiState, onSideMenuClick)
      },
      backgroundColor = SideMenuDarkColor) { innerPadding ->
        Box(modifier = modifier.padding(innerPadding)) {
          Column {
            // Display list of configurable client registers
            Column(modifier = modifier.background(SideMenuDarkColor).padding(16.dp)) {
              if (appUiState.navigationConfiguration.clientRegisters.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.registers).uppercase(),
                    fontSize = 14.sp,
                    color = MenuItemColor)
              }
              Spacer(modifier = modifier.height(8.dp))
              ClientRegisterMenus(
                  appUiState = appUiState,
                  context = context,
                  navController = navController,
                  openDrawer = openDrawer,
                  onSideMenuClick = onSideMenuClick)
              if (appUiState.navigationConfiguration.bottomSheetRegisters
                  ?.registers
                  ?.isNotEmpty() == true) {
                OtherPatientsItem(
                    navigationConfiguration = appUiState.navigationConfiguration,
                    onSideMenuClick = onSideMenuClick,
                    openDrawer = openDrawer,
                    navController = navController)
              }
            }

            Divider(color = DividerColor)

            // Display list of configurable static menu
            StaticMenus(
                modifier = modifier.background(SideMenuDarkColor),
                navigationConfiguration = appUiState.navigationConfiguration,
                context = context,
                navController = navController,
                openDrawer = openDrawer,
                onSideMenuClick = onSideMenuClick,
                appUiState = appUiState)
          }
        }
      }
}

@Composable
private fun NavBottomSection(
    modifier: Modifier,
    context: Context,
    appUiState: AppMainUiState,
    onSideMenuClick: (AppMainEvent) -> Unit
) {
  Box(
      modifier =
          modifier
              .testTag(NAV_BOTTOM_SECTION_MAIN_BOX_TEST_TAG)
              .background(SideMenuBottomItemDarkColor)
              .padding(horizontal = 16.dp, vertical = 4.dp)) {
        SideMenuItem(
            modifier.testTag(NAV_BOTTOM_SECTION_SIDE_MENU_ITEM_TEST_TAG),
            context = context,
            menuIconConfig = MenuIconConfig(type = ICON_TYPE_LOCAL, "ic_sync"),
            title = stringResource(R.string.sync),
            endText = appUiState.lastSyncTime,
            showEndText = true,
            endTextColor = SubtitleTextColor) {
              onSideMenuClick(AppMainEvent.SyncData)
            }
      }
}

@Composable
private fun OtherPatientsItem(
    navigationConfiguration: NavigationConfiguration,
    onSideMenuClick: (AppMainEvent) -> Unit,
    openDrawer: (Boolean) -> Unit,
    navController: NavController
) {
  SideMenuItem(
      title = stringResource(R.string.other_patients),
      endText = "",
      showEndText = false,
      endImageVector = Icons.Filled.KeyboardArrowRight,
      endTextColor = SubtitleTextColor,
      onSideMenuClick = {
        openDrawer(false)
        onSideMenuClick(
            AppMainEvent.OpenRegistersBottomSheet(
                registersList = navigationConfiguration.bottomSheetRegisters?.registers,
                navController = navController))
      },
      menuIconConfig = navigationConfiguration.bottomSheetRegisters?.menuIconConfig)
}

@Composable
private fun NavTopSection(
    modifier: Modifier,
    appUiState: AppMainUiState,
    versionCode: Int,
    versionName: String?
) {
  Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier =
          modifier
              .fillMaxWidth()
              .background(SideMenuTopItemDarkColor)
              .padding(horizontal = 16.dp)
              .testTag(NAV_TOP_SECTION_TEST_TAG)) {
        Text(
            text = appUiState.appTitle,
            fontSize = 22.sp,
            color = AppTitleColor,
            modifier = modifier.padding(vertical = 16.dp))
        Text(
            text = "$versionCode($versionName)",
            fontSize = 22.sp,
            color = AppTitleColor,
            modifier = modifier.padding(vertical = 16.dp))
      }
}

@Composable
private fun ClientRegisterMenus(
    appUiState: AppMainUiState,
    context: Context,
    navController: NavController,
    openDrawer: (Boolean) -> Unit,
    onSideMenuClick: (AppMainEvent) -> Unit
) {
  LazyColumn(modifier = Modifier.testTag(NAV_CLIENT_REGISTER_MENUS_LIST)) {
    items(appUiState.navigationConfiguration.clientRegisters, { it.id }) { navigationMenu ->
      SideMenuItem(
          context = context,
          menuIconConfig = navigationMenu.menuIconConfig,
          title = navigationMenu.display,
          endText = appUiState.registerCountMap[navigationMenu.id]?.toString() ?: "",
          showEndText = navigationMenu.showCount) {
            openDrawer(false)
            onSideMenuClick(
                AppMainEvent.TriggerWorkflow(
                    navController = navController, navMenu = navigationMenu))
          }
    }
  }
}

@Composable
private fun StaticMenus(
    modifier: Modifier = Modifier,
    navigationConfiguration: NavigationConfiguration,
    context: Context,
    navController: NavController,
    openDrawer: (Boolean) -> Unit,
    onSideMenuClick: (AppMainEvent) -> Unit,
    appUiState: AppMainUiState
) {
  LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
    items(navigationConfiguration.staticMenu, { it.id }) { navigationMenu ->
      SideMenuItem(
          context = context,
          menuIconConfig = navigationMenu.menuIconConfig,
          title = navigationMenu.display,
          endText = appUiState.registerCountMap[navigationMenu.id]?.toString() ?: "",
          showEndText = navigationMenu.showCount) {
            openDrawer(false)
            onSideMenuClick(
                AppMainEvent.TriggerWorkflow(
                    navController = navController, navMenu = navigationMenu))
          }
    }
  }
}

@Composable
private fun MenuActionButton(
    modifier: Modifier = Modifier,
    navigationConfiguration: NavigationConfiguration,
    navController: NavController
) {
  if (navigationConfiguration.menuActionButton != null) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable {
                  navigationConfiguration.menuActionButton?.actions?.handleClickEvent(navController)
                }
                .padding(16.dp)
                .testTag(MENU_BUTTON_TEST_TAG),
        verticalAlignment = Alignment.CenterVertically) {
          Box(
              modifier
                  .background(MenuActionButtonTextColor)
                  .size(16.dp)
                  .clip(RoundedCornerShape(2.dp)),
              contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    modifier = modifier.testTag(MENU_BUTTON_ICON_TEST_TAG),
                    contentDescription = null,
                )
              }
          Spacer(modifier.width(16.dp))
          Text(
              modifier = modifier.testTag(MENU_BUTTON_TEXT_TEST_TAG),
              text = navigationConfiguration.menuActionButton?.display?.uppercase()
                      ?: stringResource(id = R.string.register_new_client),
              color = MenuActionButtonTextColor,
              fontSize = 18.sp)
        }
  }
}

@Composable
private fun SideMenuItem(
    modifier: Modifier = Modifier,
    context: Context? = null,
    menuIconConfig: MenuIconConfig? = null,
    title: String,
    endText: String = "",
    endTextColor: Color = Color.White,
    showEndText: Boolean,
    endImageVector: ImageVector? = null,
    onSideMenuClick: () -> Unit
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
    Row(modifier = modifier.testTag(SIDE_MENU_ITEM_INNER_ROW_TEST_TAG).padding(vertical = 16.dp)) {
      if (menuIconConfig != null) {
        when (menuIconConfig.type) {
          ICON_TYPE_LOCAL -> {
            context?.retrieveResourceId(menuIconConfig.reference)?.let { drawableId ->
              Icon(
                  modifier =
                      modifier
                          .testTag(SIDE_MENU_ITEM_LOCAL_ICON_TEST_TAG)
                          .padding(end = 10.dp)
                          .size(24.dp),
                  painter = painterResource(id = drawableId),
                  contentDescription = SIDE_MENU_ICON,
                  tint = MenuItemColor)
            }
          }
          ICON_TYPE_REMOTE -> {
            MenuIcon(modifier = modifier, menuIconConfig = menuIconConfig)
          }
        }
      }
      SideMenuItemText(title = title, textColor = Color.White)
    }

    if (showEndText) {
      SideMenuItemText(title = endText, textColor = endTextColor)
    }
    endImageVector?.let { imageVector ->
      Icon(
          imageVector,
          contentDescription = null,
          tint = DefaultColor.copy(alpha = 0.7f),
          modifier =
              Modifier.padding(end = 10.dp)
                  .align(Alignment.CenterVertically)
                  .testTag(SIDE_MENU_ITEM_END_ICON_TEST_TAG))
    }
  }
}

@Composable
private fun SideMenuItemText(title: String, textColor: Color) {
  Text(
      text = title,
      color = textColor,
      fontSize = 18.sp,
      modifier = Modifier.testTag(SIDE_MENU_ITEM_TEXT_TEST_TAG))
}

@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
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
                          NavigationMenuConfig(
                              id = "id1", visible = true, display = "Register Household"))),
      navController = rememberNavController(),
      openDrawer = {},
      onSideMenuClick = {},
      appVersionPair = Pair(1, "0.0.1"))
}
