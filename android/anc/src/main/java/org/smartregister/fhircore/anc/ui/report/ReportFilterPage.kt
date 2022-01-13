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
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
  onStartDatePress: () -> Unit,
  onEndDatePress: () -> Unit,
  patientSelectionText: String,
  onPatientSelectionTypeChanged: (String) -> Unit,
  onGenerateReportPress: () -> Unit,
  selectedPatient: PatientItem?
) {
  var generateReportEnabled by remember { mutableStateOf(false) }

  Surface(color = colorResource(id = R.color.white)) {
    Column(modifier = Modifier.fillMaxSize().testTag(REPORT_FILTER_PAGE)) {
      TopBarBox(topBarTitle, onBackPress)
      Box(modifier = Modifier.padding(16.dp)) {
        Column {
          DateSelectionBox(
            startDate = startDate,
            endDate = endDate,
            canChange = true,
            onStartDatePress = {
              onStartDatePress()
              generateReportEnabled = startDate.isNotEmpty() && endDate.isNotEmpty()
            },
            onEndDatePress = {
              onEndDatePress()
              generateReportEnabled = startDate.isNotEmpty() && endDate.isNotEmpty()
            }
          )
          Spacer(modifier = Modifier.size(32.dp))
          PatientSelectionBox(patientSelectionText, selectedPatient, onPatientSelectionTypeChanged)
          Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
              GenerateReportButton(generateReportEnabled, onGenerateReportPress)
            }
          }
        }
      }
    }
  }
}

@Composable
fun ReportFilterScreen(viewModel: ReportViewModel) {
  val reportMeasureItem by remember { mutableStateOf(viewModel.selectedMeasureReportItem.value) }
  val patientSelectionType by remember { mutableStateOf(viewModel.patientSelectionType.value) }
  val selectedPatient by remember { mutableStateOf(viewModel.getSelectedPatient().value) }
  val startDate by viewModel.startDate.observeAsState("")
  val endDate by viewModel.endDate.observeAsState("")

  ReportFilterPage(
    topBarTitle = reportMeasureItem?.title ?: "",
    onBackPress = viewModel::onBackPressFromFilter,
    startDate = startDate,
    endDate = endDate,
    onStartDatePress = viewModel::onStartDatePress,
    onEndDatePress = viewModel::onEndDatePress,
    patientSelectionText = patientSelectionType ?: "All",
    onPatientSelectionTypeChanged = viewModel::onPatientSelectionTypeChanged,
    onGenerateReportPress = viewModel::auxGenerateReport,
    selectedPatient = selectedPatient
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PreviewPatientSelectionAll() {
  PatientSelectionBox(
    patientSelectionText = ReportViewModel.PatientSelectionType.ALL,
    onPatientSelectionChange = {},
    selectedPatient = PatientItem()
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PreviewPatientSelectionIndividual() {
  PatientSelectionBox(
    patientSelectionText = ReportViewModel.PatientSelectionType.INDIVIDUAL,
    onPatientSelectionChange = {},
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
    onStartDatePress = {},
    onEndDatePress = {},
    patientSelectionText = "ALL",
    onPatientSelectionTypeChanged = {},
    onGenerateReportPress = {},
    selectedPatient = PatientItem()
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
