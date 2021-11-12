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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.RadioButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.ui.report.ReportViewModel.PatientSelectionType
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

const val GENERATE_REPORT_BUTTON_TAG = "generateReportButtonTag"

@Composable
fun ReportFilterPage(
  topBarTitle: String,
  onBackPress: () -> Unit,
  startDate: String,
  endDate: String,
  onDateRangePress: () -> Unit,
  patientSelectionText: String,
  onPatientSelectionTypeChanged: (String) -> Unit,
  onGenerateReportPress: () -> Unit
) {
  Surface(color = colorResource(id = R.color.white)) {
    Column(modifier = Modifier.fillMaxSize()) {
      TopBarBox(topBarTitle, onBackPress)
      DateSelectionBox(startDate, endDate, onDateRangePress)
      PatientSelectionBox(patientSelectionText, onPatientSelectionTypeChanged)
      BottomButtonBox(onGenerateReportPress)
    }
  }
}

@Composable
fun ReportFilterScreen(viewModel: ReportViewModel) {

  val reportMeasureItem by remember { mutableStateOf(viewModel.getSelectedReport()) }
  val patientSelectionType by remember { mutableStateOf(viewModel.getPatientSelectionType()) }
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
    onGenerateReportPress = viewModel::onGenerateReportPress
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
    onGenerateReportPress = {}
  )
}

@Composable
fun DateRangeItem(text: String, clickListener: () -> Unit, modifier: Modifier = Modifier) {
  Row(
    modifier =
      modifier
        .wrapContentWidth()
        .clickable { clickListener() }
        .padding(vertical = 8.dp, horizontal = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Box(
      modifier =
        modifier
          .clip(RoundedCornerShape(15.dp))
          .background(color = DividerColor)
          .wrapContentWidth()
          .padding(8.dp),
      contentAlignment = Alignment.Center
    ) { Text(text = text, textAlign = TextAlign.Center, fontSize = 16.sp) }
  }
}

@Composable
fun BottomButtonBox(onGenerateReportClicked: () -> Unit) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
    verticalAlignment = Alignment.Bottom
  ) {
    Column(modifier = Modifier.align(Alignment.Bottom)) {
      Button(
        enabled = true,
        onClick = onGenerateReportClicked,
        modifier = Modifier.fillMaxWidth().testTag(GENERATE_REPORT_BUTTON_TAG)
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

@Composable
fun DateSelectionBox(startDate: String, endDate: String, onDateRangePress: () -> Unit) {
  Column(
    modifier = Modifier.wrapContentWidth().padding(16.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.Start
  ) {
    Text(
      text = stringResource(id = R.string.date_range),
      fontWeight = FontWeight.Bold,
      fontSize = 18.sp,
      modifier = Modifier.wrapContentWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically
    ) {
      DateRangeItem(text = startDate, clickListener = onDateRangePress)
      Text("-", fontSize = 18.sp, modifier = Modifier.padding(horizontal = 4.dp))
      DateRangeItem(text = endDate, clickListener = onDateRangePress)
    }
  }
}

@Composable
fun PatientSelectionBox(
  patientSelectionText: String,
  onPatientSelectionChange: (String) -> Unit,
) {
  Column(
    modifier = Modifier.wrapContentWidth().padding(16.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.Start
  ) {
    val patientSelection = remember { mutableStateOf(patientSelectionText) }
    Text(
      text = stringResource(id = R.string.patient),
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.size(8.dp))
    Row {
      RadioButton(
        selected = patientSelection.value == PatientSelectionType.ALL,
        onClick = {
          patientSelection.value = PatientSelectionType.ALL
          onPatientSelectionChange(patientSelection.value)
        }
      )
      Spacer(modifier = Modifier.size(16.dp))
      Text(PatientSelectionType.ALL, fontSize = 16.sp)
    }
    Spacer(modifier = Modifier.size(8.dp))
    Row {
      RadioButton(
        selected = patientSelection.value == PatientSelectionType.INDIVIDUAL,
        onClick = {
          patientSelection.value = PatientSelectionType.INDIVIDUAL
          onPatientSelectionChange(patientSelection.value)
        }
      )
      Spacer(modifier = Modifier.size(16.dp))
      Text(PatientSelectionType.INDIVIDUAL, fontSize = 16.sp)
    }
  }
}
