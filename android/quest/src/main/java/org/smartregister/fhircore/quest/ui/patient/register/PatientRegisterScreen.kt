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
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.ui.components.register.LoaderDialog
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
  screenTitle: String,
  openDrawer: (Boolean) -> Unit,
  navController: NavHostController,
  patientRegisterViewModel: PatientRegisterViewModel = hiltViewModel()
) {
  val context = LocalContext.current
  val firstTimeSyncState = patientRegisterViewModel.firstTimeSyncState.collectAsState()
  val firstTimeSync by remember { firstTimeSyncState }
  val searchTextState = patientRegisterViewModel.searchText.collectAsState()
  val searchText by remember { searchTextState }
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
            patientRegisterViewModel.syncBroadcaster.runSync()
            patientRegisterViewModel.onEvent(
              PatientRegisterEvent.OpenProfile(patientId, navController)
            )
          }
        }
      }
    )
  val registerConfigs = remember { patientRegisterViewModel.registerViewConfiguration }

  val pagingItems: LazyPagingItems<RegisterViewData> =
    patientRegisterViewModel.paginatedRegisterData.collectAsState().value.collectAsLazyPagingItems()

  Scaffold(
    topBar = {
      // Top section has toolbar and a results counts view
      TopScreenSection(
        title = screenTitle,
        searchText = searchText,
        onSearchTextChanged = { searchText ->
          patientRegisterViewModel.onEvent(
            PatientRegisterEvent.SearchRegister(searchText = searchText)
          )
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
            currentPage =
              patientRegisterViewModel.currentPage.observeAsState(initial = 0).value.plus(1),
            pagesCount = patientRegisterViewModel.countPages().observeAsState(initial = 1).value,
            previousButtonClickListener = {
              patientRegisterViewModel.onEvent(PatientRegisterEvent.MoveToPreviousPage)
            },
            nextButtonClickListener = {
              patientRegisterViewModel.onEvent(PatientRegisterEvent.MoveToNextPage)
            }
          )
          // TODO activate this button action via config; now only activated for family register
          if (patientRegisterViewModel.isAppFeatureHousehold() ||
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
              },
              enabled = !firstTimeSync
            ) {
              Text(text = registerConfigs.newClientButtonText, modifier = modifier.padding(8.dp))
            }
          }
        }
      }
    }
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      if (firstTimeSync) LoaderDialog(modifier = modifier)
      // Only show counter during search
      var iModifier = Modifier.padding(top = 0.dp)
      if (searchText.isNotEmpty()) {
        iModifier = Modifier.padding(top = 32.dp)
        RegisterHeader(resultCount = pagingItems.itemCount)
      }

      val isRefreshing by patientRegisterViewModel.isRefreshing.collectAsState()
      SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = { patientRegisterViewModel.refresh() },
        //        indicator = { _, _ -> }
        ) {
        RegisterList(
          modifier = iModifier,
          pagingItems = pagingItems,
          onRowClick = { patientId: String ->
            patientRegisterViewModel.onEvent(
              PatientRegisterEvent.OpenProfile(patientId, navController)
            )
          },
          progressMessage = patientRegisterViewModel.progressMessage()
        )
      }
    }
  }
}
