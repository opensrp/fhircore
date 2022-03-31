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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.data.local.patient.PatientRegisterRepository
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaireActivity
import org.smartregister.fhircore.quest.ui.patient.profile.model.PatientProfileViewData
import org.smartregister.fhircore.quest.util.mappers.PatientProfileViewDataMapper

@HiltViewModel
class PatientProfileViewModel
@Inject
constructor(
  val patientRegisterRepository: PatientRegisterRepository,
  val patientProfileViewDataMapper: PatientProfileViewDataMapper
) : ViewModel() {

  val patientProfileViewData: MutableState<PatientProfileViewData> =
    mutableStateOf(PatientProfileViewData())

  fun fetchPatientProfileData(
    appFeatureName: String?,
    healthModule: HealthModule,
    patientId: String
  ) {
    if (patientId.isNotEmpty())
      viewModelScope.launch {
        patientRegisterRepository.loadPatientProfileData(appFeatureName, healthModule, patientId)
          ?.let {
            patientProfileViewData.value =
              patientProfileViewDataMapper.transformInputToOutputModel(it)
          }
      }
  }

  fun onEvent(event: PatientProfileEvent) =
    when (event) {
      is PatientProfileEvent.LoadQuestionnaire ->
        event.context.launchQuestionnaireActivity(event.questionnaireId)
      is PatientProfileEvent.SeeAll -> TODO()
    }
}
