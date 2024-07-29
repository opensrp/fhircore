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

package org.smartregister.fhircore.quest.ui.register

import SubsequentSyncDetailsBar
import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.android.fhir.sync.CurrentSyncJobStatus
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.navigation.ImageConfig
import org.smartregister.fhircore.engine.configuration.register.NoResultsConfig
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ToolBarHomeNavigation
import org.smartregister.fhircore.engine.ui.components.register.LoaderDialog
import org.smartregister.fhircore.engine.ui.components.register.RegisterHeader
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.ui.theme.SideMenuTopItemDarkColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.quest.event.ToolbarClickEvent
import org.smartregister.fhircore.quest.ui.main.AppMainEvent
import org.smartregister.fhircore.quest.ui.main.AppMainUiState
import org.smartregister.fhircore.quest.ui.main.components.SyncCompleteStatus
import org.smartregister.fhircore.quest.ui.main.components.TopScreenSection
import org.smartregister.fhircore.quest.ui.register.components.RegisterCardList
import org.smartregister.fhircore.quest.ui.shared.components.ExtendedFab
import org.smartregister.fhircore.quest.ui.shared.models.AppDrawerUIState
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent

const val NO_REGISTER_VIEW_COLUMN_TEST_TAG = "noRegisterViewColumnTestTag"
const val NO_REGISTER_VIEW_TITLE_TEST_TAG = "noRegisterViewTitleTestTag"
const val NO_REGISTER_VIEW_MESSAGE_TEST_TAG = "noRegisterViewMessageTestTag"
const val NO_REGISTER_VIEW_BUTTON_TEST_TAG = "noRegisterViewButtonTestTag"
const val NO_REGISTER_VIEW_BUTTON_ICON_TEST_TAG = "noRegisterViewButtonIconTestTag"
const val NO_REGISTER_VIEW_BUTTON_TEXT_TEST_TAG = "noRegisterViewButtonTextTestTag"
const val REGISTER_CARD_TEST_TAG = "registerCardListTestTag"
const val FIRST_TIME_SYNC_DIALOG = "firstTimeSyncTestTag"
const val FAB_BUTTON_REGISTER_TEST_TAG = "fabTestTag"
const val TOP_REGISTER_SCREEN_TEST_TAG = "topScreenTestTag"
const val SYNC_SUCCESS_TAG = "syncSuccessTag"
const val SYNC_ERROR_TAG = "syncErrorTag"
const val SYNC_PROGRESS_BAR_TAG = "syncProgressBarTag"

