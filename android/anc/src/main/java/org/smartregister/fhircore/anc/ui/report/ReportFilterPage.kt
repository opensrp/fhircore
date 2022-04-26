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

package org.smartregister.fhircore.anc.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

@Composable
fun ReportFilterPage(
  topBarTitle: String,
  onBackPress: (ReportViewModel.ReportScreen) -> Unit,
  startDate: String,
  endDate: String,
  onDateRangeClick: () -> Unit,
  selectedPatient: PatientItem?,
  reportType: String,
  onReportTypeSelected: (String, Boolean) -> Unit,
  generateReport: Boolean,
  onGenerateReportClicked: () -> Unit,
  showProgressIndicator: Boolean = false
) {

  Surface(color = colorResource(id = R.color.white)) {
    Column(modifier = Modifier.fillMaxSize().testTag(REPORT_FILTER_PAGE)) {
      TopBarBox(topBarTitle) { onBackPress(ReportViewModel.ReportScreen.HOME) }
      Box(modifier = Modifier.padding(16.dp).fillMaxSize(), contentAlignment = Alignment.Center) {
        if (showProgressIndicator) {
          Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.testTag(PROGRESS_BAR_COLUMN)
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(40.dp).testTag(PROGRESS_BAR),
              strokeWidth = 2.dp
            )
            Text(
              text = stringResource(R.string.please_wait),
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(vertical = 16.dp).testTag(PROGRESS_BAR_TEXT)
            )
          }
        } else {
          Column {
            DateSelectionBox(
              startDate = startDate,
              endDate = endDate,
              canChange = true,
              onDateRangeClick = onDateRangeClick,
            )
            Spacer(modifier = Modifier.size(32.dp))
            PatientSelectionBox(
              radioOptions =
                listOf(
                  Pair(stringResource(R.string.all), false),
                  Pair(stringResource(R.string.individual), true)
                ),
              selectedPatient = selectedPatient,
              reportType = reportType,
              onReportTypeSelected = onReportTypeSelected
            )
            Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
              Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                GenerateReportButton(
                  generateReportEnabled = generateReport,
                  onGenerateReportClicked = onGenerateReportClicked
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun ReportFilterScreen(viewModel: ReportViewModel) {
  val reportMeasureItem by viewModel.selectedMeasureReportItem.observeAsState(null)
  val selectedPatient by viewModel.getSelectedPatient().observeAsState(initial = null)
  val startDate by viewModel.startDate.observeAsState("")
  val endDate by viewModel.endDate.observeAsState("")
  val reportType by viewModel.currentReportType.observeAsState("")
  val generateReport by viewModel.generateReport.observeAsState(false)
  val showProgressIndicator by viewModel.showProgressIndicator.observeAsState(false)

  ReportFilterPage(
    topBarTitle = reportMeasureItem?.title ?: "",
    onBackPress = viewModel::onBackPress,
    startDate = startDate,
    endDate = endDate,
    onDateRangeClick = viewModel::onDateRangeClick,
    selectedPatient = selectedPatient,
    generateReport = generateReport,
    onGenerateReportClicked = viewModel::onGenerateReportClicked,
    reportType = reportType,
    onReportTypeSelected = viewModel::onReportTypeSelected,
    showProgressIndicator = showProgressIndicator
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PreviewPatientSelectionAll() {
  PatientSelectionBox(
    radioOptions =
      listOf(
        Pair(stringResource(R.string.all), false),
        Pair(stringResource(R.string.individual), true)
      ),
    reportType = "Individual",
    onReportTypeSelected = { _, _ -> },
    selectedPatient = null
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PreviewPatientSelectionIndividual() {
  PatientSelectionBox(
    radioOptions =
      listOf(
        Pair(stringResource(R.string.all), false),
        Pair(stringResource(R.string.individual), true)
      ),
    reportType = "All",
    onReportTypeSelected = { _, _ -> },
    selectedPatient = PatientItem(name = "Ind Patient Item")
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun ReportFilterPreview() {
  ReportFilterPage(
    topBarTitle = "PageTitle",
    onBackPress = {},
    startDate = "StartDate",
    endDate = "EndDate",
    onDateRangeClick = {},
    selectedPatient = PatientItem(),
    generateReport = true,
    onGenerateReportClicked = {},
    onReportTypeSelected = { _, _ -> },
    reportType = "All",
    showProgressIndicator = false
  )
}

@Composable
fun GenerateReportButton(generateReportEnabled: Boolean, onGenerateReportClicked: () -> Unit) {
  Column {
    Button(
      enabled = generateReportEnabled,
      onClick = onGenerateReportClicked,
      modifier = Modifier.fillMaxWidth().testTag(REPORT_GENERATE_BUTTON)
    ) {
      Text(
        color = Color.White,
        text = stringResource(id = R.string.generate_report),
        modifier = Modifier.padding(8.dp)
      )
    }
  }
}
