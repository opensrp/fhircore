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

package org.smartregister.fhircore.quest.integration.ui.register

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.navigation.compose.rememberNavController
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.sync.CurrentSyncJobStatus
import com.google.android.fhir.sync.SyncJobStatus
import com.google.android.fhir.sync.SyncOperation
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.flowOf
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.navigation.NavigationBottomSheetRegisterConfig
import org.smartregister.fhircore.engine.configuration.navigation.NavigationConfiguration
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.configuration.register.NoResultsConfig
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.integration.Faker
import org.smartregister.fhircore.quest.ui.main.appMainUiStateOf
import org.smartregister.fhircore.quest.ui.register.FAB_BUTTON_REGISTER_TEST_TAG
import org.smartregister.fhircore.quest.ui.register.FIRST_TIME_SYNC_DIALOG
import org.smartregister.fhircore.quest.ui.register.NO_REGISTER_VIEW_COLUMN_TEST_TAG
import org.smartregister.fhircore.quest.ui.register.NoRegisterDataView
import org.smartregister.fhircore.quest.ui.register.REGISTER_CARD_TEST_TAG
import org.smartregister.fhircore.quest.ui.register.RegisterScreen
import org.smartregister.fhircore.quest.ui.register.RegisterUiState
import org.smartregister.fhircore.quest.ui.register.TOP_REGISTER_SCREEN_TEST_TAG
import org.smartregister.fhircore.quest.ui.shared.components.SYNC_PROGRESS_INDICATOR_TEST_TAG
import org.smartregister.fhircore.quest.ui.shared.models.AppDrawerUIState
import org.smartregister.fhircore.quest.ui.shared.models.SearchQuery

