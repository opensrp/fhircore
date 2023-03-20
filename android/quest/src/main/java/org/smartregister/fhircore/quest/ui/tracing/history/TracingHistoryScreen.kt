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

package org.smartregister.fhircore.quest.ui.tracing.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import org.smartregister.fhircore.engine.domain.model.TracingHistory
import org.smartregister.fhircore.engine.ui.components.ErrorMessage
import org.smartregister.fhircore.engine.ui.theme.SuccessColor
import org.smartregister.fhircore.engine.util.extension.asDdMmYyyy
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.patient.register.components.BoxedCircularProgressBar
import org.smartregister.fhircore.quest.ui.tracing.components.OutlineCard
import timber.log.Timber

@Composable
fun TracingHistoryScreen(
  navController: NavHostController,
  viewModel: TracingHistoryViewModel = hiltViewModel()
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(id = R.string.tracing_history)) },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Filled.ArrowBack, null)
          }
        },
      )
    }
  ) { innerPadding ->
    TracingHistoryScreenContainer(
      navController = navController,
      modifier = Modifier.padding(innerPadding).fillMaxSize(),
      viewModel = viewModel
    )
  }
}

@Composable
fun TracingHistoryScreenContainer(
  navController: NavHostController,
  modifier: Modifier = Modifier,
  viewModel: TracingHistoryViewModel,
) {
  val context = LocalContext.current
  val pagingItems: LazyPagingItems<TracingHistory> =
    viewModel.paginateData.collectAsState().value.collectAsLazyPagingItems()

  Box(modifier = modifier) {
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    SwipeRefresh(
      state = rememberSwipeRefreshState(isRefreshing),
      onRefresh = { viewModel.refresh() },
      //        indicator = { _, _ -> }
      ) {
      LazyColumn(
        modifier = modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        items(pagingItems) { history ->
          if (history != null)
            TracingHistoryCard(
              history = history,
              onClick = {
                viewModel.onEvent(
                  TracingHistoryEvent.OpenOutComesScreen(
                    context = context,
                    navController = navController,
                    historyId = history.historyId
                  )
                )
              }
            )
        }
        pagingItems.apply {
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

@Composable
fun TracingHistoryCard(history: TracingHistory, onClick: () -> Unit) {
  OutlineCard(modifier = Modifier.clickable(onClick = onClick)) {
    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
      Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Start Date: ${history.startDate.asDdMmYyyy()}")
      }
      Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = "End Date: ${history.endDate?.asDdMmYyyy() ?: ""}")
      }
      Text(
        text = if (history.isActive) "Active" else "Completed",
        style =
          MaterialTheme.typography.subtitle1.copy(
            color = if (history.isActive) SuccessColor else MaterialTheme.colors.onSurface,
            fontWeight = FontWeight.Bold
          ),
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}
