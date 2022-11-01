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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.util.Date
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.SearchHeaderColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.engine.util.extension.getYyyMmDd
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.navigation.MeasureReportNavigationScreen
import org.smartregister.fhircore.quest.ui.report.measure.MeasureReportEvent
import org.smartregister.fhircore.quest.ui.report.measure.MeasureReportViewModel
import org.smartregister.fhircore.quest.ui.report.measure.models.ReportRangeSelectionData

@Composable
fun ReportTypeSelectorScreen(
  screenTitle: String,
  navController: NavController,
  measureReportViewModel: MeasureReportViewModel,
) {
  val context = LocalContext.current
  val uiState = measureReportViewModel.reportTypeSelectorUiState.value

  ReportTypeSelectorPage(
    screenTitle = screenTitle,
    onGenerateReportClicked = { date ->
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
    reportGenerationRange = measureReportViewModel.getReportGenerationRange()
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun ReportTypeSelectorPage(
  screenTitle: String,
  onGenerateReportClicked: (date: Date) -> Unit,
  onBackPress: () -> Unit,
  modifier: Modifier = Modifier,
  showProgressIndicator: Boolean = false,
  reportGenerationRange: Map<String, List<ReportRangeSelectionData>>
) {

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(text = screenTitle, overflow = TextOverflow.Ellipsis, maxLines = 1) },
        navigationIcon = {
          IconButton(onClick = onBackPress) { Icon(Icons.Filled.ArrowBack, null) }
        },
        contentColor = Color.White,
        backgroundColor = MaterialTheme.colors.primary
      )
    }
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      Column(modifier = modifier.fillMaxSize()) {
        if (showProgressIndicator) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              CircularProgressIndicator(modifier = modifier.size(40.dp), strokeWidth = 2.dp)
              Text(
                text = stringResource(R.string.please_wait),
                textAlign = TextAlign.Center,
                modifier = modifier.padding(vertical = 16.dp)
              )
            }
          }
        } else {
          Box(
            modifier = modifier.fillMaxSize(),
          ) { LazyMonthList(reportGenerationRange) { onGenerateReportClicked(it.date) } }
        }
      }
    }
  }
}

/**
 * LazyColumn displaying a List<ReportRangeSelectionData> with clickable items, utilizing a
 * StickyHeader A RecyclerView equivalent with rich UI populating it's items from a dataset with a
 * sticky header
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun LazyMonthList(
  reportRangeList: Map<String, List<ReportRangeSelectionData>>,
  selectedMonth: (ReportRangeSelectionData) -> Unit
) {

  LazyColumn {
    reportRangeList.forEach { (year, monthList) ->
      stickyHeader {
        Row(Modifier.fillMaxWidth().background(color = SearchHeaderColor)) {
          Text(
            text = year,
            fontSize = 14.sp,
            color = DefaultColor.copy(alpha = 0.7f),
            modifier = Modifier.padding(8.dp).weight(0.85f)
          )
          Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = DefaultColor.copy(alpha = 0.7f),
            modifier = Modifier.padding(8.dp).weight(0.15f).align(Alignment.CenterVertically)
          )
        }
      }

      items(items = monthList, itemContent = { ListItem(data = it, selectedMonth = selectedMonth) })
    }
  }
}

/** Composable function to represent a list item */
@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun ListItem(data: ReportRangeSelectionData, selectedMonth: (ReportRangeSelectionData) -> Unit) {
  Row(Modifier.fillMaxWidth().clickable { selectedMonth(data) }) {
    Text(
      data.month,
      fontSize = 16.sp,
      style = MaterialTheme.typography.h5,
      modifier = Modifier.padding(12.dp).weight(0.85f)
    )
    Icon(
      Icons.Filled.KeyboardArrowRight,
      contentDescription = null,
      tint = DefaultColor.copy(alpha = 0.7f),
      modifier = Modifier.padding(8.dp).weight(0.15f).align(Alignment.CenterVertically)
    )
  }
  Divider(color = DividerColor, thickness = 0.5.dp)
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun ReportFilterPreview() {
  val ranges = HashMap<String, List<ReportRangeSelectionData>>()
  val months = mutableListOf<ReportRangeSelectionData>()
  val range =
    ReportRangeSelectionData(
      "March",
      "2022",
      "2021-12-12".getYyyMmDd(MeasureReportViewModel.MEASURE_REPORT_DATE_FORMAT)!!
    )
  months.add(range)
  months.add(range)
  months.add(range)
  months.add(range)
  ranges["2022"] = months
  ranges["2021"] = months
  ranges["2020"] = months
  ranges["2019"] = months
  ReportTypeSelectorPage(
    screenTitle = "First ANC",
    onGenerateReportClicked = {},
    onBackPress = {},
    showProgressIndicator = false,
    reportGenerationRange = ranges
  )
}
