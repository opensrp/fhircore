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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.util.Pair
import androidx.navigation.NavController
import java.util.Calendar
import java.util.Date
import org.hl7.fhir.r4.model.MeasureReport
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.SearchHeaderColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.parseDate
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.navigation.MeasureReportNavigationScreen
import org.smartregister.fhircore.quest.ui.report.measure.MeasureReportEvent
import org.smartregister.fhircore.quest.ui.report.measure.MeasureReportViewModel
import org.smartregister.fhircore.quest.ui.report.measure.components.DateSelectionBox
import org.smartregister.fhircore.quest.ui.report.measure.components.PatientSelector
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportTypeData
import org.smartregister.fhircore.quest.ui.report.measure.models.ReportRangeSelectionData

const val SHOW_FIXED_RANGE_TEST_TAG = "SHOW_FIXED_RANGE_TEST_TAG"
const val SHOW_PROGRESS_INDICATOR_TAG = "SHOW_PROGRESS_INDICATOR_TAG"
const val TEST_MONTH_CLICK_TAG = "TEST_MONTH_CLICK_TAG"
const val SHOW_DATE_PICKER_FORM_TAG = "SHOW_DATE_PICKER_FORM_TAG"
const val PLEASE_WAIT_TEST_TAG = "PLEASE_WAIT_TEST_TAG"
const val SCREEN_TITLE = "SCREEN_TITLE"
const val YEAR_TEST_TAG = "YEAR_TEST_TAG"
const val MONTH_TEST_TAG = "MONTH_TEST_TAG"

@Composable
fun ReportTypeSelectorScreen(
  reportId: String,
  screenTitle: String,
  navController: NavController,
  measureReportViewModel: MeasureReportViewModel,
) {
  val context = LocalContext.current
  val uiState = measureReportViewModel.reportTypeSelectorUiState.value
  if (measureReportViewModel.showFixedRangeSelection(reportId)) {
    FixedMonthYearListing(
      screenTitle = screenTitle,
      onMonthSelected = { date ->
        measureReportViewModel.onEvent(
          MeasureReportEvent.GenerateReport(navController, context),
          date
        )
      },
      onBackPress = {
        // Reset UI state
        measureReportViewModel.resetState()
        navController.popBackStack(
          route = MeasureReportNavigationScreen.MeasureReportList.route,
          inclusive = false
        )
      },
      showProgressIndicator = uiState.showProgressIndicator,
      reportGenerationRange = measureReportViewModel.getReportGenerationRange(reportId),
    )
  } else {
    ReportTypeSelectorPage(
      startDate = uiState.startDate.ifEmpty { stringResource(id = R.string.start_date) },
      endDate = uiState.endDate.ifEmpty { stringResource(id = R.string.end_date) },
      patientName = uiState.patientViewData?.name,
      generateReport =
        uiState.startDate.isNotEmpty() &&
          uiState.endDate.isNotEmpty() &&
          (uiState.patientViewData != null ||
            measureReportViewModel.reportTypeState.value ==
              MeasureReport.MeasureReportType.SUMMARY),
      onGenerateReportClicked = {
        measureReportViewModel.onEvent(MeasureReportEvent.GenerateReport(navController, context))
      },
      reportTypeState = measureReportViewModel.reportTypeState,
      onReportTypeSelected = {
        measureReportViewModel.onEvent(MeasureReportEvent.OnReportTypeChanged(it, navController))
      },
      showProgressIndicator = uiState.showProgressIndicator,
      screenTitle = screenTitle,
      dateRange = measureReportViewModel.dateRange,
      onDateRangeSelected = { newDateRange ->
        measureReportViewModel.onEvent(MeasureReportEvent.OnDateRangeSelected(newDateRange))
      },
      onBackPress = {
        // Reset UI state
        measureReportViewModel.resetState()
        navController.popBackStack(
          route = MeasureReportNavigationScreen.MeasureReportList.route,
          inclusive = false
        )
      }
    )
  }
}

