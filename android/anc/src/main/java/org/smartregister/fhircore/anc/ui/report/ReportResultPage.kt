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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun ReportResultPreview() {
  ReportResultPage(
    topBarTitle = "PageTitle",
    onBackPress = {},
    reportMeasureItem =
      ReportItem(
        description = "Description For Preview, i.e 4+ Anc women etc, 2 lines text in preview"
      ),
    startDate = "25 Nov, 2021",
    endDate = "29 Nov, 2021",
    isAllPatientSelection = true,
    selectedPatient = PatientItem(name = "Test Selected Patient")
  )
}

@Composable
fun ReportResultScreen(viewModel: ReportViewModel) {

  val reportMeasureItem by remember { mutableStateOf(viewModel.selectedMeasureReportItem.value) }
  val patientSelectionType by remember { mutableStateOf(viewModel.patientSelectionType.value) }
  val selectedPatient by remember { mutableStateOf(viewModel.selectedPatientItem.value) }
  val startDate by viewModel.startDate.observeAsState("")
  val endDate by viewModel.endDate.observeAsState("")
  val isAllPatientSelected = patientSelectionType == "All"
  ReportResultPage(
    topBarTitle = reportMeasureItem?.title ?: "",
    onBackPress = viewModel::onBackPressFromFilter,
    reportMeasureItem = reportMeasureItem ?: ReportItem(title = "Measure Report Missing"),
    startDate = startDate,
    endDate = endDate,
    isAllPatientSelection = isAllPatientSelected,
    selectedPatient = selectedPatient ?: PatientItem(name = "Patient Missing")
  )
}

@Composable
fun ReportResultPage(
  topBarTitle: String,
  onBackPress: () -> Unit,
  reportMeasureItem: ReportItem,
  startDate: String,
  endDate: String,
  isAllPatientSelection: Boolean,
  selectedPatient: PatientItem
) {
  Surface(color = colorResource(id = R.color.white)) {
    Column(modifier = Modifier.fillMaxSize().testTag(REPORT_RESULT_PAGE)) {
      TopBarBox(topBarTitle = topBarTitle, onBackPress = onBackPress)
      Spacer(modifier = Modifier.height(16.dp))
      DateRangeItem(
        text = reportMeasureItem.description,
        canChange = true,
        clickListener = {},
        Modifier.padding(start = 8.dp)
      )
      Spacer(modifier = Modifier.height(8.dp))
      DateSelectionBox(
        startDate = startDate,
        endDate = endDate,
        canChange = false,
        onDateRangePress = {}
      )
      Spacer(modifier = Modifier.height(16.dp))
      if (isAllPatientSelection) {
        Text(
          color = SubtitleTextColor,
          text = "Patient = All",
          fontSize = 14.sp,
          modifier = Modifier.wrapContentWidth()
        )
      } else {
        Text(
          color = SubtitleTextColor,
          text = "Patient = ${selectedPatient.name}",
          fontSize = 14.sp,
          modifier = Modifier.wrapContentWidth()
        )
      }
    }
  }
}
