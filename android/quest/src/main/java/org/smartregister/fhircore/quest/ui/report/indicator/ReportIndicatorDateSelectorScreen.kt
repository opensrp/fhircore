/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.report.indicator

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.util.Calendar
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.SearchHeaderColor
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.firstDayOfMonth
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.lastDayOfMonth
import org.smartregister.fhircore.quest.navigation.ReportIndicatorNavigation
import org.smartregister.fhircore.quest.ui.report.models.ReportRangeSelectionData

@Composable
fun ReportIndicatorDateSelectorScreen(
  reportId: String,
  reportIndicatorViewModel: ReportIndicatorViewModel,
  navController: NavController,
  mainNavController: NavController,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  LaunchedEffect(Unit) { reportIndicatorViewModel.loadDateRangeFromEncounters() }

  val reportPeriodRange = reportIndicatorViewModel.reportPeriodRange.value
  val isLoading = reportIndicatorViewModel.isLoading.value

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(text = stringResource(R.string.select_period)) },
        navigationIcon = {
          IconButton(onClick = { mainNavController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
          }
        },
        contentColor = Color.White,
        backgroundColor = MaterialTheme.colors.primary,
      )
    },
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding).fillMaxSize()) {
      if (isLoading) {
        Column(
          modifier = modifier.fillMaxSize(),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          CircularProgressIndicator(
            modifier = modifier.size(40.dp),
            strokeWidth = 2.dp,
          )
          Text(
            text = stringResource(R.string.please_wait),
            textAlign = TextAlign.Center,
            modifier = modifier.padding(vertical = 16.dp),
          )
        }
      } else if (reportPeriodRange.isEmpty()) {
        Box(
          modifier = modifier.fillMaxSize(),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text =
              stringResource(org.smartregister.fhircore.quest.R.string.no_encounter_data_available),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
          )
        }
      } else {
        MonthYearList(
          reportPeriodRange = reportPeriodRange,
          onMonthSelected = { selectedDate ->
            val startDate = selectedDate.date.firstDayOfMonth().formatDate(SDF_YYYY_MM_DD)
            val endDate =
              (selectedDate.endDate ?: selectedDate.date)
                .lastDayOfMonth()
                .formatDate(SDF_YYYY_MM_DD)
            val report = context.getString(org.smartregister.fhircore.quest.R.string.reports_suffix)
            val periodLabel =
              if (selectedDate.endDate != null) {
                "${selectedDate.year} $report"
              } else {
                "${selectedDate.month} ${selectedDate.year} $report"
              }

            navController.navigate(
              ReportIndicatorNavigation.ReportIndicatorList.route(
                reportId = reportId,
                startDate = startDate,
                endDate = endDate,
                periodLabel = periodLabel,
              ),
            )
          },
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthYearList(
  reportPeriodRange: Map<String, List<ReportRangeSelectionData>>,
  onMonthSelected: (ReportRangeSelectionData) -> Unit,
  modifier: Modifier = Modifier,
) {
  val expandedYears = remember { mutableStateMapOf<String, Boolean>() }
  val context = LocalContext.current
  LazyColumn {
    reportPeriodRange.forEach { (year, monthList) ->
      val isExpanded = expandedYears[year] ?: true

      stickyHeader {
        Row(
          modifier =
            modifier
              .fillMaxWidth()
              .background(color = SearchHeaderColor)
              .clickable { expandedYears[year] = !isExpanded }
              .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = year,
            fontSize = 16.sp,
            color = DefaultColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
          )
          Icon(
            imageVector =
              if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = DefaultColor,
          )
        }
      }

      if (isExpanded) {
        item {
          FullYearListItem(
            onYearSelected = {
              val calendar = Calendar.getInstance()
              calendar.set(Calendar.YEAR, year.toInt())
              calendar.set(Calendar.MONTH, Calendar.JANUARY)
              calendar.set(Calendar.DAY_OF_MONTH, 1)
              val startDate = calendar.time

              calendar.set(Calendar.MONTH, Calendar.DECEMBER)
              calendar.set(Calendar.DAY_OF_MONTH, 31)
              val endDate = calendar.time

              onMonthSelected(
                ReportRangeSelectionData(
                  context.getString(R.string.full_year),
                  year,
                  startDate,
                  endDate,
                ),
              )
            },
          )
          Divider(color = DividerColor, thickness = 0.8.dp)
        }

        itemsIndexed(
          items = monthList,
          itemContent = { index, item ->
            MonthListItem(data = item, onMonthSelected = onMonthSelected)
            if (index < monthList.lastIndex) Divider(color = DividerColor, thickness = 0.8.dp)
          },
        )
      }
    }
  }
}

@Composable
private fun FullYearListItem(
  onYearSelected: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clickable { onYearSelected() }
        .padding(16.dp)
        .background(color = SearchHeaderColor.copy(alpha = 0.3f)),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f),
    ) {
      Icon(
        imageVector = Icons.Filled.CalendarMonth,
        contentDescription = null,
        tint = MaterialTheme.colors.primary,
        modifier = Modifier.padding(end = 12.dp),
      )
      Text(
        text = stringResource(R.string.full_year),
        fontSize = 16.sp,
        style = MaterialTheme.typography.h5,
        fontWeight = FontWeight.SemiBold,
      )
    }
    Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = null,
      tint = DefaultColor.copy(alpha = 0.7f),
    )
  }
}

@Composable
private fun MonthListItem(
  data: ReportRangeSelectionData,
  onMonthSelected: (ReportRangeSelectionData) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().clickable { onMonthSelected(data) }.padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = data.month,
      fontSize = 16.sp,
      style = MaterialTheme.typography.h5,
    )
    Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = null,
      tint = DefaultColor.copy(alpha = 0.7f),
    )
  }
}
