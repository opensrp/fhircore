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

package org.smartregister.fhircore.quest.ui.patient.register.guardians

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.quest.data.patient.HivPatientGuardianRepository
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.util.mappers.RegisterViewDataMapper

@HiltViewModel
class GuardianRegisterViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  repository: HivPatientGuardianRepository,
  registerViewDataMapper: RegisterViewDataMapper
) : ViewModel() {

  // Get your argument from the SavedStateHandle
  private val patientId: String = savedStateHandle[NavigationArg.PATIENT_ID]!!

  private val _guardianUiDetails =
    mutableStateOf(GuardianUiState(patientFirstName = "", registerViewData = emptyList()))

  val guardianUiDetails: State<GuardianUiState> = _guardianUiDetails

  init {
    viewModelScope.launch {
      val patient = repository.loadPatient(patientId)
      val guardiansRegisterViewData =
        repository.loadGuardianRegisterData(patient!!).map {
          registerViewDataMapper.transformInputToOutputModel(it)
        }
      _guardianUiDetails.value =
        GuardianUiState(
          patientFirstName = patient.extractFirstName(),
          registerViewData = guardiansRegisterViewData
        )
    }
  }

  private fun Patient.extractFirstName() = this.extractName().substringBefore(" ")
}
