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
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import com.google.android.fhir.sync.Sync
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.api.HapiFhirService
import org.smartregister.fhircore.data.HapiFhirResourceDataSource
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

  fun searchResults(query: String? = null, page: Int = 0, pageSize: Int = 10) {
    viewModelScope.launch {
      val searchResults: List<Patient> =
        fhirEngine.search {
          Utils.addBasePatientFilter(this)

          apply {
            if (query?.isNotBlank() == true) {
              filter(Patient.NAME) {
                modifier = StringFilterModifier.CONTAINS
                value = query.trim()
              }
            }
          }

          sort(Patient.GIVEN, Order.ASCENDING)
          count = pageSize
          from = (page * pageSize)
        }

      val patients = samplePatients.getPatientItems(searchResults)
      patients.forEach { it.vaccineStatus = getPatientStatus(it.logicalId) }

      liveSearchedPaginatedPatients.value =
        Pair(
          patients,
          Pagination(totalItems = count(query).toInt(), pageSize = pageSize, currentPage = page)
        )
    }
  }

  public suspend fun getPatientStatus(id: String): PatientStatus {
    // check database for immunizations
    val cal: Calendar = Calendar.getInstance()
    cal.add(Calendar.DATE, -28)
    val overDueStart: Date = cal.time

    val formatter = SimpleDateFormat("dd-MM-yy", Locale.US)

    val searchResults: List<Immunization> =
      fhirEngine.search { filter(Immunization.PATIENT) { value = "Patient/$id" } }

    val computedStatus =
      if (searchResults.size == 2) VaccineStatus.VACCINATED
      else if (searchResults.size == 1 && searchResults[0].recorded.before(overDueStart))
        VaccineStatus.OVERDUE
      else if (searchResults.size == 1) VaccineStatus.PARTIAL else VaccineStatus.DUE

    return PatientStatus(
      status = computedStatus,
      details = if (searchResults.isNotEmpty()) formatter.format(searchResults[0].recorded) else ""
    )
  }

  /** Basic search for immunizations */
  fun searchImmunizations(patientId: String? = null): LiveData<List<Immunization>> {
    val liveSearchImmunization: MutableLiveData<List<Immunization>> = MutableLiveData()
    viewModelScope.launch {
      val searchResults: List<Immunization> =
        fhirEngine.search { filter(Immunization.PATIENT) { value = "Patient/$patientId" } }
      liveSearchImmunization.value = searchResults
    }
    return liveSearchImmunization
  }

  private suspend fun count(query: String? = null): Long {
    return fhirEngine.count<Patient> {
      Utils.addBasePatientFilter(this)
      apply {
        if (query?.isNotBlank() == true) {
          filter(Patient.NAME) {
            modifier = StringFilterModifier.CONTAINS
            value = query.trim()
          }
        }
      }
    }
  }

  fun getPatientItem(id: String): LiveData<PatientItem> {
    val liveSearchPatient: MutableLiveData<PatientItem> = MutableLiveData()
    viewModelScope.launch {
      val patient = fhirEngine.load(Patient::class.java, id)
      liveSearchPatient.value = samplePatients.getPatientItem(patient)
    }
    return liveSearchPatient
  }

  fun runSync() {
    viewModelScope.launch {
      // fhirEngine.syncUpload()

      /** Download Immediately from the server */
      GlobalScope.launch {
        Sync.oneTimeSync(
          fhirEngine,
          HapiFhirResourceDataSource(
            HapiFhirService.create(FhirContext.forR4().newJsonParser(), getApplication())
          ),
          mapOf(
            ResourceType.Patient to mapOf("address-city" to "NAIROBI"),
            ResourceType.Immunization to mapOf()
          )
        )
      }
    }
  }

  fun isPatientExists(id: String): LiveData<Result<Boolean>> {
    val result = MutableLiveData<Result<Boolean>>()
    viewModelScope.launch {
      try {
        fhirEngine.load(Patient::class.java, id)
        result.value = Result.success(true)
      } catch (e: ResourceNotFoundException) {
        result.value = Result.failure(e)
      }
    }
    return result
  }

  fun clearPatientList() {
    liveSearchedPaginatedPatients.value =
      Pair(emptyList(), Pagination(totalItems = 0, pageSize = 1, currentPage = 0))
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
    val logicalId: String,
    val risk: String,
    var vaccineStatus: PatientStatus? = null
  ) {
    override fun toString(): String = name
  }

  data class PatientStatus(val status: VaccineStatus, val details: String)

  enum class VaccineStatus {
    VACCINATED,
    PARTIAL,
    OVERDUE,
    DUE
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
