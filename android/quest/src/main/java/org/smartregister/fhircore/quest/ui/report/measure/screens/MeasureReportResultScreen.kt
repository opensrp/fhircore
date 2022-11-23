/*
 * Copyright 2021 Ona Systems, Inc
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfig
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.report.measure.MeasureReportViewModel
import org.smartregister.fhircore.quest.ui.report.measure.components.DateRangeItem
import org.smartregister.fhircore.quest.ui.report.measure.components.MeasureReportIndividualResultView
import org.smartregister.fhircore.quest.ui.report.measure.components.MeasureReportPopulationResultView
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportIndividualResult
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportPopulationResult
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportPatientViewData

@Composable
fun MeasureReportResultScreen(
  navController: NavController,
  measureReportViewModel: MeasureReportViewModel
) {
  val uiState = measureReportViewModel.reportTypeSelectorUiState.value

  // Previously selected measure from the list of supported measures
  val measureReportRowData = measureReportViewModel.measureReportConfigList

  MeasureReportResultPage(
    screenTitle = measureReportRowData.firstOrNull()?.module ?: "",
    navController = navController,
    measureReportConfig = measureReportRowData,
    endDate = uiState.endDate,
    startDate = uiState.startDate,
    measureReportIndividualResult = measureReportViewModel.measureReportIndividualResult.value,
    measureReportPopulationResult = measureReportViewModel.measureReportPopulationResults.value,
    patientViewData = uiState.patientViewData
  )
}

@Composable
fun MeasureReportResultPage(
  screenTitle: String,
  navController: NavController,
  measureReportConfig: MutableList<MeasureReportConfig>,
  startDate: String,
  endDate: String,
  patientViewData: MeasureReportPatientViewData?,
  measureReportIndividualResult: MeasureReportIndividualResult?,
  measureReportPopulationResult: List<MeasureReportPopulationResult>?,
  modifier: Modifier = Modifier
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
        backgroundColor = MaterialTheme.colors.primary
      )
    }
  ) { innerPadding ->
    Column(
      modifier =
        modifier
          .padding(innerPadding)
          .background(color = colorResource(id = R.color.backgroundGray))
          .fillMaxSize()
    ) {
      Column(modifier = modifier.padding(16.dp)) {
        Box(
          modifier =
            modifier
              .clip(RoundedCornerShape(8.dp))
              .background(color = colorResource(id = R.color.light_gray_background))
              .padding(12.dp)
              .wrapContentWidth(),
          contentAlignment = Alignment.Center
        ) {
          if (measureReportConfig != null)
            Text(
              text = measureReportConfig.first().description,
              textAlign = TextAlign.Start,
              fontSize = 16.sp
            )
        }
        Spacer(modifier = modifier.height(16.dp))

        // Display date range e.g. 1 Apr, 2020 - 28 Apr, 2022
        Row(
          horizontalArrangement = Arrangement.SpaceAround,
          verticalAlignment = Alignment.CenterVertically
        ) {
          DateRangeItem(text = startDate, showBackground = false)
          Text("-", fontSize = 18.sp, modifier = modifier.padding(horizontal = 8.dp))
          DateRangeItem(text = endDate, showBackground = false)
        }
        Spacer(modifier = modifier.height(16.dp))

        // Switch between individual and population result views
        if (measureReportIndividualResult != null && patientViewData != null) {
          MeasureReportIndividualResultView(
            patientViewData = patientViewData,
            isMatchedIndicator = measureReportIndividualResult.isMatchedIndicator,
            indicatorStatus = measureReportIndividualResult.status,
            indicatorDescription = measureReportIndividualResult.description
          )
        }
        if (measureReportPopulationResult != null) {
          MeasureReportPopulationResultView(measureReportPopulationResult.distinct())
        }
      }
    }
  }
}
//
@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
private fun MeasureReportResultScreenForIndividualPreview() {
  MeasureReportResultPage(
    screenTitle = "First ANC",
    navController = rememberNavController(),
    measureReportConfig =
      mutableListOf(
        MeasureReportConfig(
          title = "First ANC",
          description = "Description For Preview, i.e 4+ Anc women etc, 2 lines text in preview"
        )
      ),
    startDate = "25 Nov, 2021",
    endDate = "29 Nov, 2021",
    patientViewData =
      MeasureReportPatientViewData(
        name = "Jacky Coughlin",
        gender = "F",
        age = "27",
        logicalId = "1920192"
      ),
    measureReportIndividualResult =
      MeasureReportIndividualResult(status = "True", isMatchedIndicator = true, description = ""),
    null
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
private fun MeasureReportResultScreenForPopulationPreview() {
  val testResultItem1 =
    MeasureReportIndividualResult(title = "10 - 15 years", percentage = "10", count = "1/10")
  val testResultItem2 =
    MeasureReportIndividualResult(title = "16 - 20 years", percentage = "50", count = "30/60")
  MeasureReportResultPage(
    screenTitle = "First ANC",
    navController = rememberNavController(),
    measureReportConfig =
      mutableListOf(
        MeasureReportConfig(
          description = "Description For Preview, i.e 4+ Anc women etc, 2 lines text in preview"
        )
      ),
    startDate = "25 Nov, 2021",
    endDate = "29 Nov, 2021",
    patientViewData =
      MeasureReportPatientViewData(
        name = "Jacky Coughlin",
        gender = "F",
        age = "27",
        logicalId = "1902912"
      ),
    measureReportIndividualResult = null,
    measureReportPopulationResult =
      listOf(
        MeasureReportPopulationResult(
          title = "Age Range",
          count = "1/2",
          listOf(testResultItem1, testResultItem2)
        ),
        MeasureReportPopulationResult(
          title = "Education Level",
          count = "2/3",
          listOf(testResultItem1, testResultItem2)
        )
      )
  )
}