@Composable
fun ReportTypeSelectorPage(
  screenTitle: String,
  startDate: String,
  endDate: String,
  dateRange: MutableState<Pair<Long, Long>>,
  onDateRangeSelected: (Pair<Long, Long>) -> Unit,
  patientName: String?,
  reportTypeState: MutableState<MeasureReport.MeasureReportType>,
  onReportTypeSelected: (MeasureReport.MeasureReportType) -> Unit,
  generateReport: Boolean,
  onGenerateReportClicked: () -> Unit,
  onBackPress: () -> Unit,
  modifier: Modifier = Modifier,
  showProgressIndicator: Boolean = false
) {

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = screenTitle,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = modifier.testTag(SCREEN_TITLE)
          )
        },
        navigationIcon = {
          IconButton(onClick = onBackPress) { Icon(Icons.Filled.ArrowBack, null) }
        },
        contentColor = Color.White,
        backgroundColor = MaterialTheme.colors.primary
      )
    }
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      Column(modifier = modifier.fillMaxSize().testTag(SHOW_DATE_PICKER_FORM_TAG)) {
        Box(modifier = modifier.padding(16.dp).fillMaxSize(), contentAlignment = Alignment.Center) {
          if (showProgressIndicator) {
            Column(
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              CircularProgressIndicator(
                modifier = modifier.size(40.dp).testTag(SHOW_PROGRESS_INDICATOR_TAG),
                strokeWidth = 2.dp
              )
              Text(
                text = stringResource(R.string.please_wait),
                textAlign = TextAlign.Center,
                modifier = modifier.padding(vertical = 16.dp).testTag(PLEASE_WAIT_TEST_TAG)
              )
            }
          } else {
            Column {
              DateSelectionBox(
                startDate = startDate,
                endDate = endDate,
                dateRange = dateRange,
                onDateRangeSelected = onDateRangeSelected
              )
              Spacer(modifier = modifier.size(28.dp))
              PatientSelectionBox(
                radioOptions =
                  listOf(
                    MeasureReportTypeData(
                      textResource = R.string.all,
                      measureReportType = MeasureReport.MeasureReportType.SUMMARY
                    ),
                    MeasureReportTypeData(
                      textResource = R.string.individual,
                      measureReportType = MeasureReport.MeasureReportType.INDIVIDUAL
                    )
                  ),
                patientName = patientName,
                reportTypeState = reportTypeState,
                onReportTypeSelected = onReportTypeSelected
              )
              Column(
                modifier = modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom
              ) {
                Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
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
}

@Composable
fun FixedMonthYearListing(
  screenTitle: String,
  onMonthSelected: (date: Date?) -> Unit,
  onBackPress: () -> Unit,
  modifier: Modifier = Modifier,
  showProgressIndicator: Boolean = false,
  reportGenerationRange: Map<String, List<ReportRangeSelectionData>>,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = screenTitle,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = modifier.testTag(SCREEN_TITLE)
          )
        },
        navigationIcon = {
          IconButton(onClick = onBackPress) { Icon(Icons.Filled.ArrowBack, null) }
        },
        contentColor = Color.White,
        backgroundColor = MaterialTheme.colors.primary
      )
    }
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding).testTag(SHOW_FIXED_RANGE_TEST_TAG)) {
      Column(modifier = modifier.fillMaxSize()) {
        if (showProgressIndicator) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              CircularProgressIndicator(
                modifier = modifier.size(40.dp).testTag(SHOW_PROGRESS_INDICATOR_TAG),
                strokeWidth = 2.dp
              )
              Text(
                text = stringResource(R.string.please_wait),
                textAlign = TextAlign.Center,
                modifier = modifier.padding(vertical = 16.dp).testTag(PLEASE_WAIT_TEST_TAG)
              )
            }
          }
        } else {
          Box(
            modifier = modifier.fillMaxSize(),
          ) { LazyMonthList(reportRangeList = reportGenerationRange) { onMonthSelected(it.date) } }
        }
      }
    }
  }
}

