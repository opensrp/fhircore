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

package org.smartregister.fhircore.eir.ui.patient.details

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.eir.data.PatientRepository
import org.smartregister.fhircore.engine.configuration.view.ImmunizationProfileViewConfiguration

@HiltViewModel
class PatientDetailsViewModel @Inject constructor(val patientRepository: PatientRepository) :
  ViewModel() {

  val immunizationProfileConfiguration = MutableLiveData<ImmunizationProfileViewConfiguration>()

  val patientDemographics = MutableLiveData<Patient>()

  val patientImmunizations = MutableLiveData<List<Immunization>>()

  fun fetchDemographics(patientId: String) {
    if (patientId.isNotEmpty())
      viewModelScope.launch() {
        patientDemographics.postValue(patientRepository.fetchDemographics(patientId))
      }
  }

  fun fetchImmunizations(patientId: String) {
    if (patientId.isNotEmpty())
      viewModelScope.launch() {
        patientImmunizations.postValue(patientRepository.getPatientImmunizations(patientId))
      }
  }

  fun updateViewConfiguration(
    immunizationProfileViewConfiguration: ImmunizationProfileViewConfiguration
  ) {
    this.immunizationProfileConfiguration.value = immunizationProfileViewConfiguration
  }
}
