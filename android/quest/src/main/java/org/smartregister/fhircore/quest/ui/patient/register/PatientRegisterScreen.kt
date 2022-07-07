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

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.emptyFlow
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.appfeature.AppFeature
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.ui.components.register.RegisterFooter
import org.smartregister.fhircore.engine.ui.components.register.RegisterHeader
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.quest.ui.main.components.TopScreenSection
import org.smartregister.fhircore.quest.ui.patient.register.components.RegisterList
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData

@Composable
fun PatientRegisterScreen(
  modifier: Modifier = Modifier,
  appFeatureName: String?,
  healthModule: HealthModule,
  screenTitle: String,
  openDrawer: (Boolean) -> Unit,
  navController: NavHostController,
  refreshDataState: MutableState<Boolean>,
  patientRegisterViewModel: PatientRegisterViewModel = hiltViewModel()
) {
  val context = LocalContext.current
  val searchText by remember { patientRegisterViewModel.searchText }
  val patientRegistrationLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.StartActivityForResult(),
      onResult = {
        if (it.resultCode == Activity.RESULT_OK && it.data != null) {
          val questionnaireResponse =
            it.data!!
              .getStringExtra(QuestionnaireActivity.QUESTIONNAIRE_RESPONSE)
              ?.decodeResourceFromString<QuestionnaireResponse>()
          val patientId = questionnaireResponse?.subject?.extractId()
          if (patientId != null) {
            patientRegisterViewModel.onEvent(
              PatientRegisterEvent.OpenProfile(
                appFeatureName,
                healthModule,
                patientId,
                navController
              )
            )
          }
        }
      }
    )
  val registerConfigs = remember { patientRegisterViewModel.registerViewConfiguration }
  val currentSetTotalRecordCount by rememberUpdatedState(
    patientRegisterViewModel::setTotalRecordsCount
  )
  val currentPaginateRegisterData by rememberUpdatedState(
    patientRegisterViewModel::paginateRegisterData
  )
  val refreshDataStateValue by remember { refreshDataState }

  LaunchedEffect(Unit) {
    currentSetTotalRecordCount(appFeatureName, healthModule)
    currentPaginateRegisterData(appFeatureName, healthModule, false)
  }

  SideEffect {
    // Refresh data everytime sync completes then reset state
    if (refreshDataStateValue) {
      currentSetTotalRecordCount(appFeatureName, healthModule)
      currentPaginateRegisterData(appFeatureName, healthModule, false)
      refreshDataState.value = false
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
          // TODO activate this button action via config; now only activated for family register
          if (appFeatureName.equals(AppFeature.HouseholdManagement.name, true) ||
              patientRegisterViewModel.isRegisterFormViaSettingExists()
          ) {
            Button(
              modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
              onClick = {
                patientRegistrationLauncher.launch(
                  patientRegisterViewModel.patientRegisterQuestionnaireIntent(context)
                )
                //
                // patientRegisterViewModel.onEvent(PatientRegisterEvent.RegisterNewClient(context))
              }
            ) {
              Text(text = registerConfigs.newClientButtonText, modifier = modifier.padding(8.dp))
            }
          }
        }
      }
    }
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      RegisterList(
        pagingItems = pagingItems,
        onRowClick = { patientId: String ->
          patientRegisterViewModel.onEvent(
            PatientRegisterEvent.OpenProfile(appFeatureName, healthModule, patientId, navController)
          )
        }
      )
    }
  }
}
