/*
 * Copyright 2021 Ona Systems Inc
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

package org.smartregister.fhircore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.rest.param.ParamPrefixEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.search
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.data.SamplePatients

private const val OBSERVATIONS_JSON_FILENAME = "sample_observations_bundle.json"

/**
 * The ViewModel helper class for PatientItemRecyclerViewAdapter, that is responsible for preparing
 * data for UI.
 */
class PatientListViewModel(application: Application, private val fhirEngine: FhirEngine) :
  AndroidViewModel(application) {

  // Make sample Fhir Patients and Observations available, in case needed for demo.
  private val jsonStringObservations = getAssetFileAsString(OBSERVATIONS_JSON_FILENAME)

  private val samplePatients = SamplePatients()

  private val observations = samplePatients.getObservationItems(jsonStringObservations)
  private val liveObservations: MutableLiveData<List<ObservationItem>> =
    MutableLiveData(observations)

  val liveSearchedPatients: MutableLiveData<List<PatientItem>> by lazy {
    MutableLiveData<List<PatientItem>>()
  }

  fun getObservations(): LiveData<List<ObservationItem>> {
    return liveObservations
  }

  fun getSearchResults() {
    viewModelScope.launch {
      val searchResults: List<Patient> =
        fhirEngine.search {
          filter(Patient.ADDRESS_CITY) {
            prefix = ParamPrefixEnum.EQUAL
            value = "NAIROBI"
          }
          sort(Patient.GIVEN, Order.ASCENDING)
          count = 100
          from = 0
        }
      liveSearchedPatients.value = samplePatients.getPatientItems(searchResults)
    }
  }

  fun syncUpload() {
    viewModelScope.launch { fhirEngine.syncUpload() }
  }

  private fun getAssetFileAsString(filename: String): String {
    return this.getApplication<Application>()
      .applicationContext
      .assets
      .open(filename)
      .bufferedReader()
      .use { it.readText() }
  }

  /** The Patient's details for display purposes. */
  data class PatientItem(
    val id: String,
    val name: String,
    val gender: String,
    val dob: String,
    val html: String,
    val phone: String
  ) {
    override fun toString(): String = name
  }

  /** The Observation's details for display purposes. */
  data class ObservationItem(
    val id: String,
    val code: String,
    val effective: String,
    val value: String
  ) {
    override fun toString(): String = code
  }
}

class PatientListViewModelFactory(
  private val application: Application,
  private val fhirEngine: FhirEngine
) : ViewModelProvider.Factory {
  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(PatientListViewModel::class.java)) {
      return PatientListViewModel(application, fhirEngine) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
