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

package org.smartregister.fhircore.quest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import org.smartregister.fhircore.engine.ui.components.register.RegisterFooter
import org.smartregister.fhircore.engine.ui.components.register.RegisterHeader
import org.smartregister.fhircore.engine.ui.filter.FilterOption
import org.smartregister.fhircore.engine.ui.theme.GreyTextColor
import org.smartregister.fhircore.quest.ui.patient.register.components.RegisterList
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData

@Composable
fun PageRegisterScreen(
  modifier: Modifier,
  screenTitle: String,
  navController: NavHostController,
  registerViewModel: StandardRegisterViewModel,
  filterNavClickAction: () -> Unit,
  activeFilters: List<FilterOption> = listOf(),
  showFilterValues: Boolean = false,
) {
  val searchTextState = registerViewModel.searchText.collectAsState()
  val searchText by remember { searchTextState }

  val pagingItems: LazyPagingItems<RegisterViewData> =
    registerViewModel.paginatedRegisterData.collectAsState().value.collectAsLazyPagingItems()

  Scaffold(
    topBar = {
      // Top section has toolbar and a results counts view
      TopSection(
        title = screenTitle,
        searchText = searchText,
        onSearchTextChanged = { searchText ->
          registerViewModel.onEvent(StandardRegisterEvent.SearchRegister(searchText = searchText))
        },
        onNavIconClick = { navController.popBackStack() },
        onFilterIconClick = filterNavClickAction,
        activeFilters = activeFilters,
      )
    },
    bottomBar = {
      // Bottom section has a pagination footer and button with client registration action
      // Only show when filtering data is not active
      if (pagingItems.loadState.refresh is LoadState.NotLoading && searchText.isEmpty()) {
        registerViewModel.loadCount()
      }

      Column {
        if (searchText.isEmpty() && pagingItems.itemCount > 0) {
          RegisterFooter(
            currentPageStateFlow = registerViewModel.currentPage,
            pagesCountStateFlow = registerViewModel.totalRecordsCountPages,
            previousButtonClickListener = {
              registerViewModel.onEvent(StandardRegisterEvent.MoveToPreviousPage)
            },
            nextButtonClickListener = {
              registerViewModel.onEvent(StandardRegisterEvent.MoveToNextPage)
            },
          )
        }
      }
    },
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      // Only show counter during search
      var iModifier = Modifier.padding(top = 0.dp)
      if (searchText.isNotEmpty() || (showFilterValues && activeFilters.isNotEmpty())) {
        iModifier = Modifier.padding(top = 32.dp)
        RegisterHeader(
          resultCount = if (searchText.isEmpty()) -1 else pagingItems.itemCount,
          activeFilters = activeFilters,
        )
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
            registerViewModel.onEvent(StandardRegisterEvent.OpenProfile(patientId, navController))
          },
          progressMessage = registerViewModel.progressMessage(),
        )
      }
    }
  }
}

@Composable
fun TopSection(
  modifier: Modifier = Modifier,
  title: String,
  searchText: String,
  onSearchTextChanged: (String) -> Unit,
  onNavIconClick: () -> Unit,
  onFilterIconClick: () -> Unit = {},
  activeFilters: List<FilterOption> = listOf(),
) {
  Column(
    modifier = modifier.fillMaxWidth().background(MaterialTheme.colors.primary),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = modifier.padding(vertical = 8.dp),
    ) {
      IconButton(onClick = onNavIconClick) {
        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
      }
      Text(text = title, fontSize = 20.sp, color = Color.White, modifier = Modifier.weight(1f))
      IconButton(onClick = onFilterIconClick) {
        BadgedBox(
          badge = {
            if (activeFilters.isNotEmpty()) {
              Badge { Text(text = activeFilters.size.toString()) }
            }
          },
        ) {
          Icon(Icons.Filled.FilterList, contentDescription = "Filter", tint = Color.White)
        }
      }
    }

    OutlinedTextField(
      colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.DarkGray),
      value = searchText,
      onValueChange = { onSearchTextChanged(it) },
      maxLines = 1,
      singleLine = true,
      placeholder = {
        Text(
          color = GreyTextColor,
          text = stringResource(org.smartregister.fhircore.engine.R.string.search_hint),
        )
      },
      modifier =
        modifier
          .padding(start = 16.dp, bottom = 8.dp, end = 16.dp)
          .fillMaxWidth()
          .clip(RoundedCornerShape(size = 10.dp))
          .background(Color.White),
      leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = "Search") },
      trailingIcon = {
        if (searchText.isNotEmpty()) {
          IconButton(onClick = { onSearchTextChanged("") }) {
            Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear", tint = Color.Gray)
          }
        }
      },
    )
  }
}

@Preview
@Composable
fun PreviewTopSection() {
  TopSection(
    title = "Test Register",
    searchText = "Search \u2026",
    onSearchTextChanged = {},
    onNavIconClick = {},
    onFilterIconClick = {},
    activeFilters = listOf(),
  )
}
