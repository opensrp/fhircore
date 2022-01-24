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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.emptyFlow
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.ui.anccare.register.components.AncPatientList
import org.smartregister.fhircore.engine.ui.components.LoaderDialog
import org.smartregister.fhircore.engine.ui.components.PaginatedRegister
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor

@Composable
fun ReportSelectPatientScreen(viewModel: ReportViewModel) {

  val registerData = viewModel.registerDataViewModel.registerData.collectAsState(emptyFlow())
  val pagingItems = registerData.value.collectAsLazyPagingItems()
  val showResultsCount by viewModel.registerDataViewModel.showResultsCount.observeAsState(false)
  val showLoader by viewModel.registerDataViewModel.showLoader.observeAsState(false)

  if (showLoader) LoaderDialog(modifier = Modifier)
  Column(modifier = Modifier.testTag(REPORT_SELECT_PATIENT_LIST)) {
    SearchView(state = viewModel.searchTextState, viewModel)
    Divider(color = DividerColor)
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      color = SubtitleTextColor,
      text = stringResource(id = R.string.select_patient),
      fontSize = 14.sp,
      modifier = Modifier.wrapContentWidth().padding(horizontal = 16.dp)
    )
    Divider(color = DividerColor)
    Spacer(modifier = Modifier.height(8.dp))
    PaginatedRegister(
      loadState = pagingItems.loadState.refresh,
      showResultsCount = showResultsCount,
      resultCount = pagingItems.itemCount,
      body = { ConstructPatientSelectList(pagingItems, viewModel) },
      currentPage = viewModel.registerDataViewModel.currentPage(),
      pagesCount = viewModel.registerDataViewModel.countPages(),
      previousButtonClickListener = { viewModel.registerDataViewModel.previousPage() },
      nextButtonClickListener = { viewModel.registerDataViewModel.nextPage() }
    )
  }
}

@Composable
fun ConstructPatientSelectList(
  pagingItems: LazyPagingItems<PatientItem>,
  viewModel: ReportViewModel
) {
  AncPatientList(
    pagingItems = pagingItems,
    modifier = Modifier,
    clickListener = viewModel::onPatientItemClicked,
    showAncVisitButton = false,
    displaySelectContentOnly = true
  )
}

@Composable
fun SearchView(state: MutableState<TextFieldValue>, viewModel: ReportViewModel) {
  Box(modifier = Modifier.background(color = colorResource(id = R.color.white))) {
    TextField(
      value = state.value,
      onValueChange = { value ->
        state.value = value
        viewModel.filterValue.postValue(Pair(RegisterFilterType.SEARCH_FILTER, value.text))
        viewModel.reportState.currentScreen = ReportViewModel.ReportScreen.PICK_PATIENT
      },
      modifier = Modifier.fillMaxWidth().testTag(REPORT_SEARCH_PATIENT),
      textStyle = TextStyle(fontSize = 18.sp),
      leadingIcon = {
        IconButton(
          onClick = viewModel::onBackPressFromPatientSearch,
          Modifier.testTag(TOOLBAR_BACK_ARROW)
        ) {
          Icon(
            Icons.Filled.ArrowBack,
            contentDescription = "Back arrow",
            modifier = Modifier.padding(15.dp)
          )
        }
      },
      trailingIcon = {
        if (state.value != TextFieldValue("")) {
          IconButton(
            modifier = Modifier.testTag(REPORT_SEARCH_PATIENT_CANCEL),
            onClick = {
              // Remove text from TextField when you press the 'X' icon
              state.value = TextFieldValue("")
              viewModel.filterValue.postValue(
                Pair(RegisterFilterType.SEARCH_FILTER, state.value.text)
              )
              viewModel.reportState.currentScreen = ReportViewModel.ReportScreen.PICK_PATIENT
            }
          ) {
            Icon(
              Icons.Default.Close,
              contentDescription = "",
              modifier = Modifier.padding(15.dp).size(24.dp)
            )
          }
        }
      },
      singleLine = true,
      shape = RectangleShape, // The TextFiled has rounded corners top left and right by default
      colors =
        TextFieldDefaults.textFieldColors(
          backgroundColor = Color.White,
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent,
          disabledIndicatorColor = Color.Transparent
        )
    )
    if (viewModel.searchTextState.value.text == "") SearchHint()
  }
}

@Composable
fun SearchHint(modifier: Modifier = Modifier) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      Modifier.wrapContentHeight()
        .padding(start = 48.dp, top = 16.dp)
        .focusable(false)
        .then(modifier)
        .testTag(REPORT_SEARCH_HINT)
  ) {
    Text(
      color = Color(0xff757575),
      text = stringResource(id = R.string.search_hint),
    )
  }
}
