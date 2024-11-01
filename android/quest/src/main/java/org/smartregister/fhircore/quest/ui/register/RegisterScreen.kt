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

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.flowOf
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.register.NoResultsConfig
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ToolBarHomeNavigation
import org.smartregister.fhircore.engine.ui.components.register.LoaderDialog
import org.smartregister.fhircore.engine.ui.components.register.RegisterHeader
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.quest.event.ToolbarClickEvent
import org.smartregister.fhircore.quest.ui.main.AppMainEvent
import org.smartregister.fhircore.quest.ui.main.components.TopScreenSection
import org.smartregister.fhircore.quest.ui.register.components.RegisterCardList
import org.smartregister.fhircore.quest.ui.shared.components.ExtendedFab
import org.smartregister.fhircore.quest.ui.shared.components.SyncBottomBar
import org.smartregister.fhircore.quest.ui.shared.models.AppDrawerUIState
import org.smartregister.fhircore.quest.ui.shared.models.SearchMode
import org.smartregister.fhircore.quest.ui.shared.models.SearchQuery
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

@Composable
fun RegisterScreen(
  modifier: Modifier = Modifier,
  openDrawer: (Boolean) -> Unit,
  onEvent: (RegisterEvent) -> Unit,
  registerUiState: RegisterUiState,
  appDrawerUIState: AppDrawerUIState = AppDrawerUIState(),
  onAppMainEvent: (AppMainEvent) -> Unit,
  searchQuery: MutableState<SearchQuery>,
  currentPage: MutableState<Int>,
  pagingItems: LazyPagingItems<ResourceData>,
  navController: NavController,
  toolBarHomeNavigation: ToolBarHomeNavigation = ToolBarHomeNavigation.OPEN_DRAWER,
  decodeImage: ((String) -> Bitmap?)?,
) {
  val lazyListState: LazyListState = rememberLazyListState()
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
          searchQuery = searchQuery.value,
          filteredRecordsCount = registerUiState.filteredRecordsCount,
          isSearchBarVisible = registerUiState.registerConfiguration?.searchBar?.visible ?: true,
          searchPlaceholder = registerUiState.registerConfiguration?.searchBar?.display,
          showSearchByQrCode = registerUiState.registerConfiguration?.showSearchByQrCode ?: false,
          toolBarHomeNavigation = toolBarHomeNavigation,
          onSearchTextChanged = { uiSearchQuery, performSearchOnValueChanged ->
            searchQuery.value = uiSearchQuery
            if (performSearchOnValueChanged) {
              onEvent(RegisterEvent.SearchRegister(searchQuery = uiSearchQuery))
            }
          },
          isFilterIconEnabled = filterActions?.isNotEmpty() ?: false,
          topScreenSection = registerUiState.registerConfiguration?.topScreenSection,
          navController = navController,
          decodeImage = decodeImage,
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
            is ToolbarClickEvent.Actions -> event.actions.handleClickEvent(navController)
          }
        }
        if (!searchQuery.value.isBlank()) RegisterHeader(resultCount = pagingItems.itemCount)
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
          decodeImage = decodeImage,
        )
      }
    },
    bottomBar = {
      SyncBottomBar(
        isFirstTimeSync = registerUiState.isFirstTimeSync,
        appDrawerUIState = appDrawerUIState,
        onAppMainEvent = onAppMainEvent,
        openDrawer = openDrawer,
      )
    },
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      if (registerUiState.isFirstTimeSync) {
        LoaderDialog(
          modifier = modifier.testTag(FIRST_TIME_SYNC_DIALOG),
          percentageProgressFlow = flowOf(appDrawerUIState.percentageProgress ?: 0),
          dialogMessage =
            stringResource(
              id =
                if (appDrawerUIState.isSyncUpload == true) {
                  R.string.syncing_up
                } else {
                  R.string.syncing_down
                },
            ),
          showPercentageProgress = true,
        )
      }

      Column(modifier = Modifier.fillMaxSize()) {
        Box(
          modifier = Modifier.fillMaxWidth().background(Color.White).weight(1f),
        ) {
          if (registerUiState.registerConfiguration?.registerCard != null) {
            RegisterCardList(
              modifier = modifier.fillMaxSize().testTag(REGISTER_CARD_TEST_TAG),
              registerCardConfig = registerUiState.registerConfiguration.registerCard,
              pagingItems = pagingItems,
              navController = navController,
              lazyListState = lazyListState,
              onEvent = onEvent,
              registerUiState = registerUiState,
              currentPage = currentPage,
              showPagination =
                !registerUiState.registerConfiguration.infiniteScroll &&
                  searchQuery.value.isBlank(),
              onSearchByQrSingleResultAction = { resourceData ->
                if (
                  !searchQuery.value.isBlank() && searchQuery.value.mode == SearchMode.QrCodeScan
                ) {
                  registerUiState.registerConfiguration.onSearchByQrSingleResultValidActions
                    ?.apply {
                      handleClickEvent(
                        navController,
                        resourceData,
                        context = navController.context,
                      )
                      searchQuery.value = searchQuery.value.copy(mode = SearchMode.KeyboardInput)
                    }
                }
              },
              decodeImage = decodeImage,
            )
          } else {
            registerUiState.registerConfiguration?.noResults?.let { noResultConfig ->
              NoRegisterDataView(modifier = modifier, noResults = noResultConfig) {
                noResultConfig.actionButton?.actions?.handleClickEvent(navController)
              }
            }
          }
        }
      }
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
      params = emptyList(),
    )
  val searchText = remember { mutableStateOf(SearchQuery.emptyText) }
  val currentPage = remember { mutableIntStateOf(0) }
  val data = listOf(ResourceData("1", ResourceType.Patient, emptyMap()))
  val pagingItems = flowOf(PagingData.from(data)).collectAsLazyPagingItems()

  AppTheme {
    RegisterScreen(
      modifier = Modifier,
      openDrawer = {},
      onEvent = {},
      registerUiState = registerUiState,
      onAppMainEvent = {},
      searchQuery = searchText,
      currentPage = currentPage,
      pagingItems = pagingItems,
      navController = rememberNavController(),
      decodeImage = null,
    )
  }
}
