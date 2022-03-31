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

package org.smartregister.fhircore.quest.ui.patient.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.quest.ui.patient.profile.components.PatientForm
import org.smartregister.fhircore.quest.ui.patient.profile.components.PatientProfileCard
import org.smartregister.fhircore.quest.ui.patient.profile.components.PersonalData
import org.smartregister.fhircore.quest.ui.patient.profile.components.ProfileActionableItem
import org.smartregister.fhircore.quest.ui.patient.profile.model.PatientProfileViewSection

@Composable
fun PatientProfileScreen(
  appFeatureName: String?,
  healthModule: HealthModule,
  patientId: String?,
  navController: NavHostController,
  modifier: Modifier = Modifier,
  patientProfileViewModel: PatientProfileViewModel = hiltViewModel()
) {

  LaunchedEffect(Unit) {
    patientProfileViewModel.fetchPatientProfileData(appFeatureName, healthModule, patientId ?: "")
  }

  val context = LocalContext.current
  val patientProfileData = patientProfileViewModel.patientProfileViewData.value

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(text = "") },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Filled.ArrowBack, null)
          }
        }
      )
    }
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      LazyColumn {
        // Personal Data: e.g. sex, age, dob
        item { PersonalData(patientProfileData) }

        // Patient tasks: List of tasks for the patients
        items(items = patientProfileData.tasks) {
          PatientProfileCard(
            title = stringResource(R.string.tasks),
            onActionClick = {},
            profileViewSection = PatientProfileViewSection.TASKS
          ) { ProfileActionableItem(it) }
        }

        // Forms: Loaded for quest app
        items(items = patientProfileData.forms) {
          PatientProfileCard(
            title = stringResource(R.string.forms),
            onActionClick = { patientProfileViewModel.onEvent(PatientProfileEvent.SeeAll(it)) },
            profileViewSection = PatientProfileViewSection.FORMS
          ) {
            PatientForm(
              patientProfileViewData = it,
              onFormClick = {
                patientProfileViewModel.onEvent(PatientProfileEvent.LoadQuestionnaire(it, context))
              }
            )
          }
        }

        // Medical History: Show medication history for the patient
        items(items = patientProfileData.medicalHistoryData) {
          PatientProfileCard(
            title = stringResource(R.string.medical_history),
            onActionClick = { patientProfileViewModel.onEvent(PatientProfileEvent.SeeAll(it)) },
            profileViewSection = PatientProfileViewSection.MEDICAL_HISTORY
          ) { ProfileActionableItem(it) }
        }

        // Upcoming Services: Display upcoming services (or tasks) for the patient
        items(items = patientProfileData.upcomingServices) {
          PatientProfileCard(
            title = stringResource(R.string.upcoming_services),
            onActionClick = { patientProfileViewModel.onEvent(PatientProfileEvent.SeeAll(it)) },
            profileViewSection = PatientProfileViewSection.UPCOMING_SERVICES
          ) { ProfileActionableItem(it) }
        }

        // Service Card: Display other vital information for ANC/PNC
        items(items = patientProfileData.ancCardData) {
          PatientProfileCard(
            title = stringResource(R.string.service_card),
            onActionClick = { patientProfileViewModel.onEvent(PatientProfileEvent.SeeAll(it)) },
            profileViewSection = PatientProfileViewSection.SERVICE_CARD
          ) { ProfileActionableItem(it) }
        }
      }
    }
  }
}
