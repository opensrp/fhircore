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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
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
import org.smartregister.fhircore.engine.ui.components.FormButton
import org.smartregister.fhircore.quest.ui.patient.profile.components.PersonalData
import org.smartregister.fhircore.quest.ui.patient.profile.components.ProfileActionableItem
import org.smartregister.fhircore.quest.ui.patient.profile.components.ProfileCard
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

  val profileViewData = patientProfileViewModel.patientProfileViewData.value

  Scaffold(
    topBar = {
      TopAppBar(
        title = {},
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Filled.ArrowBack, null)
          }
        }
      )
    }
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        // Personal Data: e.g. sex, age, dob
        PersonalData(profileViewData)

        // Patient tasks: List of tasks for the patients
        if (profileViewData.tasks.isNotEmpty()) {
          ProfileCard(
            title = stringResource(R.string.tasks).uppercase(),
            onActionClick = {},
            profileViewSection = PatientProfileViewSection.TASKS
          ) { profileViewData.tasks.forEach { ProfileActionableItem(it) } }
        }

        // Forms: Loaded for quest app
        if (profileViewData.forms.isNotEmpty()) {
          ProfileCard(
            title = stringResource(R.string.forms),
            onActionClick = { patientProfileViewModel.onEvent(PatientProfileEvent.SeeAll(it)) },
            profileViewSection = PatientProfileViewSection.FORMS
          ) {
            profileViewData.forms.forEach {
              FormButton(
                formButtonData = it,
                onFormClick = { questionnaireId ->
                  patientProfileViewModel.onEvent(
                    PatientProfileEvent.LoadQuestionnaire(questionnaireId, context)
                  )
                }
              )
            }
          }
        }

        // Medical History: Show medication history for the patient
        if (profileViewData.medicalHistoryData.isNotEmpty()) {
          ProfileCard(
            title = stringResource(R.string.medical_history),
            onActionClick = { patientProfileViewModel.onEvent(PatientProfileEvent.SeeAll(it)) },
            profileViewSection = PatientProfileViewSection.MEDICAL_HISTORY
          ) { profileViewData.medicalHistoryData.forEach { ProfileActionableItem(it) } }
        }

        // Upcoming Services: Display upcoming services (or tasks) for the patient
        if (profileViewData.upcomingServices.isNotEmpty()) {
          ProfileCard(
            title = stringResource(R.string.upcoming_services),
            onActionClick = { patientProfileViewModel.onEvent(PatientProfileEvent.SeeAll(it)) },
            profileViewSection = PatientProfileViewSection.UPCOMING_SERVICES
          ) { profileViewData.upcomingServices.forEach { ProfileActionableItem(it) } }
        }

        // Service Card: Display other vital information for ANC/PNC
        if (profileViewData.ancCardData.isNotEmpty()) {
          ProfileCard(
            title = stringResource(R.string.service_card),
            onActionClick = { patientProfileViewModel.onEvent(PatientProfileEvent.SeeAll(it)) },
            profileViewSection = PatientProfileViewSection.SERVICE_CARD
          ) { profileViewData.ancCardData.forEach { ProfileActionableItem(it) } }
        }
      }
    }
  }
}
