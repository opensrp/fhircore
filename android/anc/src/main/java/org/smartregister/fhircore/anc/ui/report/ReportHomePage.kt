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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun ReportHomePreview() {
  ReportHomePage(
    topBarTitle = "PageTitle",
    onBackPress = {},
    dataList = emptyFlow(),
    onReportMeasureItemClick = {}
  )
}

@Composable
fun ReportHomeScreen(viewModel: ReportViewModel) {
  ReportHomePage(
    topBarTitle = stringResource(id = R.string.reports),
    onBackPress = viewModel::onBackPress,
    dataList = viewModel.getReportsTypeList(),
    onReportMeasureItemClick = viewModel::onReportMeasureItemClicked
  )
}

@Composable
fun ReportHomePage(
  topBarTitle: String,
  onBackPress: () -> Unit,
  dataList: Flow<PagingData<ReportItem>>,
  onReportMeasureItemClick: (ReportItem) -> Unit
) {
  Surface(color = colorResource(id = R.color.white)) {
    Column {
      TopBarBox(topBarTitle = topBarTitle, onBackPress = onBackPress)
      ReportHomeListBox(dataList = dataList, onReportMeasureItemClick = onReportMeasureItemClick)
    }
  }
}

@Composable
fun ReportHomeListBox(
  dataList: Flow<PagingData<ReportItem>>,
  onReportMeasureItemClick: (ReportItem) -> Unit
) {
  val lazyReportItems = dataList.collectAsLazyPagingItems()
  LazyColumn(
    modifier = Modifier.background(Color.White).fillMaxSize().testTag(REPORT_MEASURE_LIST)
  ) {
    items(items = lazyReportItems, key = { it.id }) { item ->
      ReportRow(item!!, Modifier.clickable(onClick = { onReportMeasureItemClick(item) }))
      Divider(color = DividerColor, thickness = 1.dp)
    }
    lazyReportItems.apply {
      when {
        loadState.refresh is LoadState.Loading -> item { LoadingItem() }
        loadState.append is LoadState.Loading -> item { LoadingItem() }
      }
    }
  }
}
