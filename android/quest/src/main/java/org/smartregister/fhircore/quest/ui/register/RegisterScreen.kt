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

package org.smartregister.fhircore.quest.ui.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.emptyFlow
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.configuration.register.NoResultsConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.ui.components.register.LoaderDialog
import org.smartregister.fhircore.engine.ui.components.register.RegisterFooter
import org.smartregister.fhircore.engine.ui.components.register.RegisterHeader
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.quest.ui.main.components.TopScreenSection
import org.smartregister.fhircore.quest.ui.register.components.RegisterCardList
import org.smartregister.fhircore.quest.ui.shared.components.ExtendedFab
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent

const val NO_REGISTER_VIEW_COLUMN_TEST_TAG = "noRegisterViewColumnTestTag"
const val NO_REGISTER_VIEW_TITLE_TEST_TAG = "noRegisterViewTitleTestTag"
const val NO_REGISTER_VIEW_MESSAGE_TEST_TAG = "noRegisterViewMessageTestTag"
const val NO_REGISTER_VIEW_BUTTON_TEST_TAG = "noRegisterViewButtonTestTag"
const val NO_REGISTER_VIEW_BUTTON_ICON_TEST_TAG = "noRegisterViewButtonIconTestTag"
const val NO_REGISTER_VIEW_BUTTON_TEXT_TEST_TAG = "noRegisterViewButtonTextTestTag"

@Composable
fun RegisterScreen(
  modifier: Modifier = Modifier,
  screenTitle: String,
  registerId: String,
  openDrawer: (Boolean) -> Unit,
  refreshDataState: MutableState<Boolean>,
  registerViewModel: RegisterViewModel = hiltViewModel(),
  navController: NavController
) {
  val context = LocalContext.current
  val firstTimeSync = remember { mutableStateOf(registerViewModel.isFirstTimeSync()) }
  val searchText by remember { registerViewModel.searchText }
  val registerConfiguration by remember {
    mutableStateOf(registerViewModel.retrieveRegisterConfiguration(registerId))
  }
  val currentSetTotalRecordCount by rememberUpdatedState(registerViewModel::setTotalRecordsCount)
  val currentPaginateRegisterData by rememberUpdatedState(registerViewModel::paginateRegisterData)
  val refreshDataStateValue by remember { refreshDataState }
  val totalRecordsCount by registerViewModel.totalRecordsCount.observeAsState(0)

  LaunchedEffect(Unit) {
    currentSetTotalRecordCount(registerId)
    currentPaginateRegisterData(registerId, false)
  }

  SideEffect {
    // Refresh data everytime sync completes then reset state
    if (refreshDataStateValue) {
      currentSetTotalRecordCount(registerId)
      currentPaginateRegisterData(registerId, false)
      firstTimeSync.value = registerViewModel.isFirstTimeSync()
      refreshDataState.value = false
    }
  }

  val pagingItems: LazyPagingItems<ResourceData> =
    registerViewModel
      .paginatedRegisterData
      .collectAsState(emptyFlow())
      .value
      .collectAsLazyPagingItems()

  Scaffold(
    topBar = {
      Column {
        // Top section has toolbar and a results counts view
        TopScreenSection(
          title = screenTitle,
          searchText = searchText,
          searchPlaceholder = registerConfiguration.searchBar?.display,
          onSearchTextChanged = { searchText ->
            registerViewModel.onEvent(
              RegisterEvent.SearchRegister(searchText = searchText, registerId = registerId)
            )
          }
        ) { openDrawer(true) }
        // Only show counter during search
        if (searchText.isNotEmpty()) RegisterHeader(resultCount = pagingItems.itemCount)
      }
    },
    bottomBar = {
      // Bottom section has a pagination footer
      Column {
        if (searchText.isEmpty() && pagingItems.itemCount > 0) {
          RegisterFooter(
            resultCount = pagingItems.itemCount,
            currentPage = registerViewModel.currentPage.observeAsState(initial = 0).value.plus(1),
            pagesCount = registerViewModel.countPages(),
            previousButtonClickListener = {
              registerViewModel.onEvent(RegisterEvent.MoveToPreviousPage(registerId))
            },
            nextButtonClickListener = {
              registerViewModel.onEvent(RegisterEvent.MoveToNextPage(registerId))
            }
          )
        }
      }
    },
    floatingActionButton = {
      val fabActions = registerConfiguration.fabActions
      if (fabActions.isNotEmpty() && fabActions.first().visible) {
        ExtendedFab(fabActions = fabActions, navController = navController)
      }
    }
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      if (firstTimeSync.value) LoaderDialog(modifier = modifier)
      if (totalRecordsCount > 0) {
        RegisterCardList(
          registerCardConfig =
            registerViewModel.retrieveRegisterConfiguration(registerId).registerCard,
          pagingItems = pagingItems,
          navController = navController
        )
      } else {
        registerConfiguration.noResults?.let { noResultConfig ->
          NoRegisterDataView(modifier = modifier, noResults = noResultConfig) {
            noResultConfig.actionButton?.actions?.handleClickEvent(navController)
          }
        }
      }
    }
  }
}

@Composable
fun NoRegisterDataView(
  modifier: Modifier = Modifier,
  noResults: NoResultsConfig,
  onClick: () -> Unit
) {
  Column(
    modifier = modifier.fillMaxSize().padding(16.dp).testTag(NO_REGISTER_VIEW_COLUMN_TEST_TAG),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      text = noResults.title,
      fontSize = 16.sp,
      modifier = modifier.padding(vertical = 8.dp).testTag(NO_REGISTER_VIEW_TITLE_TEST_TAG),
      fontWeight = FontWeight.Bold
    )
    Text(
      text = noResults.message,
      modifier =
        modifier.padding(start = 32.dp, end = 32.dp).testTag(NO_REGISTER_VIEW_MESSAGE_TEST_TAG),
      textAlign = TextAlign.Center,
      fontSize = 15.sp,
      color = Color.Gray
    )
    Button(
      modifier = modifier.padding(vertical = 16.dp).testTag(NO_REGISTER_VIEW_BUTTON_TEST_TAG),
      onClick = onClick
    ) {
      Icon(
        imageVector = Icons.Filled.Add,
        contentDescription = null,
        modifier.padding(end = 8.dp).testTag(NO_REGISTER_VIEW_BUTTON_ICON_TEST_TAG)
      )
      Text(
        text = noResults.actionButton?.display?.uppercase().toString(),
        modifier.testTag(NO_REGISTER_VIEW_BUTTON_TEXT_TEST_TAG)
      )
    }
  }
}

@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
@Composable
private fun PreviewNoRegistersView() {
  NoRegisterDataView(
    noResults =
      NoResultsConfig(
        title = "Title",
        message = "This is message",
        actionButton = NavigationMenuConfig(display = "Button Text", id = "1")
      )
  ) {}
}
