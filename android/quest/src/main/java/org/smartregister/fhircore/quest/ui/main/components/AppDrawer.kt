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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.smartregister.fhircore.engine.configuration.navigation.NavigationConfiguration
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.ui.theme.AppTitleColor
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

const val SIDE_MENU_ICON = "sideMenuIcon"
private val DividerColor = MenuItemColor.copy(alpha = 0.2f)

@Composable
fun AppDrawer(
  modifier: Modifier = Modifier,
  appUiState: AppMainUiState,
  navController: NavHostController,
  openDrawer: (Boolean) -> Unit,
  onSideMenuClick: (AppMainEvent) -> Unit
) {
  val context = LocalContext.current
  val (versionCode, versionName) = remember { context.appVersion() }

  Column(
    verticalArrangement = Arrangement.SpaceBetween,
    modifier = modifier.fillMaxHeight().background(SideMenuDarkColor)
  ) {

    // Display the app name and version
    NavTopSection(modifier, appUiState, versionCode, versionName)

    // Display menu action button
    MenuActionButton(
      modifier = modifier,
      navigationConfiguration = appUiState.navigationConfiguration
    )

    Divider(color = DividerColor)

    // Display list of configurable client registers
    Column(modifier.background(SideMenuDarkColor).padding(16.dp)) {
      if (appUiState.navigationConfiguration.clientRegisters.isNotEmpty()) {
        Text(
          text = stringResource(id = R.string.registers).uppercase(),
          fontSize = 14.sp,
          color = MenuItemColor
        )
      }
      Spacer(modifier = modifier.height(8.dp))
      ClientRegisterMenus(
        navigationConfiguration = appUiState.navigationConfiguration,
        context = context,
        navController = navController,
        openDrawer = openDrawer,
        onSideMenuClick = onSideMenuClick
      )
    }

    Divider(color = DividerColor)

    // Display list of configurable static menu
    StaticMenus(
      modifier = modifier.background(SideMenuDarkColor),
      navigationConfiguration = appUiState.navigationConfiguration,
      context = context,
      openDrawer = openDrawer
    )

    // Display bottom section of the nav (sync)
    NavBottomSection(modifier, appUiState, onSideMenuClick)
  }
}

@Composable
private fun NavBottomSection(
  modifier: Modifier,
  appUiState: AppMainUiState,
  onSideMenuClick: (AppMainEvent) -> Unit
) {
  Box(
    modifier =
      modifier.background(SideMenuBottomItemDarkColor).padding(horizontal = 16.dp, vertical = 4.dp)
  ) {
    SideMenuItem(
      iconResource = R.drawable.ic_sync,
      title = stringResource(R.string.sync),
      endText = appUiState.lastSyncTime,
      showEndText = true,
      endTextColor = SubtitleTextColor,
      onSideMenuClick = { onSideMenuClick(AppMainEvent.SyncData) }
    )
  }
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
      modifier.fillMaxWidth().background(SideMenuTopItemDarkColor).padding(horizontal = 16.dp)
  ) {
    Text(
      text = appUiState.appTitle,
      fontSize = 22.sp,
      color = AppTitleColor,
      modifier = modifier.padding(vertical = 16.dp)
    )
    Text(
      text = "$versionCode($versionName)",
      fontSize = 22.sp,
      color = AppTitleColor,
      modifier = modifier.padding(vertical = 16.dp)
    )
  }
}

@Composable
private fun ClientRegisterMenus(
  navigationConfiguration: NavigationConfiguration,
  context: Context,
  navController: NavHostController,
  openDrawer: (Boolean) -> Unit,
  onSideMenuClick: (AppMainEvent) -> Unit
) {
  LazyColumn {
    items(navigationConfiguration.clientRegisters, { it.id }) { navigationMenu ->
      SideMenuItem(
        // TODO Do we want save icons as base64 encoded strings
        iconResource = context.retrieveResourceId(navigationMenu.icon),
        title = navigationMenu.display,
        endText = "", // TODO compute register count
        showEndText = navigationMenu.showCount,
        onSideMenuClick = {
          openDrawer(false)
          onSideMenuClick(
            AppMainEvent.NavigateToScreen(
              navController = navController,
              actions = navigationMenu.actions,
              registerId = navigationMenu.id
            )
          )
        }
      )
    }
  }
}

@Composable
private fun StaticMenus(
  modifier: Modifier = Modifier,
  navigationConfiguration: NavigationConfiguration,
  context: Context,
  openDrawer: (Boolean) -> Unit
) {
  LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
    items(navigationConfiguration.staticMenu, { it.id }) { navigationMenu ->
      SideMenuItem(
        // TODO Do we want save icons as base64 encoded strings
        iconResource = context.retrieveResourceId(navigationMenu.icon),
        title = navigationMenu.display,
        endText = "", // TODO compute register count
        showEndText = navigationMenu.showCount,
        onSideMenuClick = {
          openDrawer(false)
          // TODO navigate to relevant screen/initiate an action
        }
      )
    }
  }
}

@Composable
private fun MenuActionButton(
  modifier: Modifier = Modifier,
  navigationConfiguration: NavigationConfiguration
) {
  if (navigationConfiguration.menuActionButton != null) {
    Row(
      modifier =
        modifier
          .fillMaxWidth()
          .clickable { /*TODO handle main action button click*/}
          .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier.background(MenuActionButtonTextColor).size(16.dp).clip(RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Filled.Add,
          contentDescription = null,
        )
      }
      Spacer(modifier.width(16.dp))
      Text(
        text = navigationConfiguration.menuActionButton?.display?.uppercase()
            ?: stringResource(id = R.string.register_new_client),
        color = MenuActionButtonTextColor,
        fontSize = 18.sp
      )
    }
  }
}

@Composable
private fun SideMenuItem(
  modifier: Modifier = Modifier,
  iconResource: Int?,
  title: String,
  endText: String = "",
  endTextColor: Color = Color.White,
  showEndText: Boolean,
  onSideMenuClick: () -> Unit
) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = modifier.fillMaxWidth().clickable { onSideMenuClick() },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(modifier = modifier.padding(vertical = 16.dp)) {
      if (iconResource != null) {
        Icon(
          modifier = modifier.padding(end = 10.dp).size(24.dp),
          painter = painterResource(id = iconResource),
          contentDescription = SIDE_MENU_ICON,
          tint = MenuItemColor
        )
      }
      SideMenuItemText(title = title, textColor = Color.White)
    }

    if (showEndText) {
      SideMenuItemText(title = endText, textColor = endTextColor)
    }
  }
}

@Composable
private fun SideMenuItemText(title: String, textColor: Color) {
  Text(text = title, color = textColor, fontSize = 18.sp)
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
        navigationConfiguration = NavigationConfiguration(appId = "appId")
      ),
    navController = rememberNavController(),
    openDrawer = {},
    onSideMenuClick = {}
  )
}