@HiltAndroidTest
class RegisterScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val noResults = NoResultsConfig()

  private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

  private val navigationConfiguration =
    NavigationConfiguration(
      appId = "appId",
      configType = ConfigType.Navigation.name,
      staticMenu = listOf(),
      clientRegisters =
        listOf(
          NavigationMenuConfig(id = "id3", visible = true, display = "Register 1"),
          NavigationMenuConfig(id = "id4", visible = false, display = "Register 2"),
        ),
      bottomSheetRegisters =
        NavigationBottomSheetRegisterConfig(
          visible = true,
          display = "My Register",
          registers =
            listOf(NavigationMenuConfig(id = "id2", visible = true, display = "Title My Register")),
        ),
      menuActionButton =
        NavigationMenuConfig(id = "id1", visible = true, display = "Register Household"),
    )

  private val appUiState =
    appMainUiStateOf(
      appTitle = "MOH VTS",
      username = "Demo",
      lastSyncTime = "05:30 PM, Mar 3",
      currentLanguage = "English",
      languages = listOf(Language("en", "English"), Language("sw", "Swahili")),
      navigationConfiguration =
        navigationConfiguration.copy(
          bottomSheetRegisters =
            navigationConfiguration.bottomSheetRegisters?.copy(display = "Random name"),
        ),
    )

  @Test
  fun testFloatingActionButtonIsDisplayed() {
    val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
    val registerUiState =
      RegisterUiState(
        screenTitle = "Register101",
        isFirstTimeSync = false,
        registerConfiguration =
          configurationRegistry.retrieveConfiguration(ConfigType.Register, "householdRegister"),
        registerId = "register101",
        totalRecordsCount = 1,
        filteredRecordsCount = 0,
        pagesCount = 0,
        progressPercentage = flowOf(0),
        isSyncUpload = flowOf(false),
        params = emptyList(),
      )
    val searchText = mutableStateOf(SearchQuery.emptyText)
    val currentPage = mutableStateOf(0)

    composeTestRule.setContent {
      val data = listOf(ResourceData("1", ResourceType.Patient, emptyMap()))

      val pagingItems = flowOf(PagingData.from(data)).collectAsLazyPagingItems()

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
    composeTestRule.waitUntil(5_000) { true }
    composeTestRule.onAllNodesWithTag(FAB_BUTTON_REGISTER_TEST_TAG, useUnmergedTree = true)
  }

  @Test
  fun testRegisterCardListIsRendered() {
    val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
    val registerUiState =
      RegisterUiState(
        screenTitle = "Register101",
        isFirstTimeSync = false,
        registerConfiguration =
          configurationRegistry.retrieveConfiguration(ConfigType.Register, "householdRegister"),
        registerId = "register101",
        totalRecordsCount = 1,
        filteredRecordsCount = 0,
        pagesCount = 1,
        progressPercentage = flowOf(0),
        isSyncUpload = flowOf(false),
        params = emptyList(),
      )
    val searchText = mutableStateOf(SearchQuery.emptyText)
    val currentPage = mutableStateOf(0)

    composeTestRule.setContent {
      val data = listOf(ResourceData("1", ResourceType.Patient, emptyMap()))

      val pagingItems = flowOf(PagingData.from(data)).collectAsLazyPagingItems()

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

    composeTestRule
      .onNodeWithTag(REGISTER_CARD_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testRegisterCardListIsScrollable() {
    val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
    val registerUiState =
      RegisterUiState(
        screenTitle = "Register101",
        isFirstTimeSync = false,
        registerConfiguration =
          configurationRegistry.retrieveConfiguration(ConfigType.Register, "householdRegister"),
        registerId = "register101",
        totalRecordsCount = 1,
        filteredRecordsCount = 0,
        pagesCount = 1,
        progressPercentage = flowOf(0),
        isSyncUpload = flowOf(false),
        params = emptyList(),
      )
    val searchText = mutableStateOf(SearchQuery.emptyText)
    val currentPage = mutableStateOf(0)

    composeTestRule.setContent {
      val data = listOf(ResourceData("1", ResourceType.Patient, emptyMap()))

      val pagingItems = flowOf(PagingData.from(data)).collectAsLazyPagingItems()

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

    composeTestRule
      .onNodeWithTag(REGISTER_CARD_TEST_TAG, useUnmergedTree = true)
      .performTouchInput { swipeUp() }
      .performTouchInput { swipeDown() }
  }

  @Test
  fun testThatDialogIsDisplayedDuringSyncing() {
    val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
    val registerUiState =
      RegisterUiState(
        screenTitle = "Register101",
        isFirstTimeSync = true,
        registerConfiguration =
          configurationRegistry.retrieveConfiguration(ConfigType.Register, "childRegister"),
        registerId = "register101",
        totalRecordsCount = 0,
        filteredRecordsCount = 0,
        pagesCount = 1,
        progressPercentage = flowOf(0),
        isSyncUpload = flowOf(false),
        params = emptyList(),
      )
    val searchText = mutableStateOf(SearchQuery.emptyText)
    val currentPage = mutableStateOf(0)
    val pagingItems = mockk<LazyPagingItems<ResourceData>>().apply {}
    val combinedLoadState: CombinedLoadStates = mockk()
    val loadState: LoadState = mockk()

    every { pagingItems.itemCount } returns 0
    every { combinedLoadState.refresh } returns loadState
    every { combinedLoadState.append } returns loadState
    every { loadState.endOfPaginationReached } returns true
    every { pagingItems.loadState } returns combinedLoadState

    composeTestRule.setContent {
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
    composeTestRule.onNodeWithTag(FIRST_TIME_SYNC_DIALOG, useUnmergedTree = true)
  }

  @Test
  fun testThatTopScreenIsRendered() {
    val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
    val registerUiState =
      RegisterUiState(
        screenTitle = "Register101",
        isFirstTimeSync = false,
        registerConfiguration =
          configurationRegistry.retrieveConfiguration(ConfigType.Register, "householdRegister"),
        registerId = "register101",
        totalRecordsCount = 1,
        filteredRecordsCount = 0,
        pagesCount = 0,
        progressPercentage = flowOf(0),
        isSyncUpload = flowOf(false),
        params = emptyList(),
      )
    val searchText = mutableStateOf(SearchQuery.emptyText)
    val currentPage = mutableStateOf(0)

    composeTestRule.setContent {
      val data = listOf(ResourceData("1", ResourceType.Patient, emptyMap()))

      val pagingItems = flowOf(PagingData.from(data)).collectAsLazyPagingItems()

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
    composeTestRule.waitUntil(5_000) { true }
    composeTestRule.onNodeWithTag(TOP_REGISTER_SCREEN_TEST_TAG, useUnmergedTree = true)
  }

  @Test
  fun testThatTopScreenRenderShowsQrCode() {
    val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
    val registerUiState =
      RegisterUiState(
        screenTitle = "Register101",
        isFirstTimeSync = false,
        registerConfiguration =
          configurationRegistry
            .retrieveConfiguration<RegisterConfiguration>(ConfigType.Register, "householdRegister")
            .copy(
              onSearchByQrSingleResultActions =
                listOf(ActionConfig(trigger = ActionTrigger.ON_SEARCH_SINGLE_RESULT)),
            ),
        registerId = "register101",
        totalRecordsCount = 1,
        filteredRecordsCount = 0,
        pagesCount = 0,
        progressPercentage = flowOf(0),
        isSyncUpload = flowOf(false),
        params = emptyList(),
      )
    val searchText = mutableStateOf(SearchQuery.emptyText)
    val currentPage = mutableStateOf(0)

    composeTestRule.setContent {
      val data = listOf(ResourceData("1", ResourceType.Patient, emptyMap()))

      val pagingItems = flowOf(PagingData.from(data)).collectAsLazyPagingItems()

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

    Assert.assertEquals(true, registerUiState.registerConfiguration?.showSearchByQrCode)
  }

  @Test
  fun testNoRegisterDataViewDisplaysNoTestTag() {
    composeTestRule.setContent {
      NoRegisterDataView(modifier = Modifier, noResults = noResults, onClick = {})
    }
    composeTestRule
      .onNodeWithTag(NO_REGISTER_VIEW_COLUMN_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testCountAllNodeNoRegisterDataViewDisplaysNoTestTag() {
    composeTestRule.setContent {
      NoRegisterDataView(modifier = Modifier, noResults = noResults, onClick = {})
    }
    composeTestRule
      .onAllNodesWithTag(NO_REGISTER_VIEW_COLUMN_TEST_TAG, useUnmergedTree = true)
      .assertCountEquals(1)
  }

  @Test
  fun checkNodeWithNoRegisterViewColumTestTag() {
    composeTestRule.setContent {
      NoRegisterDataView(modifier = Modifier, noResults = noResults, onClick = {})
    }
    composeTestRule
      .onNodeWithTag(NO_REGISTER_VIEW_COLUMN_TEST_TAG, useUnmergedTree = true)
      .onChildAt(0)
      .assertExists()
    composeTestRule
      .onNodeWithTag(NO_REGISTER_VIEW_COLUMN_TEST_TAG, useUnmergedTree = true)
      .onChildAt(1)
      .assertExists()
  }

  @Test
  fun testSyncInProgress() {
    val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
    val registerUiState =
      RegisterUiState(
        screenTitle = "Register101",
        isFirstTimeSync = false,
        registerConfiguration =
          configurationRegistry.retrieveConfiguration(ConfigType.Register, "householdRegister"),
        registerId = "register101",
        totalRecordsCount = 1,
        filteredRecordsCount = 0,
        pagesCount = 0,
        progressPercentage = flowOf(50),
        isSyncUpload = flowOf(true),
        currentSyncJobStatus =
          flowOf(
            CurrentSyncJobStatus.Running(
              SyncJobStatus.InProgress(
                syncOperation = SyncOperation.UPLOAD,
              ),
            ),
          ),
        params = emptyList(),
      )
    val searchText = mutableStateOf(SearchQuery.emptyText)
    val currentPage = mutableStateOf(0)

    composeTestRule.setContent {
      val data = listOf(ResourceData("1", ResourceType.Patient, emptyMap()))
      val pagingItems = flowOf(PagingData.from(data)).collectAsLazyPagingItems()
      RegisterScreen(
        modifier = Modifier,
        openDrawer = {},
        onEvent = {},
        registerUiState = registerUiState,
        appDrawerUIState =
          AppDrawerUIState(
            currentSyncJobStatus =
              CurrentSyncJobStatus.Running(
                SyncJobStatus.InProgress(
                  syncOperation = SyncOperation.UPLOAD,
                ),
              ),
          ),
        onAppMainEvent = {},
        searchQuery = searchText,
        currentPage = currentPage,
        pagingItems = pagingItems,
        navController = rememberNavController(),
        decodeImage = null,
      )
    }

    composeTestRule
      .onNodeWithTag(SYNC_PROGRESS_INDICATOR_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testSyncStatusShowsWhenSucceeded() {
    val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
    val registerUiState =
      RegisterUiState(
        screenTitle = "Register101",
        isFirstTimeSync = false,
        registerConfiguration =
          configurationRegistry.retrieveConfiguration(ConfigType.Register, "householdRegister"),
        registerId = "register101",
        totalRecordsCount = 1,
        filteredRecordsCount = 0,
        pagesCount = 0,
        progressPercentage = flowOf(100),
        isSyncUpload = flowOf(false),
        currentSyncJobStatus = flowOf(CurrentSyncJobStatus.Succeeded(OffsetDateTime.now())),
        params = emptyList(),
      )
    val searchText = mutableStateOf(SearchQuery.emptyText)
    val currentPage = mutableStateOf(0)

    composeTestRule.setContent {
      val data = listOf(ResourceData("1", ResourceType.Patient, emptyMap()))
      val pagingItems = flowOf(PagingData.from(data)).collectAsLazyPagingItems()

      RegisterScreen(
        modifier = Modifier,
        openDrawer = {},
        onEvent = {},
        registerUiState = registerUiState,
        appDrawerUIState =
          AppDrawerUIState(
            currentSyncJobStatus = CurrentSyncJobStatus.Succeeded(OffsetDateTime.now()),
          ),
        onAppMainEvent = {},
        searchQuery = searchText,
        currentPage = currentPage,
        pagingItems = pagingItems,
        navController = rememberNavController(),
        decodeImage = null,
      )
    }

    composeTestRule
      .onNodeWithText(
        applicationContext.getString(org.smartregister.fhircore.engine.R.string.sync_complete),
        useUnmergedTree = true,
      )
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testSyncStatusShowsWhenFailed() {
    val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
    val registerUiState =
      RegisterUiState(
        screenTitle = "Register101",
        isFirstTimeSync = false,
        registerConfiguration =
          configurationRegistry.retrieveConfiguration(ConfigType.Register, "householdRegister"),
        registerId = "register101",
        totalRecordsCount = 1,
        filteredRecordsCount = 0,
        pagesCount = 0,
        progressPercentage = flowOf(100),
        isSyncUpload = flowOf(false),
        currentSyncJobStatus = flowOf(CurrentSyncJobStatus.Succeeded(OffsetDateTime.now())),
        params = emptyList(),
      )
    val searchText = mutableStateOf(SearchQuery.emptyText)
    val currentPage = mutableStateOf(0)

    composeTestRule.setContent {
      val data = listOf(ResourceData("1", ResourceType.Patient, emptyMap()))
      val pagingItems = flowOf(PagingData.from(data)).collectAsLazyPagingItems()

      RegisterScreen(
        modifier = Modifier,
        openDrawer = {},
        onEvent = {},
        registerUiState = registerUiState,
        appDrawerUIState =
          AppDrawerUIState(
            currentSyncJobStatus = CurrentSyncJobStatus.Failed(OffsetDateTime.now()),
          ),
        onAppMainEvent = {},
        searchQuery = searchText,
        currentPage = currentPage,
        pagingItems = pagingItems,
        navController = rememberNavController(),
        decodeImage = null,
      )
    }

    composeTestRule
      .onNodeWithText(
        applicationContext.getString(org.smartregister.fhircore.engine.R.string.sync_error),
        useUnmergedTree = true,
      )
      .assertExists()
      .assertIsDisplayed()
  }
}
