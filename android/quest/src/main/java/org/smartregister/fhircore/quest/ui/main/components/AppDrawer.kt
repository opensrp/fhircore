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

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.android.fhir.sync.CurrentSyncJobStatus
import com.google.android.fhir.sync.SyncJobStatus
import com.google.android.fhir.sync.SyncOperation
import java.time.OffsetDateTime
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_LOCAL
import org.smartregister.fhircore.engine.configuration.navigation.ImageConfig
import org.smartregister.fhircore.engine.configuration.navigation.NavigationConfiguration
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.configuration.view.ImageProperties
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.ui.theme.AppTitleColor
import org.smartregister.fhircore.engine.ui.theme.DangerColor
import org.smartregister.fhircore.engine.ui.theme.MenuActionButtonTextColor
import org.smartregister.fhircore.engine.ui.theme.MenuItemColor
import org.smartregister.fhircore.engine.ui.theme.SideMenuBottomItemDarkColor
import org.smartregister.fhircore.engine.ui.theme.SideMenuDarkColor
import org.smartregister.fhircore.engine.ui.theme.SideMenuTopItemDarkColor
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.ui.theme.SuccessColor
import org.smartregister.fhircore.engine.ui.theme.SyncBarBackgroundColor
import org.smartregister.fhircore.engine.ui.theme.WarningColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.engine.util.extension.appVersion
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.main.AppMainEvent
import org.smartregister.fhircore.quest.ui.main.AppMainUiState
import org.smartregister.fhircore.quest.ui.main.appMainUiStateOf
import org.smartregister.fhircore.quest.ui.shared.components.Image
import org.smartregister.fhircore.quest.ui.shared.components.SyncStatusView
import org.smartregister.fhircore.quest.ui.shared.components.TRANSPARENCY
import org.smartregister.fhircore.quest.ui.shared.models.AppDrawerUIState
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
private val DividerColor = MenuItemColor.copy(alpha = 0.2f)