/**
 * LazyColumn displaying a List<ReportRangeSelectionData> with clickable items, utilizing a
 * StickyHeader for displaying the years.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyMonthList(
  modifier: Modifier = Modifier,
  reportRangeList: Map<String, List<ReportRangeSelectionData>>,
  selectedMonth: (ReportRangeSelectionData) -> Unit
) {
  LazyColumn {
    reportRangeList.forEach { (year, monthList) ->
      stickyHeader {
        Row(
          modifier = modifier.fillMaxWidth().background(color = SearchHeaderColor).padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = year,
            fontSize = 14.sp,
            color = DefaultColor,
            modifier = Modifier.testTag(YEAR_TEST_TAG)
          )
          Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = DefaultColor.copy(alpha = 0.9f).copy(alpha = 0.7f),
          )
        }
      }
      itemsIndexed(
        items = monthList,
        itemContent = { index, item ->
          ListItem(data = item, selectedMonth = selectedMonth)
          if (index < monthList.lastIndex) Divider(color = DividerColor, thickness = 0.8.dp)
        }
      )
    }
  }
}

@Composable
private fun ListItem(
  modifier: Modifier = Modifier,
  data: ReportRangeSelectionData,
  selectedMonth: (ReportRangeSelectionData) -> Unit
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clickable { selectedMonth(data) }
        .padding(16.dp)
        .testTag(TEST_MONTH_CLICK_TAG),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = data.month,
      fontSize = 16.sp,
      style = MaterialTheme.typography.h5,
      modifier = Modifier.testTag(MONTH_TEST_TAG)
    )
    Icon(
      imageVector = Icons.Filled.KeyboardArrowRight,
      contentDescription = null,
      tint = DefaultColor.copy(alpha = 0.7f)
    )
  }
}

@Composable
fun PatientSelectionBox(
  radioOptions: List<MeasureReportTypeData>,
  patientName: String?,
  reportTypeState: MutableState<MeasureReport.MeasureReportType>,
  onReportTypeSelected: (MeasureReport.MeasureReportType) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.wrapContentWidth(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.Start
  ) {
    Text(
      text = stringResource(id = R.string.patient),
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold
    )
    radioOptions.forEach { reportTypeData ->
      Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
          selected = reportTypeState.value == reportTypeData.measureReportType,
          onClick = {
            reportTypeState.value = reportTypeData.measureReportType
            onReportTypeSelected(reportTypeState.value)
          }
        )
        Text(
          text = stringResource(id = reportTypeData.textResource),
          fontSize = 16.sp,
          modifier =
            modifier.clickable {
              reportTypeState.value = reportTypeData.measureReportType
              onReportTypeSelected(reportTypeState.value)
            }
        )
      }
      Spacer(modifier = modifier.size(4.dp))
    }
    if (reportTypeState.value == MeasureReport.MeasureReportType.INDIVIDUAL &&
        !patientName.isNullOrEmpty()
    ) {
      Row(modifier = modifier.padding(start = 24.dp)) {
        Spacer(modifier = modifier.size(8.dp))
        PatientSelector(
          patientName = patientName,
          onChangePatient = { onReportTypeSelected(reportTypeState.value) },
        )
      }
    }
  }
}

@Composable
fun GenerateReportButton(
  generateReportEnabled: Boolean,
  onGenerateReportClicked: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column {
    Button(
      enabled = generateReportEnabled,
      onClick = onGenerateReportClicked,
      modifier = modifier.fillMaxWidth()
    ) {
      Text(
        color = Color.White,
        text = stringResource(id = R.string.generate_report),
        modifier = modifier.padding(8.dp)
      )
    }
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PatientSelectionAllPreview() {
  val reportTypeState = remember { mutableStateOf(MeasureReport.MeasureReportType.SUMMARY) }
  PatientSelectionBox(
    radioOptions =
      listOf(
        MeasureReportTypeData(
          textResource = R.string.all,
          measureReportType = MeasureReport.MeasureReportType.SUMMARY,
        ),
        MeasureReportTypeData(
          textResource = R.string.individual,
          measureReportType = MeasureReport.MeasureReportType.INDIVIDUAL,
        )
      ),
    reportTypeState = reportTypeState,
    onReportTypeSelected = {},
    patientName = null
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PatientSelectionIndividualPreview() {
  val reportTypeState = remember { mutableStateOf(MeasureReport.MeasureReportType.INDIVIDUAL) }
  PatientSelectionBox(
    radioOptions =
      listOf(
        MeasureReportTypeData(
          textResource = R.string.all,
          measureReportType = MeasureReport.MeasureReportType.SUMMARY,
        ),
        MeasureReportTypeData(
          textResource = R.string.individual,
          measureReportType = MeasureReport.MeasureReportType.INDIVIDUAL,
        )
      ),
    reportTypeState = reportTypeState,
    onReportTypeSelected = {},
    patientName = "John Jared"
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun FixedRangeListPreview() {
  val ranges = HashMap<String, List<ReportRangeSelectionData>>()
  val months = mutableListOf<ReportRangeSelectionData>()
  val range = ReportRangeSelectionData("March", "2022", "2021-12-12".parseDate(SDF_YYYY_MM_DD)!!)
  months.add(range)
  months.add(range)
  months.add(range)
  months.add(range)
  ranges["2022"] = months
  ranges["2021"] = months
  ranges["2020"] = months
  ranges["2019"] = months
  FixedMonthYearListing(
    screenTitle = "First ANC",
    onMonthSelected = {},
    onBackPress = {},
    showProgressIndicator = false,
    reportGenerationRange = ranges
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun ReportFilterPreview() {
  val reportTypeState = remember { mutableStateOf(MeasureReport.MeasureReportType.SUMMARY) }
  val dateRange = remember {
    mutableStateOf(Pair(Calendar.getInstance().timeInMillis, Calendar.getInstance().timeInMillis))
  }
  ReportTypeSelectorPage(
    screenTitle = "First ANC",
    startDate = "StartDate",
    endDate = "EndDate",
    dateRange = dateRange,
    onDateRangeSelected = {},
    patientName = "John Doe",
    generateReport = true,
    onGenerateReportClicked = {},
    onReportTypeSelected = {},
    reportTypeState = reportTypeState,
    showProgressIndicator = false,
    onBackPress = {}
  )
}
