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

package org.smartregister.fhircore.quest.integration.ui.report.indicator

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.quest.integration.Faker
import org.smartregister.fhircore.quest.ui.report.indicator.ReportIndicatorList
import org.smartregister.fhircore.quest.ui.report.indicator.ReportIndicatorViewModel

@HiltAndroidTest
class ReportIndicatorScreenTest {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val composeTestRule = createComposeRule()

  @BindValue
  val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @Inject lateinit var defaultRepository: DefaultRepository

  private lateinit var reportIndicatorViewModel: ReportIndicatorViewModel
  private lateinit var navController: TestNavHostController
  private val testReportId = "test-report-id"

  @Before
  fun setUp() {
    hiltRule.inject()
    reportIndicatorViewModel = ReportIndicatorViewModel(defaultRepository)
    navController = TestNavHostController(ApplicationProvider.getApplicationContext())
  }

  @Test
  fun testReportIndicatorListDisplaysLoadingState() {
    reportIndicatorViewModel.isLoading.value = true

    composeTestRule.setContent {
      AppTheme {
        ReportIndicatorList(
          navController = navController,
          reportIndicatorViewModel = reportIndicatorViewModel,
          reportId = testReportId,
          startDate = "2024-01-01",
          endDate = "2024-01-31",
          periodLabel = "January 2024 Reports",
        )
      }
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun testReportIndicatorListDisplaysNoDataMessage() {
    reportIndicatorViewModel.isLoading.value = false
    reportIndicatorViewModel.reportIndicatorsMap.clear()

    composeTestRule.setContent {
      AppTheme {
        ReportIndicatorList(
          navController = navController,
          reportIndicatorViewModel = reportIndicatorViewModel,
          reportId = testReportId,
          startDate = "2024-01-01",
          endDate = "2024-01-31",
          periodLabel = "January 2024 Reports",
        )
      }
    }

    composeTestRule.onNodeWithText("No data available").assertIsDisplayed()
  }

  @Test
  fun testReportIndicatorListDisplaysPeriodLabel() {
    reportIndicatorViewModel.isLoading.value = false
    reportIndicatorViewModel.reportIndicatorsMap.clear()

    composeTestRule.setContent {
      AppTheme {
        ReportIndicatorList(
          navController = navController,
          reportIndicatorViewModel = reportIndicatorViewModel,
          reportId = testReportId,
          startDate = "2024-01-01",
          endDate = "2024-01-31",
          periodLabel = "January 2024 Reports",
        )
      }
    }

    composeTestRule.onNodeWithText("January 2024 Reports").assertIsDisplayed()
  }

  @Test
  fun testReportIndicatorListDisplaysTopAppBar() {
    reportIndicatorViewModel.isLoading.value = false

    composeTestRule.setContent {
      AppTheme {
        ReportIndicatorList(
          navController = navController,
          reportIndicatorViewModel = reportIndicatorViewModel,
          reportId = testReportId,
          startDate = "2024-01-01",
          endDate = "2024-01-31",
          periodLabel = "January 2024 Reports",
        )
      }
    }

    composeTestRule.onNodeWithText("January 2024 Reports").assertIsDisplayed()
  }
}
