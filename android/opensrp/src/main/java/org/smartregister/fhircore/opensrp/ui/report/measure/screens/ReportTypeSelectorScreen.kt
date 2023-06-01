/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.opensrp.ui.report.measure.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.util.Pair
import androidx.navigation.NavController
import java.util.Calendar
import java.util.Date
import org.hl7.fhir.r4.model.MeasureReport
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportType
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.SearchHeaderColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.parseDate
import org.smartregister.fhircore.opensrp.R
import org.smartregister.fhircore.opensrp.navigation.MeasureReportNavigationScreen
import org.smartregister.fhircore.opensrp.ui.report.measure.MeasureReportEvent
import org.smartregister.fhircore.opensrp.ui.report.measure.MeasureReportViewModel
import org.smartregister.fhircore.opensrp.ui.report.measure.ReportTypeSelectorUiState
import org.smartregister.fhircore.opensrp.ui.report.measure.components.DateSelectionBox
import org.smartregister.fhircore.opensrp.ui.report.measure.components.SubjectSelector
import org.smartregister.fhircore.opensrp.ui.report.measure.models.MeasureReportTypeData
import org.smartregister.fhircore.opensrp.ui.report.measure.models.ReportRangeSelectionData
import org.smartregister.fhircore.opensrp.ui.shared.models.MeasureReportSubjectViewData
import org.smartregister.fhircore.opensrp.util.extensions.conditional

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
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val uiState = measureReportViewModel.reportTypeSelectorUiState.value

  ReportFilterSelector(
    screenTitle = screenTitle,
    reportTypeState = measureReportViewModel.reportTypeState,
    showFixedRangeSelection = measureReportViewModel.showFixedRangeSelection(reportId),
    showSubjectSelection = measureReportViewModel.showSubjectSelection(reportId),
    uiState = uiState,
    dateRange = measureReportViewModel.dateRange,
    reportPeriodRange = measureReportViewModel.getReportGenerationRange(reportId),
    modifier = modifier,
    onBackPressed = {
      // Reset UI state
      measureReportViewModel.resetState()
      navController.popBackStack(
        route = MeasureReportNavigationScreen.MeasureReportList.route,
        inclusive = false
      )
    },
    onGenerateReport = { date ->
      measureReportViewModel.onEvent(
        MeasureReportEvent.GenerateReport(navController, context),
        date
      )
    },
    onDateRangeSelected = { newDateRange ->
      measureReportViewModel.onEvent(MeasureReportEvent.OnDateRangeSelected(newDateRange))
    },
    onReportTypeSelected = {
      measureReportViewModel.onEvent(MeasureReportEvent.OnReportTypeChanged(it, navController))
    },
    onSubjectRemoved = { measureReportViewModel.onEvent(MeasureReportEvent.OnSubjectRemoved(it)) }
  )
}

@Composable
fun ReportFilterSelector(
  screenTitle: String,
  reportTypeState: MutableState<MeasureReportType>,
  showFixedRangeSelection: Boolean,
  showSubjectSelection: Boolean,
  uiState: ReportTypeSelectorUiState,
  dateRange: MutableState<Pair<Long, Long>>?,
  reportPeriodRange: Map<String, List<ReportRangeSelectionData>>,
  modifier: Modifier = Modifier,
  onBackPressed: () -> Unit,
  onGenerateReport: (date: Date?) -> Unit,
  onDateRangeSelected: (Pair<Long, Long>) -> Unit,
  onReportTypeSelected: (MeasureReportType) -> Unit,
  onSubjectRemoved: (MeasureReportSubjectViewData) -> Unit
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
          IconButton(onClick = onBackPressed) { Icon(Icons.Filled.ArrowBack, null) }
        },
        contentColor = Color.White,
        backgroundColor = MaterialTheme.colors.primary
      )
    }
  ) { innerPadding ->
    Row(
      modifier =
        modifier.conditional(
          uiState.showProgressIndicator,
          { modifier.alpha(0f) },
          { modifier.alpha(1f) }
        )
    ) {
      SubjectSelectionBox(
        radioOptions =
          listOf(
            MeasureReportTypeData(
              textResource = R.string.all,
              measureReportType = MeasureReportType.SUMMARY
            ),
            MeasureReportTypeData(
              textResource = R.string.individual,
              measureReportType = MeasureReportType.INDIVIDUAL
            )
          ),
        subjects = uiState.subjectViewData,
        reportTypeState = reportTypeState,
        onReportTypeSelected = onReportTypeSelected,
        onSubjectRemoved = onSubjectRemoved
      )
    }

    Divider(modifier = modifier.size(4.dp))

    Row(
      modifier =
        modifier.conditional(
          showSubjectSelection,
          { modifier.padding(top = 100.dp) },
          { modifier.fillMaxSize() }
        )
    ) {
      if (showFixedRangeSelection) {
        FixedMonthYearListing(
          onMonthSelected = onGenerateReport,
          showProgressIndicator = uiState.showProgressIndicator,
          reportGenerationRange = reportPeriodRange,
          innerPadding = innerPadding
        )
      } else {
        DateRangeSelector(
          startDate = uiState.startDate.ifEmpty { stringResource(id = R.string.start_date) },
          endDate = uiState.endDate.ifEmpty { stringResource(id = R.string.end_date) },
          generateReport =
            uiState.startDate.isNotEmpty() &&
              uiState.endDate.isNotEmpty() &&
              (uiState.subjectViewData != null ||
                reportTypeState.value == MeasureReportType.SUMMARY),
          onGenerateReportClicked = { onGenerateReport.invoke(null) },
          showProgressIndicator = uiState.showProgressIndicator,
          dateRange = dateRange!!,
          onDateRangeSelected = onDateRangeSelected,
          innerPadding = innerPadding
        )
      }
    }
  }
}

