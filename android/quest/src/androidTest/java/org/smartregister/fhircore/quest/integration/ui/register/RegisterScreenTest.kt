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

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.MutableLiveData
import androidx.navigation.compose.rememberNavController
import androidx.navigation.testing.TestNavHostController
import androidx.paging.compose.LazyPagingItems
import androidx.test.core.app.ActivityScenario
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.register.NoResultsConfig
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.integration.Faker
import org.smartregister.fhircore.quest.ui.login.PASSWORD_FORGOT_DIALOG
import org.smartregister.fhircore.quest.ui.register.DATA_MIGRATION_DIALOG
import org.smartregister.fhircore.quest.ui.register.FAB_REGISTER_BUTTON_TEST_TAG
import org.smartregister.fhircore.quest.ui.register.FIRST_TIME_SYNC_DIALOG
import org.smartregister.fhircore.quest.ui.register.NO_REGISTER_VIEW_COLUMN_TEST_TAG
import org.smartregister.fhircore.quest.ui.register.NoRegisterDataView
import org.smartregister.fhircore.quest.ui.register.REGISTER_TOP_BAR_TEST_TAG
import org.smartregister.fhircore.quest.ui.register.RegisterScreen
import org.smartregister.fhircore.quest.ui.register.RegisterUiState
import org.smartregister.fhircore.quest.ui.register.RegisterViewModel
import org.smartregister.fhircore.quest.ui.report.measure.screens.MeasureReportListScreen

@HiltAndroidTest
class RegisterScreenTest {
  @get:Rule val composeTestRule = createComposeRule()


  private val noResults = NoResultsConfig()

  @Test
  fun testTopBarRendersCorrectly() {
    val configurationRegistry : ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
    val registerUiState =
      RegisterUiState(
        screenTitle= "Register101",
        isFirstTimeSync = false,
        registerConfiguration = configurationRegistry.retrieveConfiguration(ConfigType.Register, "householdRegister"),
        registerId= "register101",
        totalRecordsCount = 0,
        filteredRecordsCount = 0,
        pagesCount = 1,
        progressPercentage = flowOf(0),
        isSyncUpload = flowOf(false),
        params = emptyMap())
    val searchText = mutableStateOf("")
    val currentPage = mutableStateOf(0)
    val pagingItems =mockk<LazyPagingItems<ResourceData>>().apply {  }
    val registerViewModel = mockk<RegisterViewModel>()
    val viewModelDataMigrationInProgress = MutableLiveData(false)
    every { registerViewModel.dataMigrationInProgress } returns viewModelDataMigrationInProgress

    composeTestRule.setContent {
      RegisterScreen(
        openDrawer = {},
        onEvent = {},
        registerUiState = registerUiState ,
        searchText = searchText ,
        currentPage = currentPage,
        pagingItems = pagingItems,
        navController = rememberNavController(),
        registerViewModel = registerViewModel
      )
    }

    // We wait for the text be drawn before we do the assertion
    composeTestRule
      .onNodeWithTag(REGISTER_TOP_BAR_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testFloatingActionButtonIsDisplayed() {
    val configurationRegistry : ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
    val registerUiState =
      RegisterUiState(
        screenTitle= "Register101",
        isFirstTimeSync = false,
        registerConfiguration = configurationRegistry.retrieveConfiguration(ConfigType.Register, "householdRegister"),
        registerId= "register101",
        totalRecordsCount = 0,
        filteredRecordsCount = 0,
        pagesCount = 1,
        progressPercentage = flowOf(0),
        isSyncUpload = flowOf(false),
        params = emptyMap())
    val searchText = mutableStateOf("")
    val currentPage = mutableStateOf(0)
    val pagingItems =mockk<LazyPagingItems<ResourceData>>().apply {  }
    val registerViewModel = mockk<RegisterViewModel>()
    val viewModelDataMigrationInProgress = MutableLiveData(false)
    every { registerViewModel.dataMigrationInProgress } returns viewModelDataMigrationInProgress

    composeTestRule.setContent {
      RegisterScreen(
        openDrawer = {},
        onEvent = {},
        registerUiState = registerUiState ,
        searchText = searchText ,
        currentPage = currentPage,
        pagingItems = pagingItems,
        navController = rememberNavController(),
        registerViewModel = registerViewModel
      )
    }
    // We wait for the text be drawn before we do the assertion
    composeTestRule.waitUntil(5_000) { true }
    composeTestRule
      .onAllNodesWithTag(FAB_REGISTER_BUTTON_TEST_TAG, useUnmergedTree = true)
  }

  @Test
  fun testThatDialogIsDisplayedDuringDataMigration() {
    val configurationRegistry : ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
    val registerUiState =
      RegisterUiState(
        screenTitle= "Register101",
        isFirstTimeSync = false,
        registerConfiguration = configurationRegistry.retrieveConfiguration(ConfigType.Register, "householdRegister"),
        registerId= "register101",
        totalRecordsCount = 0,
        filteredRecordsCount = 0,
        pagesCount = 1,
        progressPercentage = flowOf(0),
        isSyncUpload = flowOf(false),
        params = emptyMap())
    val searchText = mutableStateOf("")
    val currentPage = mutableStateOf(0)
    val pagingItems =mockk<LazyPagingItems<ResourceData>>().apply {  }
    val registerViewModel = mockk<RegisterViewModel>()
    val viewModelDataMigrationInProgress = MutableLiveData(true)
    every { registerViewModel.dataMigrationInProgress } returns viewModelDataMigrationInProgress

    composeTestRule.setContent {
      RegisterScreen(
        openDrawer = {},
        onEvent = {},
        registerUiState = registerUiState ,
        searchText = searchText ,
        currentPage = currentPage,
        pagingItems = pagingItems,
        navController = rememberNavController(),
        registerViewModel = registerViewModel
      )
    }
    composeTestRule.onNodeWithTag(DATA_MIGRATION_DIALOG).assertExists()
  }
  @Test
  fun testThatDialogIsDisplayedDuringSyncing() {
    val configurationRegistry : ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
    val registerUiState =
      RegisterUiState(
        screenTitle= "Register101",
        isFirstTimeSync = true,
        registerConfiguration = configurationRegistry.retrieveConfiguration(ConfigType.Register, "householdRegister"),
        registerId= "register101",
        totalRecordsCount = 0,
        filteredRecordsCount = 0,
        pagesCount = 1,
        progressPercentage = flowOf(0),
        isSyncUpload = flowOf(false),
        params = emptyMap())
    val searchText = mutableStateOf("")
    val currentPage = mutableStateOf(0)
    val pagingItems =mockk<LazyPagingItems<ResourceData>>().apply {  }
    val registerViewModel = mockk<RegisterViewModel>()
    val viewModelDataMigrationInProgress = MutableLiveData(false)
    every { registerViewModel.dataMigrationInProgress } returns viewModelDataMigrationInProgress

    composeTestRule.setContent {
      RegisterScreen(
        openDrawer = {},
        onEvent = {},
        registerUiState = registerUiState ,
        searchText = searchText ,
        currentPage = currentPage,
        pagingItems = pagingItems,
        navController = rememberNavController(),
        registerViewModel = registerViewModel
      )
    }
    composeTestRule.onNodeWithTag(FIRST_TIME_SYNC_DIALOG).assertExists()
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
}