@Composable
fun RegisterScreen(
  modifier: Modifier = Modifier,
  openDrawer: (Boolean) -> Unit,
  onEvent: (RegisterEvent) -> Unit,
  registerUiState: RegisterUiState,
  appDrawerUIState: AppDrawerUIState = AppDrawerUIState(),
  appUiState: AppMainUiState? = null,
  onCancel: (AppMainEvent) -> Unit,
  searchText: MutableState<String>,
  currentPage: MutableState<Int>,
  pagingItems: LazyPagingItems<ResourceData>,
  navController: NavController,
  toolBarHomeNavigation: ToolBarHomeNavigation = ToolBarHomeNavigation.OPEN_DRAWER,
) {
  val currentSyncJobStatus: CurrentSyncJobStatus? = appDrawerUIState.currentSyncJobStatus
  val lazyListState: LazyListState = rememberLazyListState()
  var syncNotificationBarExpanded by remember { mutableStateOf(true) }
  val coroutineScope = rememberCoroutineScope()

  var synBarBackgroundColor by remember { mutableStateOf(Color(0xFF002B4A)) }
  var showSyncBar by remember { mutableStateOf(false) }
  var showSyncComplete by remember { mutableStateOf(false) }
  var hasShownSyncComplete by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(
    currentSyncJobStatus,
    hasShownSyncComplete,
  ) {
    updateSyncStatus(
      currentSyncJobStatus = currentSyncJobStatus,
      coroutineScope = coroutineScope,
      hasShownSyncComplete = hasShownSyncComplete,
      setHasShownSyncComplete = { hasShownSyncComplete = it },
      setShowSyncBar = { showSyncBar = it },
      setShowSyncComplete = { showSyncComplete = it },
      setBackgroundColor = { synBarBackgroundColor = it },
    )
  }

  Scaffold(
    topBar = {
      Column {
        val filterActions = registerUiState.registerConfiguration?.registerFilter?.dataFilterActions
        TopScreenSection(
          modifier = modifier.testTag(TOP_REGISTER_SCREEN_TEST_TAG),
          title =
            registerUiState.screenTitle.ifEmpty {
              registerUiState.registerConfiguration?.topScreenSection?.title ?: ""
            },
          searchText = searchText.value,
          filteredRecordsCount = registerUiState.filteredRecordsCount,
          isSearchBarVisible = registerUiState.registerConfiguration?.searchBar?.visible ?: true,
          searchPlaceholder = registerUiState.registerConfiguration?.searchBar?.display,
          toolBarHomeNavigation = toolBarHomeNavigation,
          onSearchTextChanged = { text ->
            searchText.value = text
            onEvent(RegisterEvent.SearchRegister(searchText = text))
          },
          isFilterIconEnabled = filterActions?.isNotEmpty() ?: false,
          topScreenSection = registerUiState.registerConfiguration?.topScreenSection,
          navController = navController,
        ) { event ->
          when (event) {
            ToolbarClickEvent.Navigate ->
              when (toolBarHomeNavigation) {
                ToolBarHomeNavigation.OPEN_DRAWER -> openDrawer(true)
                ToolBarHomeNavigation.NAVIGATE_BACK -> navController.popBackStack()
              }
            ToolbarClickEvent.FilterData -> {
              onEvent(RegisterEvent.ResetFilterRecordsCount)
              filterActions?.handleClickEvent(navController)
            }
            is ToolbarClickEvent.Actions -> {
              event.actions.handleClickEvent(navController)
            }
          }
        }
        if (searchText.value.isNotEmpty()) RegisterHeader(resultCount = pagingItems.itemCount)
      }
    },
    floatingActionButton = {
      val fabActions = registerUiState.registerConfiguration?.fabActions
      if (!fabActions.isNullOrEmpty() && fabActions.first().visible) {
        ExtendedFab(
          modifier = modifier.testTag(FAB_BUTTON_REGISTER_TEST_TAG),
          fabActions = fabActions,
          navController = navController,
          lazyListState = lazyListState,
        )
      }
    },
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      if (registerUiState.isFirstTimeSync) {
        LoaderDialog(
          modifier = modifier.testTag(FIRST_TIME_SYNC_DIALOG),
          percentageProgressFlow = flowOf(appDrawerUIState.percentageProgress),
          dialogMessage =
            stringResource(
              id =
                if (appDrawerUIState.isSyncUpload) R.string.syncing_up else R.string.syncing_down,
            ),
          showPercentageProgress = true,
        )
      }

      Column(
        modifier =
          Modifier.background(synBarBackgroundColor)
            .background(
              if (showSyncComplete) {
                Color.White.copy(alpha = 0.83f)
              } else {
                Color.Transparent
              },
            ),
      ) {
        Box(
          modifier =
            Modifier.weight(1f)
              .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
              .background(Color.White),
        ) {
          if (
            registerUiState.totalRecordsCount > 0 &&
              registerUiState.registerConfiguration?.registerCard != null
          ) {
            RegisterCardList(
              modifier = modifier.testTag(REGISTER_CARD_TEST_TAG),
              registerCardConfig = registerUiState.registerConfiguration.registerCard,
              pagingItems = pagingItems,
              navController = navController,
              lazyListState = lazyListState,
              onEvent = onEvent,
              registerUiState = registerUiState,
              currentPage = currentPage,
              showPagination = searchText.value.isEmpty(),
            )
          } else {
            registerUiState.registerConfiguration?.noResults?.let { noResultConfig ->
              NoRegisterDataView(modifier = modifier, noResults = noResultConfig) {
                noResultConfig.actionButton?.actions?.handleClickEvent(navController)
              }
            }
          }
          if (
            showSyncBar &&
              appUiState!!.currentSyncJobStatus !is CurrentSyncJobStatus.Cancelled &&
              !registerUiState.isFirstTimeSync
          ) {
            Box(
              modifier =
                Modifier.align(Alignment.BottomStart)
                  .padding(start = 16.dp)
                  .height(20.dp)
                  .width(60.dp)
                  .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                  .background(synBarBackgroundColor)
                  .background(
                    if (showSyncComplete) {
                      Color.White.copy(alpha = 0.83f)
                    } else {
                      Color.Transparent
                    },
                  )
                  .clickable { syncNotificationBarExpanded = !syncNotificationBarExpanded },
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector =
                  if (syncNotificationBarExpanded) {
                    Icons.Default.KeyboardArrowDown
                  } else Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint =
                  if (synBarBackgroundColor == Color(0xFF002B4A)) {
                    Color.White
                  } else {
                    synBarBackgroundColor
                  },
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }
        if (showSyncBar && !registerUiState.isFirstTimeSync) {
          val heightDp = if (syncNotificationBarExpanded) 48.dp else 24.dp
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(
                  if (currentSyncJobStatus !is CurrentSyncJobStatus.Running) {
                    heightDp
                  } else Dp.Unspecified,
                )
                .animateContentSize()
                .background(synBarBackgroundColor)
                .background(
                  if (showSyncComplete) {
                    Color.White.copy(alpha = 0.83f)
                  } else {
                    Color.Transparent
                  },
                ),
          ) {
            val context = LocalContext.current
            SyncStatusView(
              currentSyncJobStatus = appDrawerUIState.currentSyncJobStatus,
              appUiState = appUiState,
              showSyncComplete = showSyncComplete,
              syncNotificationBarExpanded = syncNotificationBarExpanded,
              onClick = onCancel,
              percentageProgressFlow = flowOf(appDrawerUIState.percentageProgress),
              context = context,
            ) {}
          }
        }
      }
    }
  }
}

