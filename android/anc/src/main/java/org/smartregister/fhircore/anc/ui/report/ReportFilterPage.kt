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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

@Composable
fun ReportFilterPage(
  topBarTitle: String,
  onBackPress: () -> Unit,
  startDate: String,
  endDate: String,
  onDateRangePress: () -> Unit,
  patientSelectionText: String,
  onPatientSelectionTypeChanged: (String) -> Unit,
  generateReportEnabled: Boolean,
  onGenerateReportPress: () -> Unit,
  selectedPatient: PatientItem?
) {
  Surface(color = colorResource(id = R.color.white)) {
    Column(modifier = Modifier.fillMaxSize()) {
      TopBarBox(topBarTitle, onBackPress)
      DateSelectionBox(startDate, endDate, true, onDateRangePress)
      PatientSelectionBox(patientSelectionText, selectedPatient, onPatientSelectionTypeChanged)
      GenerateReportButton(generateReportEnabled, onGenerateReportPress)
    }
  }
}

@Composable
fun ReportFilterScreen(viewModel: ReportViewModel) {

  val reportMeasureItem by remember { mutableStateOf(viewModel.selectedMeasureReportItem.value) }
  val patientSelectionType by remember { mutableStateOf(viewModel.patientSelectionType.value) }
  val generateReportEnabled by remember { mutableStateOf(viewModel.isReadyToGenerateReport.value) }
  val selectedPatient by remember { mutableStateOf(viewModel.selectedPatientItem.value) }
  val startDate by viewModel.startDate.observeAsState("")
  val endDate by viewModel.endDate.observeAsState("")

  ReportFilterPage(
    topBarTitle = reportMeasureItem?.title ?: "",
    onBackPress = viewModel::onBackPressFromFilter,
    startDate = startDate,
    endDate = endDate,
    onDateRangePress = viewModel::onDateRangePress,
    patientSelectionText = patientSelectionType ?: "All",
    onPatientSelectionTypeChanged = viewModel::onPatientSelectionTypeChanged,
    generateReportEnabled = generateReportEnabled ?: true,
    onGenerateReportPress = viewModel::onGenerateReportPress,
    selectedPatient = selectedPatient ?: PatientItem()
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
    onDateRangePress = {},
    patientSelectionText = "ALL",
    onPatientSelectionTypeChanged = {},
    generateReportEnabled = false,
    onGenerateReportPress = {},
    selectedPatient = PatientItem()
  )
}

@Composable
fun GenerateReportButton(generateReportEnabled: Boolean, onGenerateReportClicked: () -> Unit) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
    verticalAlignment = Alignment.Bottom
  ) {
    Column(modifier = Modifier.align(Alignment.Bottom)) {
      Button(
        enabled = generateReportEnabled,
        onClick = onGenerateReportClicked,
        modifier =
          Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag(REPORT_GENERATE_BUTTON)
      ) {
        Text(
          color = Color.White,
          text = stringResource(id = R.string.generate_report),
          modifier = Modifier.padding(8.dp)
        )
      }
    }
  }
}
