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

package org.smartregister.fhircore.quest.ui.tracing.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.flow.StateFlow
import org.smartregister.fhircore.engine.ui.components.EmptyState
import org.smartregister.fhircore.engine.ui.components.ErrorMessage
import org.smartregister.fhircore.quest.ui.patient.register.components.BoxedCircularProgressBar
import org.smartregister.fhircore.quest.util.GeneralListViewModel
import timber.log.Timber

@Composable
fun <T : Any> TracingDataScaffoldList(
  title: String,
  navController: NavHostController,
  viewModel: GeneralListViewModel<T>,
  content: (@Composable() (value: T) -> Unit)
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(title) },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Filled.ArrowBack, null)
          }
        },
      )
    }
  ) { innerPadding ->
    val pagingItems: LazyPagingItems<T> =
      viewModel.paginateData.collectAsState().value.collectAsLazyPagingItems()

    TracingDataList(
      modifier = Modifier.padding(innerPadding).fillMaxSize(),
      pagingItems = pagingItems,
      refreshing = viewModel.isRefreshing,
      refresh = { viewModel.refresh() },
      content = content
    )
  }
}

@Composable
fun <T : Any> TracingDataList(
  pagingItems: LazyPagingItems<T>,
  modifier: Modifier = Modifier,
  refreshing: StateFlow<Boolean>,
  refresh: () -> Unit,
  content: (@Composable() (value: T) -> Unit)
) {
  Box(modifier = modifier) {
    val isRefreshing by refreshing.collectAsState()
    SwipeRefresh(
      state = rememberSwipeRefreshState(isRefreshing),
      onRefresh = refresh,
      //        indicator = { _, _ -> }
      ) {
      LazyColumn(
        modifier = modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        items(pagingItems) { history -> if (history != null) content(history) }
        pagingItems.apply {
          if (itemCount <= 0 && loadState.source.refresh is LoadState.NotLoading && loadState.append.endOfPaginationReached) {
            item { EmptyState(message = "No items available") }
          }
          when {
            loadState.refresh is LoadState.Loading ->
              item { BoxedCircularProgressBar(progressMessage = "Refreshing") }
            loadState.append is LoadState.Loading ->
              item { BoxedCircularProgressBar(progressMessage = "Loading") }
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
