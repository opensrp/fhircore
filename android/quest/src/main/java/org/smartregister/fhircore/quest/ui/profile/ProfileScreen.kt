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

package org.smartregister.fhircore.quest.ui.profile

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.flow.SharedFlow
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.ImageProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.domain.model.TopBarConfig
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.fhircore.quest.ui.shared.components.CompoundText
import org.smartregister.fhircore.quest.ui.shared.components.ExtendedFab
import org.smartregister.fhircore.quest.ui.shared.components.Image
import org.smartregister.fhircore.quest.ui.shared.components.SnackBarMessage
import org.smartregister.fhircore.quest.ui.shared.components.ViewRenderer
import org.smartregister.fhircore.quest.util.extensions.hookSnackBar
import org.smartregister.fhircore.quest.util.extensions.isScrollingDown

const val DROPDOWN_MENU_TEST_TAG = "dropDownMenuTestTag"
const val FAB_BUTTON_TEST_TAG = "fabButtonTestTag"
const val PROFILE_TOP_BAR_TEST_TAG = "profileTopBarTestTag"
const val PROFILE_TOP_BAR_ICON_TEST_TAG = "profileTopBarIconTestTag"
const val PADDING_BOTTOM_WITH_FAB = 80
const val PADDING_BOTTOM_WITHOUT_FAB = 32

@Composable
fun ProfileScreen(
  modifier: Modifier = Modifier,
  navController: NavController,
  profileUiState: ProfileUiState,
  snackStateFlow: SharedFlow<SnackBarMessageConfig>,
  decodedImageMap: MutableMap<String, Bitmap> = mutableMapOf(),
  onEvent: (ProfileEvent) -> Unit,
) {
  val scaffoldState = rememberScaffoldState()
  val lazyListState = rememberLazyListState()

  LaunchedEffect(Unit) {
    snackStateFlow.hookSnackBar(scaffoldState, profileUiState.resourceData, navController)
  }
  val fabActions = profileUiState.profileConfiguration?.fabActions
  Scaffold(
    scaffoldState = scaffoldState,
    topBar = {
      if (profileUiState.profileConfiguration?.topAppBar == null) {
        SimpleTopAppBar(
          modifier = modifier,
          navController = navController,
          elevation = 4,
          profileUiState = profileUiState,
          lazyListState = lazyListState,
          onEvent = onEvent,
          collapsible = false,
        )
      } else {
        CustomProfileTopAppBar(
          navController = navController,
          profileUiState = profileUiState,
          onEvent = onEvent,
          lazyListState = lazyListState,
        )
      }
    },
    floatingActionButton = {
      if (!fabActions.isNullOrEmpty() && fabActions.first().visible) {
        ExtendedFab(
          modifier = Modifier.testTag(FAB_BUTTON_TEST_TAG),
          fabActions = fabActions,
          resourceData = profileUiState.resourceData,
          navController = navController,
          lazyListState = lazyListState,
        )
      }
    },
    isFloatingActionButtonDocked = true,
    snackbarHost = { snackBarHostState ->
      SnackBarMessage(
        snackBarHostState = snackBarHostState,
        backgroundColorHex = profileUiState.snackBarTheme.backgroundColor,
        actionColorHex = profileUiState.snackBarTheme.actionTextColor,
        contentColorHex = profileUiState.snackBarTheme.messageTextColor,
      )
    },
  ) { innerPadding ->
    Box(
      modifier =
        modifier
          .background(profileUiState.profileConfiguration?.contentBackgroundColor.parseColor())
          .fillMaxSize()
          .padding(innerPadding),
    ) {
      if (profileUiState.showDataLoadProgressIndicator) {
        CircularProgressIndicator(
          modifier = modifier.align(Alignment.Center).size(24.dp),
          strokeWidth = 1.8.dp,
          color = MaterialTheme.colors.primary,
        )
      }
      LazyColumn(
        state = lazyListState,
        modifier =
          Modifier.padding(
            bottom =
              if (!fabActions.isNullOrEmpty() && fabActions.first().visible) {
                PADDING_BOTTOM_WITH_FAB.dp
              } else {
                PADDING_BOTTOM_WITHOUT_FAB.dp
              },
          ),
      ) {
        item(key = profileUiState.resourceData?.baseResourceId) {
          ViewRenderer(
            viewProperties = profileUiState.profileConfiguration?.views ?: emptyList(),
            resourceData =
              profileUiState.resourceData ?: ResourceData("", ResourceType.Patient, emptyMap()),
            navController = navController,
            decodedImageMap = profileUiState.decodedImageMap,
          )
        }
      }
    }
  }
}