@Composable
fun DateRangeSelector(
  startDate: String,
  endDate: String,
  dateRange: MutableState<Pair<Long, Long>>,
  onDateRangeSelected: (Pair<Long, Long>) -> Unit,
  generateReport: Boolean,
  onGenerateReportClicked: () -> Unit,
  modifier: Modifier = Modifier,
  showProgressIndicator: Boolean = false,
  innerPadding: PaddingValues
) {
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
          DateSelectionBox(
            startDate = startDate,
            endDate = endDate,
            dateRange = dateRange,
            onDateRangeSelected = onDateRangeSelected
          )
          Column(modifier = modifier.fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
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

@Composable
fun FixedMonthYearListing(
  onMonthSelected: (date: Date?) -> Unit,
  modifier: Modifier = Modifier,
  showProgressIndicator: Boolean = false,
  reportGenerationRange: Map<String, List<ReportRangeSelectionData>>,
  innerPadding: PaddingValues,
) {
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
fun SubjectSelectionBox(
  radioOptions: List<MeasureReportTypeData>,
  subjects: Set<MeasureReportSubjectViewData>,
  reportTypeState: MutableState<MeasureReport.MeasureReportType>,
  onReportTypeSelected: (MeasureReport.MeasureReportType) -> Unit,
  onSubjectRemoved: (MeasureReportSubjectViewData) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = stringResource(id = R.string.subject),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
      )
      radioOptions.forEach { reportTypeData ->
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
    if (reportTypeState.value == MeasureReport.MeasureReportType.INDIVIDUAL && subjects.isNotEmpty()
    ) {
      Row(modifier = modifier.padding(start = 24.dp)) {
        Spacer(modifier = modifier.size(8.dp))
        SubjectSelector(
          subjects = subjects,
          onAddSubject = { onReportTypeSelected(reportTypeState.value) },
          onRemoveSubject = { onSubjectRemoved(it) }
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

@PreviewWithBackgroundExcludeGenerated
@Composable
fun SubjectSelectionAllPreview() {
  val reportTypeState = remember { mutableStateOf(MeasureReport.MeasureReportType.SUMMARY) }
  SubjectSelectionBox(
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
    onSubjectRemoved = {},
    subjects = setOf()
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun SubjectSelectionIndividualPreview() {
  val reportTypeState = remember { mutableStateOf(MeasureReport.MeasureReportType.INDIVIDUAL) }
  SubjectSelectionBox(
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
    onSubjectRemoved = {},
    subjects =
      setOf(
        MeasureReportSubjectViewData(ResourceType.Patient, "1", "John Jared"),
        MeasureReportSubjectViewData(ResourceType.Patient, "2", "Jane Doe"),
        MeasureReportSubjectViewData(ResourceType.Patient, "3", "John Doe"),
        MeasureReportSubjectViewData(ResourceType.Patient, "4", "Lorem Ipsm"),
        MeasureReportSubjectViewData(ResourceType.Patient, "5", "Mary Magdalene")
      )
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun FixedRangeListPreview() {
  val reportType = remember { mutableStateOf(MeasureReportType.SUMMARY) }
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
  ReportFilterSelector(
    screenTitle = "ANC Report",
    reportTypeState = reportType,
    showFixedRangeSelection = true,
    showSubjectSelection = true,
    uiState = ReportTypeSelectorUiState(),
    dateRange = null,
    reportPeriodRange = ranges,
    onBackPressed = {},
    onGenerateReport = {},
    onDateRangeSelected = {},
    onReportTypeSelected = {},
    onSubjectRemoved = {}
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun ReportFilterPreview() {
  val dateRange = remember {
    mutableStateOf(Pair(Calendar.getInstance().timeInMillis, Calendar.getInstance().timeInMillis))
  }
  val reportType = remember { mutableStateOf(MeasureReportType.SUMMARY) }
  ReportFilterSelector(
    screenTitle = "ANC Report",
    reportTypeState = reportType,
    showFixedRangeSelection = false,
    showSubjectSelection = true,
    uiState = ReportTypeSelectorUiState(),
    dateRange = dateRange,
    reportPeriodRange = mapOf(),
    onBackPressed = {},
    onGenerateReport = {},
    onDateRangeSelected = {},
    onReportTypeSelected = {},
    onSubjectRemoved = {}
  )
}