@Composable
fun AppDrawer(
  modifier: Modifier = Modifier,
  appUiState: AppMainUiState,
  appDrawerUIState: AppDrawerUIState = AppDrawerUIState(),
  navController: NavController,
  openDrawer: (Boolean) -> Unit,
  onSideMenuClick: (AppMainEvent) -> Unit,
  appVersionPair: Pair<Int, String>? = null,
  unSyncedResourceCount: MutableIntState,
  onCountUnSyncedResources: () -> Unit,
) {
  val context = LocalContext.current
  val (versionCode, versionName) = remember { appVersionPair ?: context.appVersion() }
  val navigationConfiguration = appUiState.navigationConfiguration

  LaunchedEffect(Unit) { onCountUnSyncedResources() }

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
      NavBottomSection(
        appUiState = appUiState,
        appDrawerUIState = appDrawerUIState,
        unSyncedResourceCount = unSyncedResourceCount,
        onSideMenuClick = onSideMenuClick,
        openDrawer = openDrawer,
      )
    },
  ) { innerPadding ->
    Box(
      modifier = modifier.padding(innerPadding).background(SideMenuDarkColor).fillMaxSize(),
    ) {
      LazyColumn(modifier = modifier) {
        item {
          Column(modifier = modifier.padding(horizontal = 16.dp)) {
            if (navigationConfiguration.clientRegisters.size > 1) {
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
            endTextColor = MenuItemColor,
          ) {
            openDrawer(false)
            onSideMenuClick(
              AppMainEvent.TriggerWorkflow(navController = navController, navMenu = navigationMenu),
            )
          }
        }

        item {
          if (navigationConfiguration.bottomSheetRegisters?.registers?.isNotEmpty() == true) {
            OtherPatientsItem(
              navigationConfiguration = navigationConfiguration,
              onSideMenuClick = onSideMenuClick,
              openDrawer = openDrawer,
              navController = navController,
            )
            if (navigationConfiguration.staticMenu.isNotEmpty()) Divider(color = DividerColor)
          }
        }

        item { Divider(color = DividerColor) }

        // Display list of configurable static menu
        items(navigationConfiguration.staticMenu, { it.id }) { navigationMenu ->
          SideMenuItem(
            imageConfig = navigationMenu.menuIconConfig,
            title = navigationMenu.display,
            endText = appUiState.registerCountMap[navigationMenu.id]?.toString() ?: "",
            showEndText = navigationMenu.showCount,
            endTextColor = MenuItemColor,
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
  appUiState: AppMainUiState,
  appDrawerUIState: AppDrawerUIState,
  unSyncedResourceCount: MutableIntState,
  onSideMenuClick: (AppMainEvent) -> Unit,
  openDrawer: (Boolean) -> Unit,
) {
  val currentSyncJobStatus = appDrawerUIState.currentSyncJobStatus
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var showDefaultSyncStatus by remember { mutableStateOf(false) }
  val syncStatusBackgroundColor =
    when (currentSyncJobStatus) {
      is CurrentSyncJobStatus.Failed -> DangerColor.copy(alpha = TRANSPARENCY)
      is CurrentSyncJobStatus.Running -> SyncBarBackgroundColor
      is CurrentSyncJobStatus.Succeeded -> SuccessColor.copy(alpha = TRANSPARENCY)
      else -> Color.Unspecified
    }
  Box(
    modifier = Modifier.background(syncStatusBackgroundColor).fillMaxWidth(),
    contentAlignment = Alignment.Center,
  ) {
    when (currentSyncJobStatus) {
      is CurrentSyncJobStatus.Running -> {
        SyncStatusView(
          isSyncUpload = appDrawerUIState.isSyncUpload,
          currentSyncJobStatus = currentSyncJobStatus,
          minimized = false,
          progressPercentage = appDrawerUIState.percentageProgress,
          onCancel = {
            onSideMenuClick(AppMainEvent.CancelSyncData(context))
            openDrawer(false)
          },
        )
        SideEffect { showDefaultSyncStatus = false }
      }
      is CurrentSyncJobStatus.Failed -> {
        SyncStatusView(
          isSyncUpload = appDrawerUIState.isSyncUpload,
          currentSyncJobStatus = currentSyncJobStatus,
          minimized = false,
        ) {
          openDrawer(false)
          onSideMenuClick(AppMainEvent.SyncData(context))
        }
      }
      is CurrentSyncJobStatus.Succeeded -> {
        LaunchedEffect(Unit) {
          coroutineScope.launch {
            delay(7.seconds)
            showDefaultSyncStatus = true
          }
        }
        if (showDefaultSyncStatus) {
          DefaultSyncStatus(
            appUiState = appUiState,
            context = context,
            unSyncedResourceCount = unSyncedResourceCount,
            openDrawer = openDrawer,
            onSideMenuClick = onSideMenuClick,
          )
        } else {
          SyncStatusView(
            isSyncUpload = appDrawerUIState.isSyncUpload,
            currentSyncJobStatus = currentSyncJobStatus,
            minimized = false,
          )
        }
      }
      else -> {
        DefaultSyncStatus(
          appUiState = appUiState,
          context = context,
          unSyncedResourceCount = unSyncedResourceCount,
          openDrawer = openDrawer,
          onSideMenuClick = onSideMenuClick,
        )
      }
    }
  }
}

@Composable
private fun DefaultSyncStatus(
  appUiState: AppMainUiState,
  context: Context,
  unSyncedResourceCount: MutableIntState,
  openDrawer: (Boolean) -> Unit,
  onSideMenuClick: (AppMainEvent) -> Unit,
) {
  val allDataSynced = unSyncedResourceCount.intValue == 0
  Box(
    modifier =
      Modifier.background(
          if (allDataSynced) {
            SideMenuBottomItemDarkColor
          } else {
            WarningColor.copy(alpha = TRANSPARENCY)
          },
        )
        .padding(vertical = 16.dp),
  ) {
    SideMenuItem(
      modifier = Modifier,
      imageConfig = ImageConfig(type = ICON_TYPE_LOCAL, reference = "ic_sync"),
      title =
        stringResource(
          if (allDataSynced) {
            org.smartregister.fhircore.engine.R.string.manual_sync
          } else {
            org.smartregister.fhircore.engine.R.string.sync
          },
        ),
      subTitle =
        if (allDataSynced) {
          null
        } else {
          stringResource(org.smartregister.fhircore.engine.R.string.unsynced_data_present)
        },
      subTitleTextColor = SubtitleTextColor,
      endText = appUiState.lastSyncTime,
      padding = 0,
      showEndText = true,
      endTextColor = if (allDataSynced) SubtitleTextColor else Color.Unspecified,
      mainTextColor = if (allDataSynced) Color.White else Color.Unspecified,
      mainTextBold = !allDataSynced,
      startIcon = if (allDataSynced) null else Icons.Default.Error,
      startIconColor = if (allDataSynced) null else WarningColor,
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
    endImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
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
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = appUiState.appTitle,
      fontSize = 18.sp,
      color = AppTitleColor,
      modifier = modifier.padding(top = 16.dp, bottom = 16.dp, end = 8.dp),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = "$versionCode($versionName)",
      fontSize = 14.sp,
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
        fontSize = 16.sp,
      )
    }
  }
}

@Composable
private fun SideMenuItem(
  modifier: Modifier = Modifier,
  imageConfig: ImageConfig? = null,
  mainTextColor: Color = Color.White,
  title: String,
  subTitle: String? = null,
  subTitleTextColor: Color = SubtitleTextColor,
  endText: String = "",
  endTextColor: Color = Color.White,
  padding: Int = 12,
  showEndText: Boolean,
  endImageVector: ImageVector? = null,
  mainTextBold: Boolean = false,
  startIcon: ImageVector? = null,
  startIconColor: Color? = null,
  onSideMenuClick: () -> Unit,
) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier =
      modifier
        .fillMaxWidth()
        .clickable { onSideMenuClick() }
        .padding(vertical = padding.dp)
        .testTag(SIDE_MENU_ITEM_MAIN_ROW_TEST_TAG)
        .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      modifier = modifier.padding(end = 16.dp).testTag(SIDE_MENU_ITEM_INNER_ROW_TEST_TAG),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (startIcon != null) {
        Icon(
          imageVector = startIcon,
          contentDescription = null,
          tint = startIconColor ?: MenuItemColor,
          modifier = Modifier.padding(end = 16.dp),
        )
      } else {
        Image(
          paddingEnd = 8,
          imageProperties = ImageProperties(imageConfig = imageConfig, size = 32),
          tint = MenuItemColor,
          navController = rememberNavController(),
        )
      }
      Column {
        SideMenuItemText(title = title, textColor = mainTextColor, boldText = mainTextBold)
        if (!subTitle.isNullOrBlank()) {
          SideMenuItemText(
            title = subTitle,
            textColor = subTitleTextColor,
            boldText = false,
            textSize = 14,
          )
        }
      }
    }
    if (showEndText) {
      SideMenuItemText(title = endText, textColor = endTextColor, textSize = 14)
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
private fun SideMenuItemText(
  title: String,
  textColor: Color,
  textSize: Int = 16,
  boldText: Boolean = false,
) {
  Text(
    text = title,
    color = textColor,
    fontSize = textSize.sp,
    fontWeight = if (boldText) FontWeight.Bold else FontWeight.Normal,
    modifier = Modifier.testTag(SIDE_MENU_ITEM_TEXT_TEST_TAG),
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun AppDrawerPreview() {
  AppTheme {
    AppDrawer(
      appUiState =
        appMainUiStateOf(
          appTitle = "MOH VTS",
          username = "Demo",
          lastSyncTime = "Mar 3, 05:30 PM",
          currentLanguage = "English",
          languages = listOf(Language("en", "English"), Language("sw", "Swahili")),
          navigationConfiguration =
            NavigationConfiguration(
              appId = "appId",
              configType = ConfigType.Navigation.name,
              staticMenu = listOf(),
              clientRegisters =
                listOf(
                  NavigationMenuConfig(id = "id0", visible = true, display = "Households"),
                ),
              menuActionButton =
                NavigationMenuConfig(id = "id1", visible = true, display = "Register Household"),
            ),
        ),
      navController = rememberNavController(),
      openDrawer = {},
      onSideMenuClick = {},
      appVersionPair = Pair(1, "0.0.1"),
      unSyncedResourceCount = remember { mutableIntStateOf(0) },
      onCountUnSyncedResources = {},
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun AppDrawerWithUnSyncedDataPreview() {
  AppTheme {
    AppDrawer(
      appUiState =
        appMainUiStateOf(
          appTitle = "MOH VTS",
          username = "Demo",
          lastSyncTime = "Aug 16, 06:54 PM",
          currentLanguage = "English",
          languages = listOf(Language("en", "English"), Language("sw", "Swahili")),
          navigationConfiguration =
            NavigationConfiguration(
              appId = "appId",
              configType = ConfigType.Navigation.name,
              staticMenu = listOf(),
              clientRegisters =
                listOf(
                  NavigationMenuConfig(id = "id0", visible = true, display = "Households"),
                  NavigationMenuConfig(id = "id2", visible = true, display = "PNC"),
                  NavigationMenuConfig(id = "id3", visible = true, display = "ANC"),
                  NavigationMenuConfig(id = "id4", visible = true, display = "Family Planning"),
                ),
              menuActionButton =
                NavigationMenuConfig(id = "id1", visible = true, display = "Register Household"),
            ),
        ),
      navController = rememberNavController(),
      openDrawer = {},
      onSideMenuClick = {},
      appVersionPair = Pair(1, "0.0.1"),
      unSyncedResourceCount = remember { mutableIntStateOf(10) },
      onCountUnSyncedResources = {},
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun AppDrawerOnSyncCompletePreview() {
  AppTheme {
    AppDrawer(
      appUiState =
        appMainUiStateOf(
          appTitle = "MOH VTS",
          username = "Demo",
          lastSyncTime = "Mar 3, 05:30 PM",
          currentLanguage = "English",
          languages = listOf(Language("en", "English"), Language("sw", "Swahili")),
          navigationConfiguration =
            NavigationConfiguration(
              appId = "appId",
              configType = ConfigType.Navigation.name,
              staticMenu = listOf(),
              clientRegisters =
                listOf(
                  NavigationMenuConfig(id = "id0", visible = true, display = "Households"),
                  NavigationMenuConfig(id = "id2", visible = true, display = "PNC"),
                  NavigationMenuConfig(id = "id3", visible = true, display = "ANC"),
                  NavigationMenuConfig(id = "id4", visible = true, display = "Family Planning"),
                ),
              menuActionButton =
                NavigationMenuConfig(id = "id1", visible = true, display = "Register Household"),
            ),
        ),
      appDrawerUIState =
        AppDrawerUIState(
          currentSyncJobStatus = CurrentSyncJobStatus.Succeeded(OffsetDateTime.now()),
        ),
      navController = rememberNavController(),
      openDrawer = {},
      onSideMenuClick = {},
      appVersionPair = Pair(1, "0.0.1"),
      unSyncedResourceCount = remember { mutableIntStateOf(0) },
      onCountUnSyncedResources = {},
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun AppDrawerOnSyncFailedPreview() {
  AppTheme {
    AppDrawer(
      appUiState =
        appMainUiStateOf(
          appTitle = "MOH VTS",
          username = "Demo",
          lastSyncTime = "Mar 3, 05:30 PM",
          currentLanguage = "English",
          languages = listOf(Language("en", "English"), Language("sw", "Swahili")),
          navigationConfiguration =
            NavigationConfiguration(
              appId = "appId",
              configType = ConfigType.Navigation.name,
              staticMenu = listOf(),
              clientRegisters =
                listOf(
                  NavigationMenuConfig(id = "id0", visible = true, display = "Households"),
                  NavigationMenuConfig(id = "id2", visible = true, display = "PNC"),
                  NavigationMenuConfig(id = "id3", visible = true, display = "ANC"),
                  NavigationMenuConfig(id = "id4", visible = true, display = "Family Planning"),
                ),
              menuActionButton =
                NavigationMenuConfig(id = "id1", visible = true, display = "Register Household"),
            ),
        ),
      appDrawerUIState =
        AppDrawerUIState(
          currentSyncJobStatus = CurrentSyncJobStatus.Failed(OffsetDateTime.now()),
        ),
      navController = rememberNavController(),
      openDrawer = {},
      onSideMenuClick = {},
      appVersionPair = Pair(1, "0.0.1"),
      unSyncedResourceCount = remember { mutableIntStateOf(0) },
      onCountUnSyncedResources = {},
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun AppDrawerOnSyncRunningPreview() {
  AppTheme {
    AppDrawer(
      appUiState =
        appMainUiStateOf(
          appTitle = "MOH VTS",
          username = "Demo",
          lastSyncTime = "Mar 3, 05:30 PM",
          currentLanguage = "English",
          languages = listOf(Language("en", "English"), Language("sw", "Swahili")),
          navigationConfiguration =
            NavigationConfiguration(
              appId = "appId",
              configType = ConfigType.Navigation.name,
              staticMenu = listOf(),
              clientRegisters =
                listOf(
                  NavigationMenuConfig(id = "id0", visible = true, display = "Households"),
                  NavigationMenuConfig(id = "id2", visible = true, display = "PNC"),
                  NavigationMenuConfig(id = "id3", visible = true, display = "ANC"),
                  NavigationMenuConfig(id = "id4", visible = true, display = "Family Planning"),
                ),
              menuActionButton =
                NavigationMenuConfig(id = "id1", visible = true, display = "Register Household"),
            ),
        ),
      appDrawerUIState =
        AppDrawerUIState(
          currentSyncJobStatus =
            CurrentSyncJobStatus.Running(SyncJobStatus.InProgress(SyncOperation.DOWNLOAD, 200, 35)),
        ),
      navController = rememberNavController(),
      openDrawer = {},
      onSideMenuClick = {},
      appVersionPair = Pair(1, "0.0.1"),
      unSyncedResourceCount = remember { mutableIntStateOf(0) },
      onCountUnSyncedResources = {},
    )
  }
}