private fun updateSyncStatus(
  currentSyncJobStatus: CurrentSyncJobStatus?,
  coroutineScope: CoroutineScope,
  hasShownSyncComplete: Boolean,
  setHasShownSyncComplete: (Boolean) -> Unit,
  setShowSyncBar: (Boolean) -> Unit,
  setShowSyncComplete: (Boolean) -> Unit,
  setBackgroundColor: (Color) -> Unit,
) {
  when (currentSyncJobStatus) {
    is CurrentSyncJobStatus.Succeeded -> {
      if (!hasShownSyncComplete) {
        setBackgroundColor(Color(0xFF1DB11B))
        setShowSyncBar(true)
        setShowSyncComplete(true)
        setHasShownSyncComplete(true)

        coroutineScope.launch {
          delay(10.seconds)
          setShowSyncBar(false)
          setShowSyncComplete(false)
          setBackgroundColor(SideMenuTopItemDarkColor)
        }
      }
    }
    is CurrentSyncJobStatus.Failed -> {
      setBackgroundColor(Color(0xFFDF0E1A))
      setShowSyncBar(true)
      setShowSyncComplete(true)
      setHasShownSyncComplete(false)
      coroutineScope.launch {
        delay(10.seconds)
        setShowSyncBar(false)
        setShowSyncComplete(false)
        setBackgroundColor(SideMenuTopItemDarkColor)
      }
    }
    is CurrentSyncJobStatus.Running -> {
      setShowSyncBar(true)
      setBackgroundColor(Color(0xFF002B4A))
      setShowSyncComplete(false)
      setHasShownSyncComplete(false)
    }
    else -> {
      setShowSyncBar(false)
    }
  }
}

