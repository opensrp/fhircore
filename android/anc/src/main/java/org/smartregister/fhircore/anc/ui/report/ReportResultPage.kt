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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.anc.data.report.model.ResultItem
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PreviewResultItemIndividual() {
  ResultItemIndividual(
    selectedPatient = PatientItem(demographics = "Jacky Coughlin, F, 27"),
    isMatchedIndicator = true,
    indicatorStatus = "True",
    indicatorDescription = "Jacky Got her first ANC contact"
  )
}

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
    isAllPatientSelection = false,
    selectedPatient =
      PatientItem(name = "Test Selected Patient", demographics = "Test Select, F, 28"),
    ResultItem(status = "True", description = "Testing Preview text")
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
  val resultForIndividual by viewModel.resultForIndividual.observeAsState(ResultItem())

  ReportResultPage(
    topBarTitle = reportMeasureItem?.title ?: "",
    onBackPress = viewModel::onBackPressFromResult,
    reportMeasureItem = reportMeasureItem ?: ReportItem(title = "Measure Report Missing"),
    startDate = startDate,
    endDate = endDate,
    isAllPatientSelection = isAllPatientSelected,
    selectedPatient = selectedPatient ?: PatientItem(name = "Patient Missing"),
    resultForIndividual = resultForIndividual
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
  selectedPatient: PatientItem,
  resultForIndividual: ResultItem
) {
  Surface(color = colorResource(id = R.color.white)) {
    Column(
      modifier =
        Modifier.background(color = colorResource(id = R.color.backgroundGray))
          .fillMaxSize()
          .testTag(REPORT_RESULT_PAGE)
    ) {
      TopBarBox(topBarTitle = topBarTitle, onBackPress = onBackPress)
      Column(modifier = Modifier.padding(16.dp)) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
              .background(color = colorResource(id = R.color.light_gray))
              .padding(12.dp)
              .wrapContentWidth(),
          contentAlignment = Alignment.Center
        ) {
          Text(text = reportMeasureItem.description, textAlign = TextAlign.Start, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        DateSelectionBox(startDate = startDate, endDate = endDate, canChange = false)
        Spacer(modifier = Modifier.height(16.dp))
        if (isAllPatientSelection) {
          Text(
            color = SubtitleTextColor,
            text = "Patient = All",
            fontSize = 14.sp,
            modifier = Modifier.wrapContentWidth()
          )
        } else {
          ResultItemIndividual(
            selectedPatient = selectedPatient,
            isMatchedIndicator = resultForIndividual.isMatchedIndicator,
            indicatorStatus = resultForIndividual.status,
            indicatorDescription = resultForIndividual.description
          )
        }
      }
    }
  }
}
