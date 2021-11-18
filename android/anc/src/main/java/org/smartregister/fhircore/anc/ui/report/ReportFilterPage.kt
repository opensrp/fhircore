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
import androidx.compose.material.Icon
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.ui.report.ReportViewModel.PatientSelectionType
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

const val GENERATE_REPORT_BUTTON_TAG = "generateReportButtonTag"
const val CHANGE_PATIENT_TAG = "changePatientButtonTag"

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
      DateSelectionBox(startDate, endDate, onDateRangePress)
      PatientSelectionBox(patientSelectionText, selectedPatient, onPatientSelectionTypeChanged)
      BottomButtonBox(generateReportEnabled, onGenerateReportPress)
    }
  }
}

@Composable
fun ReportFilterScreen(viewModel: ReportViewModel) {

  val reportMeasureItem by remember { mutableStateOf(viewModel.selectedMeasureReportItem.value) }
  val patientSelectionType by remember { mutableStateOf(viewModel.patientSelectionType.value) }
  val generateReportEnabled by remember { mutableStateOf(viewModel.isReadyToGenerateReport.value) }
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
    selectedPatient = viewModel.getSelectedPatient()
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
          .background(color = colorResource(id = R.color.backgroundGray))
          .wrapContentWidth()
          .padding(8.dp),
      contentAlignment = Alignment.Center
    ) { Text(text = text, textAlign = TextAlign.Center, fontSize = 16.sp) }
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun FilterSelectedPatientPreview() {
  SelectedPatientItem(
    selectedPatient = PatientItem(name = "PatientX"),
    onCancelSelectedPatient = {},
    onChangeClickListener = {}
  )
}

@Composable
fun SelectedPatientItem(
  selectedPatient: PatientItem,
  onCancelSelectedPatient: () -> Unit,
  onChangeClickListener: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.wrapContentWidth().padding(vertical = 8.dp, horizontal = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Box(
      modifier =
        modifier
          .clip(RoundedCornerShape(15.dp))
          .background(color = colorResource(id = R.color.backgroundGray))
          .wrapContentWidth()
          .padding(8.dp),
      contentAlignment = Alignment.Center
    ) {
      Row {
        Text(text = selectedPatient.name, textAlign = TextAlign.Center, fontSize = 16.sp)
        Row(
          modifier =
            modifier
              .clip(RoundedCornerShape(8.dp))
              .background(color = colorResource(id = R.color.backgroundGray))
              .wrapContentWidth()
              .clickable { onCancelSelectedPatient() }
              .padding(4.dp)
        ) {
          Text(
            text = "X",
            textAlign = TextAlign.Center,
            color = colorResource(id = android.R.color.transparent),
            fontSize = 16.sp
          )
          Icon(
            Icons.Filled.Close,
            contentDescription = "Back arrow",
            modifier = Modifier.padding(4.dp)
          )
        }
      }
    }
    Row(
      modifier =
        modifier
          .wrapContentWidth()
          .clickable { onChangeClickListener() }
          .padding(vertical = 8.dp, horizontal = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = stringResource(id = R.string.change),
        textAlign = TextAlign.Center,
        color = colorResource(id = android.R.color.holo_blue_light),
        fontSize = 16.sp
      )
    }
  }
}

@Composable
fun BottomButtonBox(generateReportEnabled: Boolean, onGenerateReportClicked: () -> Unit) {
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
          Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag(GENERATE_REPORT_BUTTON_TAG)
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
  selectedPatient: PatientItem?,
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

    if (patientSelection.value == PatientSelectionType.INDIVIDUAL) {
      Row {
        Spacer(modifier = Modifier.size(8.dp))
        SelectedPatientItem(
          selectedPatient = selectedPatient!!,
          onCancelSelectedPatient = { onPatientSelectionChange(PatientSelectionType.ALL) },
          onChangeClickListener = { onPatientSelectionChange(PatientSelectionType.INDIVIDUAL) }
        )
      }
    }
  }
}
