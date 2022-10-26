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

package org.smartregister.fhircore.quest.ui.tracing.register

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import org.smartregister.fhircore.engine.ui.components.register.RegisterFooter
import org.smartregister.fhircore.engine.ui.components.register.RegisterHeader
import org.smartregister.fhircore.quest.ui.main.components.TopScreenSection
import org.smartregister.fhircore.quest.ui.patient.register.components.RegisterList
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData

@Composable
fun TracingRegisterScreen(
  modifier: Modifier = Modifier,
  screenTitle: String,
  openDrawer: (Boolean) -> Unit,
  navController: NavHostController,
  registerViewModel: TracingRegisterViewModel = hiltViewModel()
) {
  val context = LocalContext.current
  val searchTextState = registerViewModel.searchText.collectAsState()
  val searchText by remember { searchTextState }
  val registerConfigs = remember { registerViewModel.registerViewConfiguration }

  val pagingItems: LazyPagingItems<RegisterViewData> =
    registerViewModel.paginatedRegisterData.collectAsState().value.collectAsLazyPagingItems()

  Scaffold(
    topBar = {
      // Top section has toolbar and a results counts view
      TopScreenSection(
        title = screenTitle,
        searchText = searchText,
        onSearchTextChanged = { searchText ->
          registerViewModel.onEvent(TracingRegisterEvent.SearchRegister(searchText = searchText))
        }
      ) { openDrawer(true) }
    },
    bottomBar = {
      // Bottom section has a pagination footer and button with client registration action
      // Only show when filtering data is not active
      Column {
        if (searchText.isEmpty()) {
          RegisterFooter(
            resultCount = pagingItems.itemCount,
            currentPage = registerViewModel.currentPage.observeAsState(initial = 0).value.plus(1),
            pagesCount = registerViewModel.countPages().observeAsState(initial = 1).value,
            previousButtonClickListener = {
              registerViewModel.onEvent(TracingRegisterEvent.MoveToPreviousPage)
            },
            nextButtonClickListener = {
              registerViewModel.onEvent(TracingRegisterEvent.MoveToNextPage)
            }
          )
        }
      }
    }
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      // Only show counter during search
      var iModifier = Modifier.padding(top = 0.dp)
      if (searchText.isNotEmpty()) {
        iModifier = Modifier.padding(top = 32.dp)
        RegisterHeader(resultCount = pagingItems.itemCount)
      }

      val isRefreshing by registerViewModel.isRefreshing.collectAsState()
      SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = { registerViewModel.refresh() },
        //        indicator = { _, _ -> }
        ) {
        RegisterList(
          modifier = iModifier,
          pagingItems = pagingItems,
          onRowClick = { patientId: String ->
            registerViewModel.onEvent(TracingRegisterEvent.OpenProfile(patientId, navController))
          },
          progressMessage = registerViewModel.progressMessage()
        )
      }
    }
  }
}
