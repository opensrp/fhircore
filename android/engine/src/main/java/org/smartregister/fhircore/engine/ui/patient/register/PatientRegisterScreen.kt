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

package org.smartregister.fhircore.engine.ui.patient.register

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.Job
import org.smartregister.fhircore.engine.ui.components.register.RegisterFooter
import org.smartregister.fhircore.engine.ui.components.register.RegisterHeader
import org.smartregister.fhircore.engine.ui.components.register.RegisterList
import org.smartregister.fhircore.engine.ui.main.component.TopScreenSection

@Composable
fun PatientRegisterScreen(
  modifier: Modifier = Modifier,
  openDrawer: () -> Job,
  patientRegisterViewModel: PatientRegisterViewModel = hiltViewModel()
) {
  val searchText by remember { patientRegisterViewModel.searchText }
  val pagingItems =
    patientRegisterViewModel
      .paginateData(currentPage = 0, loadAll = false)
      .collectAsState()
      .value
      .collectAsLazyPagingItems()
  Column {
    TopScreenSection(
      title = "AllFamilies",
      searchText = searchText,
      onSearchTextChanged = { searchText ->
        patientRegisterViewModel.onEvent(PatientRegisterEvent.SearchRegister(searchText))
      }
    ) { openDrawer() }
    RegisterHeader(resultCount = 0)
    RegisterList(pagingItems = pagingItems)
    RegisterFooter(
      resultCount = pagingItems.itemCount,
      currentPage = patientRegisterViewModel.currentPage.observeAsState(initial = 0).value,
      pagesCount = patientRegisterViewModel.countPages(),
      previousButtonClickListener = {
        patientRegisterViewModel.onEvent(PatientRegisterEvent.MoveToPreviousPage)
      },
      nextButtonClickListener = {
        patientRegisterViewModel.onEvent(PatientRegisterEvent.MoveToNextPage)
      }
    )
    Button(modifier = modifier.fillMaxWidth(), onClick = { /*TODO Register client*/}) {
      Text(text = "Register Client", modifier = modifier.padding(8.dp))
    }
  }
}
