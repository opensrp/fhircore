/*
 * Copyright 2021-2023 Ona Systems, Inc
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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import kotlinx.coroutines.flow.SharedFlow
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.ListProperties
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.domain.model.TopBarConfig
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.fhircore.quest.ui.shared.components.CompoundText
import org.smartregister.fhircore.quest.ui.shared.components.ExtendedFab
import org.smartregister.fhircore.quest.ui.shared.components.SnackBarMessage
import org.smartregister.fhircore.quest.ui.shared.components.ViewRenderer
import org.smartregister.fhircore.quest.util.extensions.hookSnackBar
import org.smartregister.fhircore.quest.util.extensions.isScrollingDown
import org.smartregister.fhircore.quest.util.extensions.isVisible
import timber.log.Timber

const val DROPDOWN_MENU_TEST_TAG = "dropDownMenuTestTag"
const val FAB_BUTTON_TEST_TAG = "fabButtonTestTag"
const val PROFILE_TOP_BAR_TEST_TAG = "profileTopBarTestTag"
const val PROFILE_TOP_BAR_ICON_TEST_TAG = "profileTopBarIconTestTag"

@Composable
fun ProfileScreen(
  modifier: Modifier = Modifier,
  navController: NavController,
  profileUiState: MutableState<ProfileUiState>,
  resourceData: MutableLiveData<ResourceData?>,
  snackStateFlow: SharedFlow<SnackBarMessageConfig>,
  onEvent: (ProfileEvent) -> Unit
) {
  val scaffoldState = rememberScaffoldState()
  val lazyListState = rememberLazyListState()
  var showOverflowMenu by remember { mutableStateOf(false) }
  val resourceData2 by resourceData.observeAsState()
  /*val resourceData by remember {
    mutableStateOf(profileUiState.value.value.resourceData!!.computedValuesMap)
  }*/

  LaunchedEffect(Unit) {
    snackStateFlow.hookSnackBar(scaffoldState, profileUiState.value.resourceData, navController)
  }

  Timber.e("Recomposing the profile screen")

  Scaffold(
    scaffoldState = scaffoldState,
    topBar = {
      if (profileUiState.value.profileConfiguration?.topAppBar == null) {
        SimpleTopAppBar(
          modifier = modifier,
          navController = navController,
          elevation = 4,
          profileUiState = profileUiState.value,
          lazyListState = lazyListState,
          onEvent = onEvent
        )
      } else {
        CustomProfileTopAppBar(
          navController = navController,
          profileUiState = profileUiState.value,
          onEvent = onEvent,
          lazyListState = lazyListState
        )
      }
    },
    floatingActionButton = {
      val fabActions = profileUiState.value.profileConfiguration?.fabActions

      if (!fabActions.isNullOrEmpty() && fabActions.first().visible) {
        ExtendedFab(
          modifier = Modifier.testTag(FAB_BUTTON_TEST_TAG),
          fabActions = fabActions,
          resourceData = profileUiState.value.resourceData,
          navController = navController,
          lazyListState = lazyListState
        )
      }
    },
    isFloatingActionButtonDocked = true,
    snackbarHost = { snackBarHostState ->
      SnackBarMessage(
        snackBarHostState = snackBarHostState,
        backgroundColorHex = profileUiState.value.snackBarTheme.backgroundColor,
        actionColorHex = profileUiState.value.snackBarTheme.actionTextColor,
        contentColorHex = profileUiState.value.snackBarTheme.messageTextColor
      )
    },
  ) { innerPadding ->
    Box(modifier = modifier.background(Color.White).fillMaxSize().padding(innerPadding)) {
      if (profileUiState.value.showDataLoadProgressIndicator) {
        CircularProgressIndicator(
          modifier = modifier.align(Alignment.Center).size(24.dp),
          strokeWidth = 1.8.dp,
          color = MaterialTheme.colors.primary
        )
      }

      val viewPropertiesList = profileUiState.value.profileConfiguration?.views
      if (viewPropertiesList != null && resourceData2 != null) {
        viewPropertiesList.forEach { viewProperties ->
          Timber.e("Re-rendering the view-properties-list")
          if (viewProperties.isVisible(resourceData2!!.computedValuesMap) &&
              viewProperties.viewType == ViewType.LIST
          ) {
            /*org.smartregister.fhircore.quest.ui.shared.components.List(
              modifier = modifier,
              viewProperties = properties as ListProperties,
              resourceData = resourceData,
              navController = navController,
            )

            GenerateView(
              modifier = generateModifier(viewProperties),
              properties = viewProperties,
              resourceData = resourceData2!!,
              navController = navController
            )*/
            viewProperties as ListProperties
            val currentListResourceData = resourceData2!!.listResourceDataMap[viewProperties.id]
            LazyColumn(state = lazyListState) {
              currentListResourceData!!.forEachIndexed { index, listResourceData ->
                Timber.e("Rendering item list item with ID ${listResourceData.baseResource!!.id}")
                item (key = listResourceData.baseResource!!.id) {
                  Spacer(modifier = modifier.height(6.dp))
                  Column(
                    modifier =
                    Modifier.padding(
                      horizontal = viewProperties.padding.dp,
                      vertical = viewProperties.padding.div(4).dp
                    )
                  ) {
                    ViewRenderer(
                      viewProperties = viewProperties.registerCard.views,
                      resourceData = listResourceData,
                      navController = navController,
                    )
                  }
                  Spacer(modifier = modifier.height(6.dp))
                  if (index < currentListResourceData.lastIndex && viewProperties.showDivider)
                    Divider(color = DividerColor, thickness = 0.5.dp)
                }
              }
            }
          }
        }
      } else {
        LazyColumn(state = lazyListState) {
          item(key = profileUiState.value.resourceData?.baseResourceId) {
            ViewRenderer(
              viewProperties = profileUiState.value.profileConfiguration?.views ?: emptyList(),
              resourceData = resourceData2
                  ?: ResourceData("", ResourceType.Patient, mutableMapOf(), mutableMapOf()),
              navController = navController
            )
          }
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
  lazyListState: LazyListState
) {
  val topBarConfig = remember { profileUiState.profileConfiguration?.topAppBar ?: TopBarConfig() }

  Column(modifier = modifier.fillMaxWidth().background(MaterialTheme.colors.primary)) {
    SimpleTopAppBar(
      modifier = modifier,
      navController = navController,
      elevation = 0,
      titleTextProperties = topBarConfig.title,
      profileUiState = profileUiState,
      onEvent = onEvent,
      lazyListState = lazyListState
    )
    AnimatedVisibility(visible = lazyListState.isScrollingDown()) {
      Column(modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        ViewRenderer(
          viewProperties = topBarConfig.content,
          resourceData = profileUiState.resourceData
              ?: ResourceData("", ResourceType.Patient, mutableMapOf(), mutableMapOf()),
          navController = navController
        )
      }
    }
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
  onEvent: (ProfileEvent) -> Unit
) {
  TopAppBar(
    modifier = modifier.testTag(PROFILE_TOP_BAR_TEST_TAG),
    title = {
      if (titleTextProperties != null && profileUiState.resourceData != null) {
        AnimatedVisibility(visible = !lazyListState.isScrollingDown()) {
          CompoundText(
            compoundTextProperties = titleTextProperties,
            resourceData = profileUiState.resourceData!!,
            navController = navController
          )
        }
      }
    },
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
      ProfileTopAppBarMenuAction(
        profileUiState = profileUiState,
        onEvent = onEvent,
        navController = navController
      )
    },
    elevation = elevation.dp
  )
}

@Composable
private fun ProfileTopAppBarMenuAction(
  profileUiState: ProfileUiState,
  onEvent: (ProfileEvent) -> Unit,
  navController: NavController,
  modifier: Modifier = Modifier
) {
  var showOverflowMenu by remember { mutableStateOf(false) }

  IconButton(
    onClick = { showOverflowMenu = !showOverflowMenu },
    modifier = modifier.testTag(DROPDOWN_MENU_TEST_TAG)
  ) { Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = null, tint = Color.White) }

  DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
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
      ) { Text(text = it.title, color = if (enabled) it.titleColor.parseColor() else DefaultColor) }
    }
  }
}
