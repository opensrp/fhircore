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
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.search
import com.google.android.fhir.sync.Sync
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.data.FhirPeriodicSyncWorker
import org.smartregister.fhircore.data.SamplePatients
import org.smartregister.fhircore.domain.Pagination
import org.smartregister.fhircore.util.Utils

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

  val liveSearchedPaginatedPatients: MutableLiveData<Pair<List<PatientItem>, Pagination>> by lazy {
    MutableLiveData<Pair<List<PatientItem>, Pagination>>()
  }

  fun getObservations(): LiveData<List<ObservationItem>> {
    return liveObservations
  }

  val liveSearchPatient: MutableLiveData<PatientItem> by lazy { MutableLiveData<PatientItem>() }

  fun searchResults(query: String? = null, page: Int = 0, pageSize: Int = 10) {
    viewModelScope.launch {
      val searchResults: List<Patient> =
        fhirEngine.search {
          //Utils.addBasePatientFilter(this)

          apply {
            if (query?.isNotBlank() == true) {
              filter(Patient.FAMILY) {
                value = query.trim()
                modifier = StringFilterModifier.CONTAINS
              }
            }
          }
          sort(Patient.GIVEN, Order.ASCENDING)
          count = pageSize
          from = (page * pageSize)
        }

      liveSearchedPaginatedPatients.value =
        Pair(
          samplePatients.getPatientItems(searchResults),
          Pagination(totalItems = count(query), pageSize = pageSize, currentPage = page)
        )
    }
  }

  /** Returns number of records in database */
  // TODO This is a wasteful query that will be replaced by implementation of
  // https://github.com/google/android-fhir/issues/458
  // https://github.com/opensrp/fhircore/issues/107
  private suspend fun count(query: String? = null): Int {
    val searchResults: List<Patient> =
      fhirEngine.search {
        Utils.addBasePatientFilter(this)

        apply {
          if (query?.isNotBlank() == true) {
            filter(Patient.FAMILY) {
              value = query.trim()
              modifier = StringFilterModifier.CONTAINS
            }
          }
        }
        sort(Patient.GIVEN, Order.ASCENDING)
        count = 10000
        from = 0
      }
    return searchResults.size
  }

  fun getPatientItem(id: String) {
    var patientItems: List<PatientItem>? = null
    viewModelScope.launch {
      val patient = fhirEngine.load(Patient::class.java, id)
      patientItems = samplePatients.getPatientItems(listOf(patient))
    }

    liveSearchPatient.value = patientItems?.get(0)
  }

  fun syncUpload() {
    // TODO: Fix this
    viewModelScope.launch { Sync.oneTimeSync<FhirPeriodicSyncWorker>(getApplication()) }
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
    val phone: String,
    val logicalId: String
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
