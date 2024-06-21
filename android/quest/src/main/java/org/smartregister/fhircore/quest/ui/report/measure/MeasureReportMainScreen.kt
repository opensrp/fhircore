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

package org.smartregister.fhircore.quest.ui.report.measure

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.quest.navigation.MeasureReportNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.ui.report.measure.screens.MeasureReportListScreen
import org.smartregister.fhircore.quest.ui.report.measure.screens.MeasureReportResultScreen
import org.smartregister.fhircore.quest.ui.report.measure.screens.MeasureReportSubjectsScreen
import org.smartregister.fhircore.quest.ui.report.measure.screens.ReportDateSelectorScreen

@Composable
fun MeasureReportMainScreen(
  reportId: String,
  practitionerId: String,
  mainNavController: NavController,
  measureReportViewModel: MeasureReportViewModel,
) {
  // Use a different navController internally for navigating Report Composable screens
  val navController = rememberNavController()
  val uiState = measureReportViewModel.reportTypeSelectorUiState.value

  NavHost(
    navController = navController,
    startDestination = MeasureReportNavigationScreen.ReportDateSelector.route,
  ) {
    // Display list of supported measures for reporting
    composable(MeasureReportNavigationScreen.MeasureReportModule.route) {
      MeasureReportListScreen(
        navController = navController,
        dataList = measureReportViewModel.reportMeasuresList(reportId),
        onReportMeasureClicked = { measureReportRowData ->
          measureReportViewModel.onEvent(
            MeasureReportEvent.OnSelectMeasure(
              reportConfigurations = measureReportRowData,
              navController = navController,
              practitionerId = practitionerId,
            ),
          )
        },
        showProgressIndicator = uiState.showProgressIndicator,
      )
    }
    // Page for selecting report date
    composable(
      route = MeasureReportNavigationScreen.ReportDateSelector.route,
      arguments =
        listOf(
          navArgument(NavigationArg.SCREEN_TITLE) {
            type = NavType.StringType
            defaultValue = ""
          },
        ),
    ) {
      ReportDateSelectorScreen(
        reportId = reportId,
        practitionerId = practitionerId,
        screenTitle = stringResource(R.string.select_month),
        navController = navController,
        mainNavController = mainNavController,
        measureReportViewModel = measureReportViewModel,
      )
    }

    // Page for selecting subject to evaluate their measure
    composable(MeasureReportNavigationScreen.SubjectsList.route) {
      MeasureReportSubjectsScreen(
        reportId = reportId,
        navController = navController,
        measureReportViewModel = measureReportViewModel,
      )
    }

    // Page for displaying measure report results
    composable(MeasureReportNavigationScreen.MeasureReportResult.route) {
      MeasureReportResultScreen(
        navController = navController,
        measureReportViewModel = measureReportViewModel,
      )
    }
  }
}
