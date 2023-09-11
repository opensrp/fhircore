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

package org.smartregister.fhircore.quest.ui.report.other

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.util.Pair
import androidx.navigation.NavController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.view.TabViewProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.util.extension.SDF_D_MMM_YYYY_WITH_COMA
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.profile.DROPDOWN_MENU_TEST_TAG
import org.smartregister.fhircore.quest.ui.shared.components.TabView
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class, ExperimentalPagerApi::class)
@Composable
fun OtherReportScreen(
  navController: NavController,
  modifier: Modifier = Modifier,
  onEvent: (OtherReportEvent) -> Unit,
  otherReportUiState: OtherReportUiState,
  initialDateRange: Pair<Long?, Long?>,
) {
  val currentSelectedDateState = remember { mutableStateOf(initialDateRange) }
  val state = rememberDateRangePickerState(
    initialSelectedStartDateMillis = currentSelectedDateState.value.first,
    initialSelectedEndDateMillis = currentSelectedDateState.value.second,
  )
  val bottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
  val coroutineScope = rememberCoroutineScope()

  val selectedPageState = remember {
    mutableStateOf(otherReportUiState.otherReportConfiguration?.tabBar?.selectedTabIndex ?: 0)
  }

  ModalBottomSheetLayout(
    sheetState = bottomSheetState,
    sheetContent = {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(600.dp)
          .background(Color.White)
      ) {
        DateRangePickerUi(
          state,
          onDateRangeSelected = {
            coroutineScope.launch {
              bottomSheetState.hide()
            }

            if(currentSelectedDateState.value.first != state.selectedStartDateMillis ||
              currentSelectedDateState.value.second != state.selectedEndDateMillis) {
              currentSelectedDateState.value =
                Pair(state.selectedStartDateMillis, state.selectedEndDateMillis)

              onEvent(
                OtherReportEvent.OnDateRangeSelected(currentSelectedDateState.value)
              )
            }
          }
        )
      }
    }
  ) {
    Scaffold(
      topBar = {
        Column(modifier = modifier
          .background(MaterialTheme.colors.primary)
          .fillMaxWidth()) {
          TopAppBar(
            title = { Text(text = stringResource(R.string.reports)) },
            navigationIcon = {
              IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, null)
              }
            },
            contentColor = Color.White,
            backgroundColor = MaterialTheme.colors.primary,
            elevation = 0.dp,
            actions = {
              if(otherReportUiState.otherReportConfiguration?.showDateFilter == true) {
                IconButton(
                  onClick = {
                    state.setSelection(
                      startDateMillis = currentSelectedDateState.value.first,
                      endDateMillis = currentSelectedDateState.value.second
                    )
                    coroutineScope.launch {
                      bottomSheetState.show()
                    }
                  },
                  modifier = modifier.testTag(DROPDOWN_MENU_TEST_TAG),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = Color.White
                  )
                }
              }
            }
          )
          if(currentSelectedDateState.value.first != null && currentSelectedDateState.value.second != null) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceAround,
            ) {
              currentSelectedDateState.value.first?.let { Date(it).formatDate(SDF_D_MMM_YYYY_WITH_COMA) }?.let {
                  Text(text = it, color = Color.White)
                }

                Text(text = " ~ ", color = Color.White)

              currentSelectedDateState.value.second?.let { Date(it).formatDate(SDF_D_MMM_YYYY_WITH_COMA) }?.let {
                  Text(text = it, color = Color.White)
                }

                IconButton(
                  onClick = {
                    currentSelectedDateState.value = Pair(null, null)
                    onEvent(
                      OtherReportEvent.OnDateRangeSelected(currentSelectedDateState.value)
                    )
                  }
                ) {
                  Icon(imageVector = Icons.Default.ClearAll, tint = Color.White, contentDescription = "Clear")
                }
            }
          }
        }
      },
    ) { innerPadding ->
      Box(modifier = modifier
        .background(Color.White)
        .fillMaxSize()
        .padding(innerPadding)) {
        if (otherReportUiState.showDataLoadProgressIndicator) {
          CircularProgressIndicator(
            modifier = modifier
              .align(Alignment.Center)
              .size(24.dp),
            strokeWidth = 1.8.dp,
            color = MaterialTheme.colors.primary,
          )
        }

        if (otherReportUiState.otherReportConfiguration?.tabBar != null) {
          TabView(
            modifier = modifier,
            viewProperties = otherReportUiState.otherReportConfiguration.tabBar as TabViewProperties,
            resourceData = otherReportUiState.resourceData ?: ResourceData(
              "",
              ResourceType.MeasureReport,
              emptyMap()
            ),
            navController = navController,
            selectedTabIndex = selectedPageState.value,
            tabChangedEvent = { index ->
              selectedPageState.value = index
            }
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerUi(
  state: DateRangePickerState,
  onDateRangeSelected: () -> Unit,
) {
  DateRangePicker(state,
    modifier = Modifier,
    dateFormatter = DatePickerFormatter("yy MM dd", "yy MM dd", "yy MM dd"),
    dateValidator = { timeInMillis ->
      timeInMillis < Calendar.getInstance().timeInMillis
    },
    title = {
      Text(text = "Select date range to filter the report", modifier = Modifier
        .padding(16.dp))
    },
    headline = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(Modifier.weight(1f)) {
          (if(state.selectedStartDateMillis != null)
            state.selectedStartDateMillis?.let { Date(it).formatDate(SDF_D_MMM_YYYY_WITH_COMA) }
          else "Start Date")?.let {
            Text(text = it, modifier = Modifier.align(Alignment.Center))
          }
        }
        Box(Modifier.weight(0.2f)) {
          Text(text = " ~ ")
        }
        Box(Modifier.weight(1f)) {
          (if(state.selectedEndDateMillis != null)
            state.selectedEndDateMillis?.let { Date(it).formatDate(SDF_D_MMM_YYYY_WITH_COMA) }
          else "End Date")?.let {
            Text(text = it, modifier = Modifier.align(Alignment.Center))
          }
        }
        Box(Modifier.weight(0.2f)) {
          IconButton(
            onClick = onDateRangeSelected,
            enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null
          ) {
            Icon(imageVector = Icons.Default.DoneAll, contentDescription = "Done")
          }
        }
      }
    },
    showModeToggle = true,
    colors = DatePickerDefaults.colors(
      containerColor = Color.Blue,
      titleContentColor = Color.Black,
      headlineContentColor = Color.Black,
      weekdayContentColor = Color.Black,
      subheadContentColor = Color.Black,
      yearContentColor = Color.Green,
      currentYearContentColor = Color.Red,
      selectedYearContainerColor = Color.Red,
      disabledDayContentColor = Color.Gray,
      todayDateBorderColor = Color.Blue,
      dayInSelectionRangeContainerColor = Color.LightGray,
      dayInSelectionRangeContentColor = Color.White,
      selectedDayContainerColor = Color.Black
    )
  )
}
