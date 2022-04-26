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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.emptyFlow
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.ui.components.register.RegisterFooter
import org.smartregister.fhircore.engine.ui.components.register.RegisterHeader
import org.smartregister.fhircore.quest.ui.main.components.TopScreenSection
import org.smartregister.fhircore.quest.ui.patient.register.components.RegisterList
import org.smartregister.fhircore.quest.ui.patient.register.model.RegisterViewData

@Composable
fun PatientRegisterScreen(
  modifier: Modifier = Modifier,
  appFeatureName: String?,
  healthModule: HealthModule,
  screenTitle: String,
  openDrawer: (Boolean) -> Unit,
  navController: NavHostController,
  patientRegisterViewModel: PatientRegisterViewModel = hiltViewModel()
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val searchText by remember { patientRegisterViewModel.searchText }
  val registerConfigs = remember { patientRegisterViewModel.registerViewConfiguration }
  // Safely update the current lambdas when a new one is provided
  val currentSetTotalRecordCount by rememberUpdatedState(
    patientRegisterViewModel::setTotalRecordsCount
  )
  val currentPaginateRegisterData by rememberUpdatedState(
    patientRegisterViewModel::paginateRegisterData
  )

  DisposableEffect(lifecycleOwner) {
    // Create an observer that triggers our remembered functions for setting register total count
    // and paginating the data. This will be triggered whenever the activity resumes to refresh data
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        currentSetTotalRecordCount(appFeatureName, healthModule)
        currentPaginateRegisterData(appFeatureName, healthModule, false)
      }
    }

    lifecycleOwner.lifecycle.run {
      // Register observer to lifecycle
      addObserver(observer)
      // Remove observer when effect leaves the lifecycle
      onDispose { removeObserver(observer) }
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
            onClick = {
              patientRegisterViewModel.onEvent(PatientRegisterEvent.RegisterNewClient(context))
            }
          ) { Text(text = registerConfigs.newClientButtonText, modifier = modifier.padding(8.dp)) }
        }
      }
    }
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      RegisterList(
        pagingItems = pagingItems,
        onOpenProfileClick = { patientId: String ->
          patientRegisterViewModel.onEvent(
            PatientRegisterEvent.OpenProfile(appFeatureName, healthModule, patientId, navController)
          )
        }
      )
    }
  }
}
