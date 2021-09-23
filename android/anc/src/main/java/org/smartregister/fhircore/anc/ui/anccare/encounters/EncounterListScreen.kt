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

package org.smartregister.fhircore.anc.ui.anccare.encounters

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
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
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import org.hl7.fhir.r4.model.Encounter
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.anc.model.EncounterItem
import org.smartregister.fhircore.engine.ui.theme.AppTheme

@Composable
fun EncounterListScreen(dataProvider: EncounterDataProvider) {
  Surface(color = colorResource(id = R.color.white)) {
    Column {

      // top bar
      TopAppBar(
        title = { Text(text = stringResource(id = R.string.past_encounters)) },
        navigationIcon = {
          IconButton(onClick = { dataProvider.getAppBackClickListener().invoke() }) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back arrow")
          }
        }
      )

      val lazyEncounterItems = dataProvider.getEncounterList().collectAsLazyPagingItems()

      LazyColumn(modifier = Modifier.background(Color.White).fillMaxSize()) {
        itemsIndexed(lazyEncounterItems) { index, item ->
          EncounterItem(item!!, index == lazyEncounterItems.itemCount.minus(1))
        }

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

@Preview
@Composable
fun EncounterItem(
  @PreviewParameter(DummyItem::class) item: EncounterItem,
  isLastItem: Boolean = false
) {
  Column {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.background(Color.White).padding(12.dp).fillMaxWidth()
    ) {

      // icon
      Image(
        painter =
          when (item.status) {
            Encounter.EncounterStatus.FINISHED -> painterResource(id = R.drawable.ic_check)
            Encounter.EncounterStatus.CANCELLED -> painterResource(id = R.drawable.ic_cancelled)
            Encounter.EncounterStatus.UNKNOWN -> painterResource(id = R.drawable.ic_help)
            else -> painterResource(id = R.drawable.ic_help)
          },
        null
      )

      // content
      Column(modifier = Modifier.fillMaxWidth().padding(start = 12.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
          Text(
            text = SimpleDateFormat.getDateInstance().format(item.periodStartDate),
            color = colorResource(id = R.color.status_gray),
            fontSize = 12.sp
          )

          Text(
            text = item.status.name,
            color = colorResource(id = R.color.status_gray),
            fontSize = 11.sp
          )
        }

        Text(text = item.display, color = Color.Black, fontSize = 16.sp)
      }
    }

    if (!isLastItem) {
      Divider(
        color = colorResource(id = R.color.light_gray),
        modifier = Modifier.padding(start = 12.dp, end = 12.dp)
      )
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
fun EncounterListScreenPreview() {
  AppTheme { EncounterListScreen(dummyData()) }
}

fun dummyData() =
  object : EncounterDataProvider {
    override fun getEncounterList(): Flow<PagingData<EncounterItem>> {
      return Pager(PagingConfig(pageSize = 20)) {
          object : PagingSource<Int, EncounterItem>() {
            override fun getRefreshKey(state: PagingState<Int, EncounterItem>): Int? {
              return state.anchorPosition
            }

            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EncounterItem> {
              delay(3000)
              var nextPage: Int? = params.key ?: 0

              val data = mutableListOf<EncounterItem>()
              (0..20).forEach {
                data.add(
                  EncounterItem("$it", Encounter.EncounterStatus.FINISHED, "Encounter $it", Date())
                )
              }

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

class DummyItem : PreviewParameterProvider<EncounterItem> {
  override val values: Sequence<EncounterItem>
    get() =
      listOf(EncounterItem("1", Encounter.EncounterStatus.FINISHED, "Dummy", Date())).asSequence()
}
