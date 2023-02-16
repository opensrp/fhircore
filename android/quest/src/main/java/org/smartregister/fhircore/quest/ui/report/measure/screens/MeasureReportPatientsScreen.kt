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

package org.smartregister.fhircore.quest.ui.report.measure.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import kotlinx.coroutines.flow.emptyFlow
import org.smartregister.fhircore.engine.ui.components.CircularProgressBar
import org.smartregister.fhircore.engine.ui.components.ErrorMessage
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.report.measure.MeasureReportEvent
import org.smartregister.fhircore.quest.ui.report.measure.MeasureReportViewModel
import org.smartregister.fhircore.quest.ui.report.measure.components.MeasureReportPatientRow
import org.smartregister.fhircore.quest.ui.shared.components.SearchBar
import timber.log.Timber

@Composable
fun MeasureReportPatientsScreen(
  reportId: String,
  navController: NavController,
  measureReportViewModel: MeasureReportViewModel,
  modifier: Modifier = Modifier
) {
  LaunchedEffect(Unit) { measureReportViewModel.retrievePatients(reportId) }

  val pagingItems =
    measureReportViewModel.patientsData.collectAsState(emptyFlow()).value.collectAsLazyPagingItems()

  Scaffold(
    topBar = {
      Column {
        SearchBar(
          onTextChanged = { text ->
            measureReportViewModel.onEvent(
              MeasureReportEvent.OnSearchTextChanged(reportId = reportId, searchText = text)
            )
          },
          onBackPress = { navController.popBackStack() },
          searchTextState = measureReportViewModel.searchTextState
        )
        Text(
          color = SubtitleTextColor,
          text = stringResource(id = R.string.select_patient),
          fontSize = 14.sp,
          modifier = modifier.wrapContentWidth().padding(16.dp)
        )
        Divider(color = DividerColor)
      }
    }
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      LazyColumn {
        items(pagingItems, key = { it.logicalId }) {
          MeasureReportPatientRow(
            measureReportPatientViewData = it!!,
            onRowClick = { patientViewData ->
              measureReportViewModel.onEvent(MeasureReportEvent.OnPatientSelected(patientViewData))
              navController.popBackStack()
            }
          )
          Divider(color = DividerColor, thickness = 1.dp)
        }
        pagingItems.apply {
          when {
            loadState.refresh is LoadState.Loading ->
              item { CircularProgressBar(modifier = modifier.wrapContentSize(Alignment.Center)) }
            loadState.append is LoadState.Loading ->
              item { CircularProgressBar(modifier = modifier.wrapContentSize(Alignment.Center)) }
            loadState.refresh is LoadState.Error -> {
              val loadStateError = pagingItems.loadState.refresh as LoadState.Error
              item {
                ErrorMessage(
                  message = loadStateError.error.also { Timber.e(it) }.localizedMessage!!,
                  onClickRetry = { retry() }
                )
              }
            }
            loadState.append is LoadState.Error -> {
              val error = pagingItems.loadState.append as LoadState.Error
              item {
                ErrorMessage(message = error.error.localizedMessage!!, onClickRetry = { retry() })
              }
            }
          }
        }
      }
    }
  }
}
