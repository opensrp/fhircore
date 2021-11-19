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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

const val TOOLBAR_TITLE = "toolbarTitle"
const val TOOLBAR_BACK_ARROW = "toolbarBackArrow"
const val REPORT_HOME_PRELOAD = "reportHomePreLoad"
const val REPORT_MEASURE_LIST = "reportMeasureList"
const val REPORT_MEASURE_ITEM = "reportMeasureItem"
const val REPORT_DATE_RANGE_SELECTION = "reportDateRangeSelection"
const val REPORT_DATE_SELECT_ITEM = "reportDateSelectItem"
const val REPORT_PATIENT_SELECTION = "reportPatientSelection"
const val REPORT_PATIENT_ITEM = "reportPatientItem"
const val REPORT_CANCEL_PATIENT = "reportCancelPatient"
const val REPORT_CHANGE_PATIENT = "reportChangePatient"
const val REPORT_SELECT_PATIENT_LIST = "reportSelectPatientList"
const val REPORT_SEARCH_PATIENT = "reportSearchPatient"
const val REPORT_GENERATE_BUTTON = "reportGenerateButton"
const val REPORT_RESULT_PAGE = "reportResultPage"

@Composable
fun ReportView(reportViewModel: ReportViewModel) {
  // Choose which screen to show based on the value in the ReportScreen from ReportState
  when (reportViewModel.reportState.currentScreen) {
    ReportViewModel.ReportScreen.HOME -> ReportHomeScreen(reportViewModel)
    ReportViewModel.ReportScreen.FILTER -> ReportFilterScreen(reportViewModel)
    ReportViewModel.ReportScreen.PICK_PATIENT -> ReportSelectPatientScreen(reportViewModel)
    ReportViewModel.ReportScreen.RESULT -> ReportResultScreen(reportViewModel)
    ReportViewModel.ReportScreen.PREHOMElOADING -> ReportPreLoadingHomeScreen(reportViewModel)
  }
}

@Composable
fun LoadingItem() {
  CircularProgressIndicator(
    modifier =
      Modifier.testTag("ProgressBarItem")
        .fillMaxWidth()
        .padding(16.dp)
        .wrapContentWidth(Alignment.CenterHorizontally)
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PatientSelectionPreview() {
  PatientSelectionBox(
    patientSelectionText = ReportViewModel.PatientSelectionType.ALL,
    onPatientSelectionChange = {},
    selectedPatient = PatientItem()
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun DateRangePreview() {
  DateSelectionBox(startDate = "Start date", endDate = "End date", onDateRangePress = {})
}

@Composable
fun TopBarBox(topBarTitle: String, onBackPress: () -> Unit) {
  TopAppBar(
    title = {
      Text(
        text = topBarTitle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.testTag(TOOLBAR_TITLE)
      )
    },
    navigationIcon = {
      IconButton(onClick = onBackPress, Modifier.testTag(TOOLBAR_BACK_ARROW)) {
        Icon(Icons.Filled.ArrowBack, contentDescription = "Back arrow")
      }
    }
  )
}

@Composable
fun ReportRow(
  reportItem: ReportItem,
  modifier: Modifier = Modifier,
) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
  ) {
    Column(modifier = modifier.padding(16.dp).weight(0.70f).testTag(REPORT_MEASURE_ITEM)) {
      Text(text = reportItem.title, fontSize = 18.sp, modifier = modifier.wrapContentWidth())
      Spacer(modifier = modifier.height(8.dp))
      Row(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          color = SubtitleTextColor,
          text = reportItem.description,
          fontSize = 14.sp,
          modifier = modifier.wrapContentWidth()
        )
      }
    }
    Image(
      painter = painterResource(id = R.drawable.ic_forward_arrow),
      contentDescription = "",
      colorFilter = ColorFilter.tint(colorResource(id = R.color.status_gray)),
      modifier = Modifier.padding(end = 12.dp)
    )
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun ReportRowPreview() {
  val reportItem =
    ReportItem("fid", "4+ ANC Contacts ", "Pregnant women with at least four ANC Contacts", "4")
  ReportRow(reportItem = reportItem)
}
