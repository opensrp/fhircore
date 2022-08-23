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

package org.smartregister.fhircore.quest.ui.patient.profile.childcontact

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.emptyFlow
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.ui.theme.PatientProfileSectionsBackgroundColor
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.patient.profile.PatientProfileEvent
import org.smartregister.fhircore.quest.ui.patient.profile.PatientProfileViewModel
import org.smartregister.fhircore.quest.ui.patient.register.components.RegisterList
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData

@Composable
fun ChildContactsProfileScreen(
  appFeatureName: String?,
  healthModule: HealthModule,
  patientId: String?,
  navController: NavHostController,
  modifier: Modifier = Modifier,
  patientProfileViewModel: PatientProfileViewModel = hiltViewModel(),
  refreshDataState: MutableState<Boolean>
) {

  val context = LocalContext.current
  val profileViewData = patientProfileViewModel.patientProfileViewData.value
  //  var showOverflowMenu by remember { mutableStateOf(false) }
  //  val viewState = childContactsViewModel.patientProfileUiState.value
  val refreshDataStateValue by remember { refreshDataState }

  val currentPaginateRegisterData by rememberUpdatedState(
    patientProfileViewModel::paginateChildrenRegisterData
  )

  LaunchedEffect(Unit) {
    patientProfileViewModel.fetchPatientProfileDataWithChildren(
      appFeatureName,
      healthModule,
      patientId ?: ""
    )
  }

  SideEffect {
    // Refresh child contacts data on resume
    if (refreshDataStateValue) {
      patientProfileViewModel.fetchPatientProfileDataWithChildren(
        appFeatureName,
        healthModule,
        patientId ?: ""
      )
      currentPaginateRegisterData(appFeatureName, healthModule, true)
      refreshDataState.value = false
    }
  }

  val pagingItems: LazyPagingItems<RegisterViewData> =
    patientProfileViewModel
      .paginatedChildrenRegisterData
      .collectAsState(emptyFlow())
      .value
      .collectAsLazyPagingItems()

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(text = stringResource(R.string.patient_x_children, profileViewData.familyName))
        },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Filled.ArrowBack, null)
          }
        }
      )
    }
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      Column(modifier = modifier.background(PatientProfileSectionsBackgroundColor)) {
        // Personal Data: e.g. sex, age, dob
        // PersonalData(profileViewData)
        RegisterList(
          modifier = modifier,
          pagingItems = pagingItems,
          onRowClick = { patientId: String ->
            patientProfileViewModel.onEvent(
              PatientProfileEvent.OpenChildProfile(
                appFeatureName,
                healthModule,
                patientId,
                navController
              )
            )
          }
        )
      }
    }
  }
}
