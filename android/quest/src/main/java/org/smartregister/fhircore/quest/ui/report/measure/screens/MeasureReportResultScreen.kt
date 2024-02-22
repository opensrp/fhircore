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

package org.smartregister.fhircore.quest.ui.report.measure.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.quest.ui.report.measure.MeasureReportViewModel
import org.smartregister.fhircore.quest.ui.report.measure.components.DateRangeItem
import org.smartregister.fhircore.quest.ui.report.measure.components.MeasureReportIndividualResultView
import org.smartregister.fhircore.quest.ui.report.measure.components.MeasureReportPopulationResultView
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportIndividualResult
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportPopulationResult
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportSubjectViewData

@Composable
fun MeasureReportResultScreen(
  navController: NavController,
  measureReportViewModel: MeasureReportViewModel,
) {
  val uiState = measureReportViewModel.reportTypeSelectorUiState.value

  // Previously selected measure from the list of supported measures
  val measureReportRowData = measureReportViewModel.reportConfigurations

  MeasureReportResultPage(
    screenTitle = measureReportRowData.firstOrNull()?.module ?: "",
    navController = navController,
    startDate = uiState.startDate,
    endDate = uiState.endDate,
    subjectViewData = uiState.subjectViewData,
    measureReportIndividualResult = measureReportViewModel.measureReportIndividualResult.value,
    measureReportPopulationResult = measureReportViewModel.measureReportPopulationResults.value,
  )
}

@Composable
fun MeasureReportResultPage(
  screenTitle: String,
  navController: NavController,
  startDate: String,
  endDate: String,
  subjectViewData: Set<MeasureReportSubjectViewData>,
  measureReportIndividualResult: MeasureReportIndividualResult?,
  measureReportPopulationResult: List<MeasureReportPopulationResult>?,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(text = screenTitle, overflow = TextOverflow.Ellipsis, maxLines = 1) },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Filled.ArrowBack, null)
          }
        },
        contentColor = Color.White,
        backgroundColor = MaterialTheme.colors.primary,
      )
    },
  ) { innerPadding ->
    Column(
      modifier =
        modifier
          .padding(innerPadding)
          .background(color = colorResource(id = R.color.backgroundGray))
          .fillMaxSize(),
    ) {
      Column(modifier = modifier.padding(16.dp)) {
        Spacer(modifier = modifier.height(16.dp))

        // Display date range e.g. 1 Apr, 2020 - 28 Apr, 2022
        Row(
          horizontalArrangement = Arrangement.SpaceAround,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          DateRangeItem(text = startDate, showBackground = false)
          Text("-", fontSize = 18.sp, modifier = modifier.padding(horizontal = 8.dp))
          DateRangeItem(text = endDate, showBackground = false)
        }
        Spacer(modifier = modifier.height(16.dp))

        // Switch between individual and population result views
        if (measureReportIndividualResult != null && subjectViewData.isNotEmpty()) {
          Row(modifier = modifier.fillMaxWidth()) {
            subjectViewData.forEach {
              MeasureReportIndividualResultView(
                subjectViewData = it,
              )
            }
          }
        }
        if (measureReportPopulationResult != null) {
          MeasureReportPopulationResultView(
            measureReportPopulationResult.distinctBy { it.title },
          )
        }
      }
    }
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun MeasureReportResultScreenForIndividualPreview() {
  MeasureReportResultPage(
    screenTitle = "First ANC",
    navController = rememberNavController(),
    startDate = "25 Nov, 2021",
    endDate = "29 Nov, 2021",
    subjectViewData =
      setOf(
        MeasureReportSubjectViewData(
          display = "Jacky Coughlin, F, 27",
          logicalId = "1920192",
          type = ResourceType.Patient,
        ),
        MeasureReportSubjectViewData(
          display = "Jane Doe, F, 18",
          logicalId = "1910192",
          type = ResourceType.Patient,
        ),
      ),
    measureReportIndividualResult =
      MeasureReportIndividualResult(status = "True", isMatchedIndicator = true, description = ""),
    null,
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun MeasureReportResultScreenForPopulationPreview() {
  val testResultItem1 =
    MeasureReportIndividualResult(title = "10 - 15 years", percentage = "10", count = "1/10")
  val testResultItem2 =
    MeasureReportIndividualResult(title = "16 - 20 years", percentage = "50", count = "30/60")
  MeasureReportResultPage(
    screenTitle = "First ANC",
    navController = rememberNavController(),
    startDate = "25 Nov, 2021",
    endDate = "29 Nov, 2021",
    subjectViewData =
      setOf(
        MeasureReportSubjectViewData(
          display = "Jacky Coughlin, F, 27",
          logicalId = "1902912",
          type = ResourceType.Patient,
        ),
        MeasureReportSubjectViewData(
          display = "Jane Doe, F, 18",
          logicalId = "1912912",
          type = ResourceType.Patient,
        ),
      ),
    measureReportIndividualResult = null,
    measureReportPopulationResult =
      listOf(
        MeasureReportPopulationResult(
          title = "Age Range",
          count = "1/2",
          listOf(testResultItem1, testResultItem2),
        ),
        MeasureReportPopulationResult(
          title = "Education Level",
          count = "2/3",
          listOf(testResultItem1, testResultItem2),
        ),
      ),
  )
}
