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

package org.smartregister.fhircore.quest.ui.report.measure

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.anc.ui.anccare.shared.Anc
import org.smartregister.fhircore.engine.ui.register.RegisterDataViewModel
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

const val TOOLBAR_TITLE = "toolbarTitle"
const val TOOLBAR_BACK_ARROW = "toolbarBackArrow"
const val REPORT_MEASURE_LIST = "reportMeasureList"
const val REPORT_MEASURE_ITEM = "reportMeasureItem"
const val REPORT_FILTER_PAGE = "reportFilterPage"
const val REPORT_DATE_RANGE_SELECTION = "reportDateRangeSelection"
const val REPORT_DATE_SELECT_ITEM = "reportDateSelectItem"
const val REPORT_PATIENT_SELECTION = "reportPatientSelection"
const val REPORT_PATIENT_ITEM = "reportPatientItem"
const val REPORT_CANCEL_PATIENT = "reportCancelPatient"
const val REPORT_CHANGE_PATIENT = "reportChangePatient"
const val REPORT_SELECT_PATIENT_LIST = "reportSelectPatientList"
const val REPORT_SEARCH_PATIENT = "reportSearchPatient"
const val REPORT_SEARCH_PATIENT_CANCEL = "reportSearchPatientCancel"
const val REPORT_GENERATE_BUTTON = "reportGenerateButton"
const val REPORT_RESULT_PAGE = "reportResultPage"
const val ANC_PATIENT_ITEM = "ancPatientItem"
const val PATIENT_ANC_VISIT = "patientAncVisit"
const val REPORT_SEARCH_HINT = "reportSearchHint"
const val REPORT_RESULT_MEASURE_DESCRIPTION = "reportResultMeasureDescription"
const val REPORT_RESULT_ITEM_INDIVIDUAL = "reportResultIndividual"
const val REPORT_RESULT_PATIENT_DATA = "reportResultPatientData"
const val REPORT_RESULT_POPULATION_DATA = "reportResultPopulationData"
const val REPORT_RESULT_POPULATION_BOX = "reportResultPopulationBox"
const val REPORT_RESULT_POPULATION_ITEM = "reportResultPopulationItem"
const val INDICATOR_STATUS = "indicatorStatus"
const val PROGRESS_BAR_COLUMN = "progressBarColumn"
const val PROGRESS_BAR = "progressBar"
const val PROGRESS_BAR_TEXT = "progressBarText"
const val INDICATOR_TEXT = "indicatorText"