@Composable
fun SyncStatusView(
  currentSyncJobStatus: CurrentSyncJobStatus?,
  appUiState: AppMainUiState?,
  showSyncComplete: Boolean,
  syncNotificationBarExpanded: Boolean,
  onClick: (AppMainEvent) -> Unit,
  modifier: Modifier = Modifier,
  percentageProgressFlow: Flow<Int>,
  context: Context,
  openDrawer: (Boolean) -> Unit,
) {
  when {
    currentSyncJobStatus is CurrentSyncJobStatus.Running &&
      appUiState?.currentSyncJobStatus !is CurrentSyncJobStatus.Cancelled -> {
      SubsequentSyncDetailsBar(
        percentageProgressFlow = percentageProgressFlow,
        modifier = Modifier.testTag(SYNC_PROGRESS_BAR_TAG),
        hideExtraInformation = syncNotificationBarExpanded,
      ) {
        onClick(AppMainEvent.CancelSyncData(context))
      }
    }
    currentSyncJobStatus is CurrentSyncJobStatus.Failed -> {
      SyncCompleteStatus(
        modifier = modifier.testTag(SYNC_ERROR_TAG),
        imageConfig = ImageConfig(type = "local", "ic_sync_fail"),
        title = context.getString(R.string.sync_error),
        showEndText = false,
        showImage = syncNotificationBarExpanded,
        syncSuccess = false,
      ) {
        openDrawer(false)
        onClick(AppMainEvent.SyncData(context))
      }
    }
    currentSyncJobStatus is CurrentSyncJobStatus.Succeeded && showSyncComplete -> {
      SyncCompleteStatus(
        modifier = modifier.testTag(SYNC_SUCCESS_TAG),
        imageConfig = ImageConfig(type = "local", "ic_sync_success"),
        title = context.getString(R.string.sync_completed),
        showImage = syncNotificationBarExpanded,
        showEndText = false,
        onCancelButtonClick = {},
      )
    }
    else -> {
      // Handle any other cases if needed
    }
  }
}

@Composable
fun NoRegisterDataView(
  modifier: Modifier = Modifier,
  noResults: NoResultsConfig,
  onClick: () -> Unit,
) {
  Column(
    modifier = modifier.fillMaxSize().padding(16.dp).testTag(NO_REGISTER_VIEW_COLUMN_TEST_TAG),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = noResults.title,
      fontSize = 16.sp,
      modifier = modifier.padding(vertical = 8.dp).testTag(NO_REGISTER_VIEW_TITLE_TEST_TAG),
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = noResults.message,
      modifier =
        modifier.padding(start = 32.dp, end = 32.dp).testTag(NO_REGISTER_VIEW_MESSAGE_TEST_TAG),
      textAlign = TextAlign.Center,
      fontSize = 15.sp,
      color = Color.Gray,
    )
    if (noResults.actionButton != null) {
      Button(
        modifier = modifier.padding(vertical = 16.dp).testTag(NO_REGISTER_VIEW_BUTTON_TEST_TAG),
        onClick = onClick,
      ) {
        Icon(
          imageVector = Icons.Filled.Add,
          contentDescription = null,
          modifier.padding(end = 8.dp).testTag(NO_REGISTER_VIEW_BUTTON_ICON_TEST_TAG),
        )
        Text(
          text = noResults.actionButton?.display?.uppercase().toString(),
          modifier.testTag(NO_REGISTER_VIEW_BUTTON_TEXT_TEST_TAG),
        )
      }
    }
  }
}

@Composable
@PreviewWithBackgroundExcludeGenerated
fun RegisterScreenWithDataPreview() {
  val registerUiState =
    RegisterUiState(
      screenTitle = "Sample Register",
      isFirstTimeSync = false,
      registerConfiguration =
        RegisterConfiguration(
          "app",
          configType = ConfigType.Register.name,
          id = "register",
          fhirResource =
            FhirResourceConfig(baseResource = ResourceConfig(resource = ResourceType.Patient)),
        ),
      registerId = "register101",
      totalRecordsCount = 1,
      filteredRecordsCount = 0,
      pagesCount = 1,
      progressPercentage = flowOf(0),
      isSyncUpload = flowOf(false),
      params = emptyMap(),
    )
  val searchText = remember { mutableStateOf("") }
  val currentPage = remember { mutableIntStateOf(0) }
  val data = listOf(ResourceData("1", ResourceType.Patient, emptyMap()))
  val pagingItems = flowOf(PagingData.from(data)).collectAsLazyPagingItems()

  AppTheme {
    RegisterScreen(
      modifier = Modifier,
      openDrawer = {},
      onEvent = {},
      registerUiState = registerUiState,
      searchText = searchText,
      currentPage = currentPage,
      pagingItems = pagingItems,
      navController = rememberNavController(),
      onCancel = {},
    )
  }
}
