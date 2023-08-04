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

package org.dtree.fhircore.dataclerk.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import org.dtree.fhircore.dataclerk.ui.home.PatientItemCard

@Composable
fun SearchScreen(
  navHostController: NavHostController,
  viewModel: SearchViewModel = hiltViewModel()
) {
  val userSearchModelState by viewModel.userSearchModelState.collectAsState(
    initial = SearchModelState.Empty
  )
  SearchBarUI(
    searchText = userSearchModelState.searchText,
    placeholderText = "Search Patients",
    onSearchTextChanged = { viewModel.onSearchChanged(it) },
    onClearClick = { viewModel.onClearClick() },
    onNavigateBack = { navHostController.popBackStack() },
    matchesFound = userSearchModelState.patients.isNotEmpty()
  ) {
    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(8.dp),
    ) {
      items(items = userSearchModelState.patients) { patient ->
        PatientItemCard(
          patient,
          onClick = { navHostController.navigate("patient/${patient.resourceId}") }
        )
      }
    }
  }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchBarUI(
  searchText: String,
  placeholderText: String = "",
  onSearchTextChanged: (String) -> Unit = {},
  onClearClick: () -> Unit = {},
  onNavigateBack: () -> Unit = {},
  matchesFound: Boolean,
  results: (@Composable() () -> Unit) = {}
) {

  Box {
    Column(modifier = Modifier.fillMaxSize()) {
      SearchBar(searchText, placeholderText, onSearchTextChanged, onClearClick, onNavigateBack)

      if (matchesFound) {
        Text("Results", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold)
        results()
      } else {
        if (searchText.isNotEmpty()) {
          NoSearchResults()
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun SearchBar(
  searchText: String,
  placeholderText: String = "",
  onSearchTextChanged: (String) -> Unit = {},
  onClearClick: () -> Unit = {},
  onNavigateBack: () -> Unit = {}
) {
  var showClearButton by remember { mutableStateOf(false) }
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusRequester = remember { FocusRequester() }

  TopAppBar(
    navigationIcon = {
      IconButton(onClick = { onNavigateBack() }) {
        Icon(imageVector = Icons.Filled.ArrowBack, modifier = Modifier, contentDescription = "")
      }
    },
    actions = { Text("") },
    title = {
      OutlinedTextField(
        modifier =
          Modifier.fillMaxWidth()
            .padding(vertical = 2.dp)
            .onFocusChanged { focusState -> showClearButton = (focusState.isFocused) }
            .focusRequester(focusRequester),
        value = searchText,
        onValueChange = onSearchTextChanged,
        placeholder = { Text(text = placeholderText) },
        colors =
          TextFieldDefaults.textFieldColors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            containerColor = Color.Transparent,
            cursorColor = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
          ),
        trailingIcon = {
          AnimatedVisibility(visible = showClearButton, enter = fadeIn(), exit = fadeOut()) {
            IconButton(onClick = { onClearClick() }) {
              Icon(imageVector = Icons.Filled.Close, contentDescription = "")
            }
          }
        },
        maxLines = 1,
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
      )
    }
  )

  LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
fun NoSearchResults() {

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = CenterHorizontally
  ) { Text("No matches found") }
}
