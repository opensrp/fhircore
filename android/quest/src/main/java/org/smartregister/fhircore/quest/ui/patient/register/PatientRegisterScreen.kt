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

package org.smartregister.fhircore.quest.ui.patient.register

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.emptyFlow
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.ui.components.register.RegisterFooter
import org.smartregister.fhircore.engine.ui.components.register.RegisterHeader
import org.smartregister.fhircore.quest.ui.main.component.TopScreenSection
import org.smartregister.fhircore.quest.ui.patient.register.components.RegisterList
import org.smartregister.fhircore.quest.ui.patient.register.model.RegisterViewData

@Composable
fun PatientRegisterScreen(
  modifier: Modifier = Modifier,
  appFeatureName: String?,
  healthModule: HealthModule,
  screenTitle: String,
  openDrawer: (Boolean) -> Unit,
  patientRegisterViewModel: PatientRegisterViewModel = hiltViewModel()
) {
  val searchText by remember { patientRegisterViewModel.searchText }
  // TODO activate after view configurations refactor
  //  val registerConfigs = remember { patientRegisterViewModel.registerViewConfiguration }
  val context = LocalContext.current
  LaunchedEffect(Unit) {
    patientRegisterViewModel.run {
      setTotalRecordsCount(appFeatureName, healthModule)
      paginateRegisterData(appFeatureName, healthModule)
    }
  }
  val pagingItems: LazyPagingItems<RegisterViewData> =
    patientRegisterViewModel
      .paginatedRegisterData
      .collectAsState(emptyFlow())
      .value
      .collectAsLazyPagingItems()

  Scaffold(
    topBar = {
      // Top section has toolbar and a results counts view
      Column {
        TopScreenSection(
          title = screenTitle,
          searchText = searchText,
          onSearchTextChanged = { searchText ->
            patientRegisterViewModel.onEvent(
              PatientRegisterEvent.SearchRegister(
                searchText = searchText,
                appFeatureName = appFeatureName,
                healthModule = healthModule
              )
            )
          }
        ) { openDrawer(true) }
        // Only show counter during search
        if (searchText.isNotEmpty()) RegisterHeader(resultCount = pagingItems.itemCount)
      }
    },
    bottomBar = {
      // Bottom section has a pagination footer and button with client registration action
      // Only show when filtering data is not active
      Column {
        if (searchText.isEmpty()) {
          RegisterFooter(
            resultCount = pagingItems.itemCount,
            currentPage =
              patientRegisterViewModel.currentPage.observeAsState(initial = 0).value.plus(1),
            pagesCount = patientRegisterViewModel.countPages(),
            previousButtonClickListener = {
              patientRegisterViewModel.onEvent(
                PatientRegisterEvent.MoveToPreviousPage(
                  appFeatureName = appFeatureName,
                  healthModule = healthModule
                )
              )
            },
            nextButtonClickListener = {
              patientRegisterViewModel.onEvent(
                PatientRegisterEvent.MoveToNextPage(
                  appFeatureName = appFeatureName,
                  healthModule = healthModule
                )
              )
            }
          )
          Button(
            modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            onClick = { PatientRegisterEvent.RegisterNewClient(context) }
          ) { Text(text = "Register Client", modifier = modifier.padding(8.dp)) }
        }
      }
    }
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) { RegisterList(pagingItems = pagingItems) }
  }
}