@Composable
fun CustomProfileTopAppBar(
  modifier: Modifier = Modifier,
  navController: NavController,
  profileUiState: ProfileUiState,
  onEvent: (ProfileEvent) -> Unit,
  lazyListState: LazyListState,
) {
  val topBarConfig = remember { profileUiState.profileConfiguration?.topAppBar ?: TopBarConfig() }

  Column(modifier = modifier.fillMaxWidth().background(MaterialTheme.colors.primary)) {
    SimpleTopAppBar(
      modifier = modifier,
      navController = navController,
      elevation = 0,
      titleTextProperties =
        topBarConfig.title?.interpolate(
          profileUiState.resourceData?.computedValuesMap ?: emptyMap(),
        ),
      profileUiState = profileUiState,
      collapsible = topBarConfig.collapsible,
      onEvent = onEvent,
      lazyListState = lazyListState,
    )
    if (topBarConfig.collapsible) {
      AnimatedVisibility(visible = lazyListState.isScrollingDown()) {
        RenderSimpleAppTopBar(
          modifier = modifier,
          topBarConfig = topBarConfig,
          profileUiState = profileUiState,
          navController = navController,
          titleContentPadding = 16,
        )
      }
    } else {
      RenderSimpleAppTopBar(
        modifier = modifier,
        topBarConfig = topBarConfig,
        profileUiState = profileUiState,
        navController = navController,
        titleContentPadding = 0,
      )
    }
  }
}

@Composable
private fun RenderSimpleAppTopBar(
  modifier: Modifier,
  topBarConfig: TopBarConfig,
  profileUiState: ProfileUiState,
  navController: NavController,
  titleContentPadding: Int,
) {
  Column(
    modifier =
      modifier.padding(
        start = titleContentPadding.dp,
        end = titleContentPadding.dp,
        bottom = titleContentPadding.dp,
      ),
  ) {
    ViewRenderer(
      viewProperties = topBarConfig.content,
      resourceData =
        profileUiState.resourceData ?: ResourceData("", ResourceType.Patient, emptyMap()),
      navController = navController,
      decodedImageMap = profileUiState.decodedImageMap,
    )
  }
}

@Composable
private fun SimpleTopAppBar(
  modifier: Modifier,
  navController: NavController,
  elevation: Int = 0,
  titleTextProperties: CompoundTextProperties? = null,
  profileUiState: ProfileUiState,
  lazyListState: LazyListState,
  collapsible: Boolean,
  onEvent: (ProfileEvent) -> Unit,
) {
  TopAppBar(
    modifier = modifier.testTag(PROFILE_TOP_BAR_TEST_TAG),
    title = {
      if (titleTextProperties != null && profileUiState.resourceData != null) {
        if (collapsible) {
          AnimatedVisibility(visible = !lazyListState.isScrollingDown()) {
            CompoundText(
              compoundTextProperties = titleTextProperties,
              resourceData = profileUiState.resourceData,
              navController = navController,
            )
          }
        } else {
          CompoundText(
            compoundTextProperties = titleTextProperties,
            resourceData = profileUiState.resourceData,
            navController = navController,
          )
        }
      }
    },
    navigationIcon = {
      IconButton(onClick = { navController.popBackStack() }) {
        Icon(
          Icons.Filled.ArrowBack,
          null,
          modifier = modifier.testTag(PROFILE_TOP_BAR_ICON_TEST_TAG),
        )
      }
    },
    actions = {
      ProfileTopAppBarMenuAction(
        profileUiState = profileUiState,
        onEvent = onEvent,
        navController = navController,
      )
    },
    elevation = elevation.dp,
  )
}

@Composable
private fun ProfileTopAppBarMenuAction(
  profileUiState: ProfileUiState,
  onEvent: (ProfileEvent) -> Unit,
  navController: NavController,
  modifier: Modifier = Modifier,
) {
  if (!profileUiState.profileConfiguration?.overFlowMenuItems.isNullOrEmpty()) {
    var showOverflowMenu by remember { mutableStateOf(false) }
    IconButton(
      onClick = { showOverflowMenu = !showOverflowMenu },
      modifier = modifier.testTag(DROPDOWN_MENU_TEST_TAG),
    ) {
      Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = null, tint = Color.White)
    }

    DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
      profileUiState.profileConfiguration?.overFlowMenuItems?.forEach {
        val overflowMenuItemConfig =
          it.interpolate(profileUiState.resourceData?.computedValuesMap ?: emptyMap())
        if (!overflowMenuItemConfig.visible.toBoolean()) return@forEach
        val enabled = overflowMenuItemConfig.enabled.toBoolean()
        if (overflowMenuItemConfig.showSeparator) Divider(color = DividerColor, thickness = 1.dp)
        val contentColor =
          if (enabled) overflowMenuItemConfig.titleColor.parseColor() else DefaultColor

        DropdownMenuItem(
          enabled = enabled,
          onClick = {
            showOverflowMenu = false
            onEvent(
              ProfileEvent.OverflowMenuClick(
                navController = navController,
                resourceData = profileUiState.resourceData,
                overflowMenuItemConfig = overflowMenuItemConfig,
              ),
            )
          },
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          modifier =
            modifier
              .fillMaxWidth()
              .background(
                color =
                  if (overflowMenuItemConfig.confirmAction) {
                    overflowMenuItemConfig.backgroundColor.parseColor().copy(alpha = 0.1f)
                  } else {
                    Color.Transparent
                  },
              ),
        ) {
          Row {
            Image(
              imageProperties = ImageProperties(imageConfig = overflowMenuItemConfig.icon),
              tint = contentColor,
              navController = navController,
              resourceData = profileUiState.resourceData!!,
            )
            if (overflowMenuItemConfig.icon != null) Spacer(modifier = Modifier.width(4.dp))
            Text(text = overflowMenuItemConfig.title, color = contentColor)
          }
        }
      }
    }
  }
}
