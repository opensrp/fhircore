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

package org.smartregister.fhircore.anc.ui.reports

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemsIndexed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.report.ReportDataProvider
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

const val TOOLBAR_TITLE = "toolbarTitle"
const val TOOLBAR_BACK_ARROW = "toolbarBackArrow"

@Composable
fun ReportsHomeScreen(dataProvider: ReportDataProvider) {
  Surface(color = colorResource(id = R.color.white)) {
    Column {

      // top bar
      TopAppBar(
        title = {
          Text(text = stringResource(id = R.string.reports), Modifier.testTag(TOOLBAR_TITLE))
        },
        navigationIcon = {
          IconButton(
            onClick = { dataProvider.getAppBackClickListener().invoke() },
            Modifier.testTag(TOOLBAR_BACK_ARROW)
          ) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back arrow") }
        }
      )

      val lazyEncounterItems = dataProvider.getReportsTypeList().collectAsLazyPagingItems()

      LazyColumn(modifier = Modifier.background(Color.White).fillMaxSize()) {
        itemsIndexed(lazyEncounterItems) { _, item -> ReportRow(item!!, { _, _ -> }) }

        lazyEncounterItems.apply {
          when {
            loadState.refresh is LoadState.Loading -> {
              item { LoadingItem() }
            }
            loadState.append is LoadState.Loading -> {
              item { LoadingItem() }
            }
          }
        }
      }
    }
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

@Preview
@Composable
@ExcludeFromJacocoGeneratedReport
fun ReportsHomeScreenPreview() {
  AppTheme { ReportsHomeScreen(dummyReportData()) }
}

fun dummyReportData() =
  object : ReportDataProvider {
    override fun getReportsTypeList(): Flow<PagingData<ReportItem>> {
      return Pager(PagingConfig(pageSize = 20)) {
          object : PagingSource<Int, ReportItem>() {
            override fun getRefreshKey(state: PagingState<Int, ReportItem>): Int? {
              return state.anchorPosition
            }

            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ReportItem> {
              delay(3000)
              var nextPage: Int? = params.key ?: 0

              val data = mutableListOf<ReportItem>()
              (0..20).forEach { data.add(ReportItem("$it", "title $it", "Report $it", "")) }

              val dataMap =
                mapOf(Pair(0, data), Pair(1, data), Pair(2, data), Pair(3, data), Pair(4, data))

              val result = dataMap[nextPage] ?: listOf()

              nextPage = if (nextPage!! >= 4) null else nextPage.plus(1)
              return LoadResult.Page(result, null, nextPage)
            }
          }
        }
        .flow
    }

    override fun getAppBackClickListener(): () -> Unit {
      return {}
    }
  }

@Composable
fun ReportRow(
  reportItem: ReportItem,
  clickListener: (ReportListenerIntent, ReportItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
  ) {
    Column(
      modifier =
        modifier
          .clickable { clickListener(OpenReportFilter, reportItem) }
          .padding(16.dp)
          .weight(0.70f)
    ) {
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
  ReportRow(reportItem = reportItem, { _, _ -> })
}
