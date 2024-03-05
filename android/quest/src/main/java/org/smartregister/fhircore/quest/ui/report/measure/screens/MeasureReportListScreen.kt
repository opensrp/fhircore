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

package org.smartregister.fhircore.quest.ui.report.measure.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.report.measure.ReportConfiguration
import org.smartregister.fhircore.quest.ui.report.measure.components.MeasureReportRow

@Composable
fun MeasureReportListScreen(
  navController: NavController,
  dataList: Flow<PagingData<ReportConfiguration>>,
  onReportMeasureClicked: (List<ReportConfiguration>) -> Unit,
  modifier: Modifier = Modifier,
  showProgressIndicator: Boolean = false,
) {
  val lazyReportItems = dataList.collectAsLazyPagingItems().itemSnapshotList.groupBy { it?.module }
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(text = stringResource(R.string.reports)) },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Filled.ArrowBack, null)
          }
        },
        contentColor = Color.White,
        backgroundColor = MaterialTheme.colors.primary,
      )
    },
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      if (showProgressIndicator) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            CircularProgressIndicator(
              modifier = modifier.size(40.dp).testTag(SHOW_PROGRESS_INDICATOR_TAG),
              strokeWidth = 2.dp,
            )
            Text(
              text = stringResource(R.string.please_wait),
              textAlign = TextAlign.Center,
              modifier = modifier.padding(vertical = 16.dp).testTag(PLEASE_WAIT_TEST_TAG),
            )
          }
        }
      } else {
        LazyColumn(
          modifier = modifier.background(Color.White).fillMaxSize().padding(bottom = 32.dp),
        ) {
          lazyReportItems.keys.forEach { key ->
            item {
              key?.let { it1 ->
                MeasureReportRow(
                  it1,
                  { onReportMeasureClicked(lazyReportItems[key] as List<ReportConfiguration>) },
                )
              }
            }
          }
        }
      }
    }
  }
}
