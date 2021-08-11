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

package org.smartregister.fhircore.eir.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.search
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.eir.util.DefaultDispatcherProvider
import org.smartregister.fhircore.eir.util.DispatcherProvider

class PatientDetailsViewModel(
  var dispatcher: DispatcherProvider = DefaultDispatcherProvider,
  val fhirEngine: FhirEngine,
  val patientId: String
) : ViewModel() {

  val patientDemographics = MutableLiveData<Patient>()

  val patientImmunizations = MutableLiveData<List<Immunization>>()

  fun fetchDemographics() {
    if (patientId.isNotEmpty())
      viewModelScope.launch(dispatcher.io()) {
        val patient = fhirEngine.load(Patient::class.java, patientId)
        patientDemographics.postValue(patient)
      }
  }

  fun fetchImmunizations() {
    if (patientId.isNotEmpty())
      viewModelScope.launch(dispatcher.io()) {
        val immunizations: List<Immunization> =
          fhirEngine.search { filter(Immunization.PATIENT) { value = "Patient/$patientId" } }
        patientImmunizations.postValue(immunizations)
      }
  }
}