@Composable
fun ReportView(
  reportViewModel: ReportViewModel,
  registerDataViewModel: RegisterDataViewModel<Anc, PatientItem>,
) {
  // Choose which screen to show based on the value in the ReportScreen from ReportState
  when (reportViewModel.currentScreen) {
    ReportViewModel.ReportScreen.HOME -> ReportHomeScreen(reportViewModel)
    ReportViewModel.ReportScreen.FILTER -> ReportFilterScreen(reportViewModel)
    ReportViewModel.ReportScreen.PICK_PATIENT ->
      ReportSelectPatientScreen(
        viewModel = reportViewModel,
        registerDataViewModel = registerDataViewModel
      )
    ReportViewModel.ReportScreen.RESULT -> ReportResultScreen(reportViewModel)
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun TopBarPreview() {
  TopBarBox(topBarTitle = "Reports", onBackPress = {})
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
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun LoadingItemPreview() {
  LoadingItem()
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
fun PreviewDateSelection() {
  DateSelectionBox(
    startDate = "Start date",
    endDate = "End date",
    canChange = true,
    onDateRangeClick = {}
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PreviewDateRangeSelected() {
  DateSelectionBox(
    startDate = "Start date",
    endDate = "End date",
    canChange = false,
    onDateRangeClick = {}
  )
}

@Composable
fun DateSelectionBox(
  modifier: Modifier = Modifier,
  startDate: String = "",
  endDate: String = "",
  canChange: Boolean = false,
  showDateRangePicker: Boolean = true,
  onDateRangeClick: () -> Unit = {}
) {
  Column(
    modifier = modifier.wrapContentWidth().testTag(REPORT_DATE_RANGE_SELECTION),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.Start
  ) {
    Text(
      text = stringResource(id = R.string.date_range),
      fontWeight = FontWeight.Bold,
      fontSize = 18.sp,
      modifier = modifier.wrapContentWidth()
    )
    Spacer(modifier = modifier.height(16.dp))
    Row(
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically
    ) {
      DateRangeItem(text = startDate, canChange = canChange)
      Text("-", fontSize = 18.sp, modifier = modifier.padding(horizontal = 8.dp))
      DateRangeItem(text = endDate, canChange = canChange)
      if (showDateRangePicker) {
        Icon(
          Icons.Filled.CalendarToday,
          stringResource(R.string.date_range),
          modifier = modifier.clickable { onDateRangeClick() }.padding(8.dp)
        )
      }
    }
  }
}

@Composable
fun DateRangeItem(text: String, canChange: Boolean, modifier: Modifier = Modifier) {
  var newBackGroundColor = colorResource(id = R.color.transparent)
  var textPaddingHorizontal = 0.dp
  var textPaddingVertical = 0.dp

  if (canChange) {
    newBackGroundColor = colorResource(id = R.color.light)
    textPaddingHorizontal = 12.dp
    textPaddingVertical = 4.dp
  }

  Row(
    modifier = modifier.wrapContentWidth().testTag(REPORT_DATE_SELECT_ITEM),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Box(
      modifier =
        modifier
          .clip(RoundedCornerShape(15.dp))
          .background(color = newBackGroundColor)
          .wrapContentWidth()
          .padding(horizontal = textPaddingHorizontal, vertical = textPaddingVertical),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = text,
        textAlign = TextAlign.Start,
        fontSize = 16.sp,
        color = colorResource(id = R.color.darkGrayText)
      )
    }
  }
}

@Composable
fun PatientSelectionBox(
  radioOptions: List<Pair<String, Boolean>>,
  selectedPatient: PatientItem?,
  reportType: String,
  onReportTypeSelected: (String, Boolean) -> Unit,
) {
  Column(
    modifier = Modifier.wrapContentWidth().testTag(REPORT_PATIENT_SELECTION),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.Start
  ) {
    Text(
      text = stringResource(id = R.string.patient),
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold
    )

    radioOptions.forEach { optionPair ->
      Row {
        RadioButton(
          selected = reportType == optionPair.first,
          onClick = {
            if (optionPair.second) onReportTypeSelected(optionPair.first, true)
            else onReportTypeSelected(optionPair.first, false)
          }
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
          text = optionPair.first,
          fontSize = 16.sp,
        )
      }
      Spacer(modifier = Modifier.size(8.dp))
    }

    if (reportType.equals(stringResource(id = R.string.individual), ignoreCase = true) &&
        selectedPatient != null
    ) {
      Row(modifier = Modifier.padding(start = 24.dp)) {
        Spacer(modifier = Modifier.size(8.dp))
        SelectedPatientItem(
          selectedPatient = selectedPatient,
          onCancelSelectedPatient = { onReportTypeSelected(reportType, true) },
          onChangeClickListener = { onReportTypeSelected(reportType, true) }
        )
      }
    }
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun SelectedPatientPreview() {
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
    modifier =
      modifier
        .wrapContentWidth()
        .padding(vertical = 8.dp, horizontal = 8.dp)
        .testTag(REPORT_PATIENT_ITEM),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
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
      Row(
        modifier = Modifier.align(Alignment.Center),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = selectedPatient.name, textAlign = TextAlign.Center, fontSize = 16.sp)
        Spacer(modifier = Modifier.size(8.dp))
        Row(
          modifier =
            modifier
              .clip(RoundedCornerShape(8.dp))
              .background(color = colorResource(id = R.color.backgroundGray))
              .wrapContentWidth()
              .clickable { onCancelSelectedPatient() }
              .testTag(REPORT_CANCEL_PATIENT)
        ) {
          Box(
            modifier =
              modifier
                .clip(RoundedCornerShape(25.dp))
                .size(24.dp)
                .background(color = colorResource(id = R.color.darkGrayText))
                .wrapContentWidth()
                .padding(4.dp)
                .testTag(REPORT_CANCEL_PATIENT),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Filled.Close,
              contentDescription = "Back arrow",
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }
    Row(
      modifier =
        modifier
          .wrapContentWidth()
          .clickable { onChangeClickListener() }
          .padding(vertical = 8.dp, horizontal = 12.dp)
          .testTag(REPORT_CHANGE_PATIENT),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
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
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun ReportRowPreview() {
  val reportItem =
    ReportItem("fid", "4+ ANC Contacts ", "Pregnant women with at least four ANC Contacts", "4")
  ReportRow(reportItem = reportItem)
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
