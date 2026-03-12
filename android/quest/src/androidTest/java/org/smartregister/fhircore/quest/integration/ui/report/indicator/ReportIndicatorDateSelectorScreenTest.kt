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
import java.util.Calendar
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.quest.integration.Faker
import org.smartregister.fhircore.quest.ui.report.indicator.ReportIndicatorDateSelectorScreen
import org.smartregister.fhircore.quest.ui.report.indicator.ReportIndicatorViewModel
import org.smartregister.fhircore.quest.ui.report.models.ReportRangeSelectionData

@HiltAndroidTest
class ReportIndicatorDateSelectorScreenTest {
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
  fun testDateSelectorScreenDisplaysLoadingState() {
    reportIndicatorViewModel.isLoading.value = true

    composeTestRule.setContent {
      AppTheme {
        ReportIndicatorDateSelectorScreen(
          reportId = testReportId,
          reportIndicatorViewModel = reportIndicatorViewModel,
          navController = navController,
          mainNavController = navController,
        )
      }
    }

    composeTestRule.onNodeWithText("Please wait…").assertIsDisplayed()
  }

  @Test
  fun testDateSelectorScreenDisplaysNoDataMessage() {
    reportIndicatorViewModel.isLoading.value = false
    reportIndicatorViewModel.reportPeriodRange.value = emptyMap()

    composeTestRule.setContent {
      AppTheme {
        ReportIndicatorDateSelectorScreen(
          reportId = testReportId,
          reportIndicatorViewModel = reportIndicatorViewModel,
          navController = navController,
          mainNavController = navController,
        )
      }
    }

    composeTestRule.onNodeWithText("No data available").assertIsDisplayed()
  }

  @Test
  fun testDateSelectorScreenDisplaysYearHeaders() {
    val calendar = Calendar.getInstance()
    calendar.set(2024, Calendar.JANUARY, 1)

    val testData =
      mapOf(
        "2024" to
          listOf(
            ReportRangeSelectionData("January", "2024", calendar.time),
            ReportRangeSelectionData("February", "2024", calendar.time),
          ),
      )

    reportIndicatorViewModel.isLoading.value = false
    reportIndicatorViewModel.reportPeriodRange.value = testData

    composeTestRule.setContent {
      AppTheme {
        ReportIndicatorDateSelectorScreen(
          reportId = testReportId,
          reportIndicatorViewModel = reportIndicatorViewModel,
          navController = navController,
          mainNavController = navController,
        )
      }
    }

    composeTestRule.onNodeWithText("2024").assertIsDisplayed()
  }

  @Test
  fun testDateSelectorScreenDisplaysFullYearOption() {
    val calendar = Calendar.getInstance()
    calendar.set(2024, Calendar.JANUARY, 1)

    val testData =
      mapOf(
        "2024" to
          listOf(
            ReportRangeSelectionData("January", "2024", calendar.time),
          ),
      )

    reportIndicatorViewModel.isLoading.value = false
    reportIndicatorViewModel.reportPeriodRange.value = testData

    composeTestRule.setContent {
      AppTheme {
        ReportIndicatorDateSelectorScreen(
          reportId = testReportId,
          reportIndicatorViewModel = reportIndicatorViewModel,
          navController = navController,
          mainNavController = navController,
        )
      }
    }

    composeTestRule.onNodeWithText("Full Year").assertIsDisplayed()
  }

  @Test
  fun testDateSelectorScreenDisplaysTopAppBar() {
    composeTestRule.setContent {
      AppTheme {
        ReportIndicatorDateSelectorScreen(
          reportId = testReportId,
          reportIndicatorViewModel = reportIndicatorViewModel,
          navController = navController,
          mainNavController = navController,
        )
      }
    }

    composeTestRule.onNodeWithText("Select Period").assertIsDisplayed()
  }
}
