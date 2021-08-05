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
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import com.google.android.fhir.sync.Sync
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PositiveIntType
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.R
import org.smartregister.fhircore.api.HapiFhirService
import org.smartregister.fhircore.data.HapiFhirResourceDataSource
import org.smartregister.fhircore.domain.Pagination
import org.smartregister.fhircore.fragment.PAGE_COUNT
import org.smartregister.fhircore.fragment.PatientDetailsCard
import org.smartregister.fhircore.fragment.toDetailsCard
import org.smartregister.fhircore.model.PatientItem
import org.smartregister.fhircore.model.PatientStatus
import org.smartregister.fhircore.model.PatientVaccineSummary
import org.smartregister.fhircore.model.VaccineStatus
import org.smartregister.fhircore.util.Utils

/**
 * The ViewModel helper class for PatientItemRecyclerViewAdapter, that is responsible for preparing
 * data for UI.
 */
class PatientListViewModel(application: Application, private val fhirEngine: FhirEngine) :
  AndroidViewModel(application) {

  var showOverduePatientsOnly = MutableLiveData(false)
  var loadingListObservable = MutableLiveData(-1)

  val liveSearchedPaginatedPatients: MutableLiveData<Pair<List<PatientItem>, Pagination>> by lazy {
    MutableLiveData<Pair<List<PatientItem>, Pagination>>()
  }

  fun searchResults(query: String? = null, page: Int = 0, pageSize: Int = 10) {
    viewModelScope.launch(Dispatchers.IO) {
      loadingListObservable.postValue(1)
      var totalCount = count(query).toInt()

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
          count = totalCount
          from = (page * pageSize)
        }

      var patients = searchResults.map { it.toPatientItem() }
      patients.forEach { it.vaccineStatus = getPatientStatus(it.logicalId) }
      if (showOverduePatientsOnly.value!!) {
        patients = patients.filter { it.vaccineStatus!!.status == VaccineStatus.OVERDUE }
      }
      totalCount = patients.size + (page * pageSize)
      patients = patients.take(pageSize)

      loadingListObservable.postValue(0)
      liveSearchedPaginatedPatients.postValue(
        Pair(patients, Pagination(totalItems = totalCount, pageSize = pageSize, currentPage = page))
      )
    }
  }

  suspend fun getPatientStatus(id: String): PatientStatus {
    // check database for immunizations
    val cal: Calendar = Calendar.getInstance()
    cal.add(Calendar.DATE, -28)
    val overDueStart: Date = cal.time

    val formatter = SimpleDateFormat("dd-MM-yy", Locale.US)

    val searchResults: List<Immunization> =
      fhirEngine.search { filter(Immunization.PATIENT) { value = "Patient/$id" } }

    val computedStatus =
      if (searchResults.size >= 2) VaccineStatus.VACCINATED
      else if (searchResults.size == 1 && searchResults[0].recorded.before(overDueStart))
        VaccineStatus.OVERDUE
      else if (searchResults.size == 1) VaccineStatus.PARTIAL else VaccineStatus.DUE

    return PatientStatus(
      status = computedStatus,
      details = if (searchResults.isNotEmpty()) formatter.format(searchResults[0].recorded) else ""
    )
  }

  /** Basic search for immunizations */
  fun fetchPatientDetailsCards(
    context: Context,
    patientId: String
  ): LiveData<List<PatientDetailsCard>> {
    val liveSearchImmunization: MutableLiveData<List<PatientDetailsCard>> = MutableLiveData()
    viewModelScope.launch {
      val result =
        mutableListOf(fhirEngine.load(Patient::class.java, patientId).toDetailsCard(context))

      val immunizations: List<Immunization> =
        fhirEngine.search { filter(Immunization.PATIENT) { value = "Patient/$patientId" } }

      immunizations.forEachIndexed { index, element ->
        result.add(element.toDetailsCard(context, index, index < 1))
      }

      if (immunizations.isEmpty()) {
        result.add(
          PatientDetailsCard(-1, -1, "-1", "", context.getString(R.string.no_vaccine_received), "")
        )
      }

      liveSearchImmunization.value = result
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
      val patient = fhirEngine.load(Patient::class.java, id).toPatientItem()

      val immunizations: List<Immunization> =
        fhirEngine.search { filter(Immunization.PATIENT) { value = "Patient/$id" } }

      if (immunizations.isNotEmpty()) {
        val immunization = immunizations[0]
        patient.vaccineSummary =
          PatientVaccineSummary(
            (immunization.protocolApplied[0].doseNumber as PositiveIntType).value,
            immunization.vaccineCode.coding.first().code
          )
      }

      liveSearchPatient.value = patient
    }
    return liveSearchPatient
  }

  fun runSync() {
    /** Download Immediately from the server */
    GlobalScope.launch(Dispatchers.IO) {
      loadingListObservable.postValue(1)
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
      searchResults("", 0, PAGE_COUNT)
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

fun Patient.toPatientItem(): PatientItem {
  val name = this.name[0].nameAsSingleString

  // Show nothing if no values available for gender and date of birth.
  val gender = if (this.hasGenderElement()) this.genderElement.valueAsString else ""
  val dob = if (this.hasBirthDateElement()) this.birthDateElement.valueAsString else ""
  val html: String = if (this.hasText()) this.text.div.valueAsString else ""
  val phone: String =
    if (this.hasTelecom() && this.telecom[0].hasValue()) this.telecom[0].value else ""
  val logicalId: String = this.logicalId
  val ext = this.extension.singleOrNull { it.value.toString().contains("risk") }
  val risk = ext?.value?.toString() ?: ""
  val lastSeen = Utils.getLastSeen(logicalId, meta.lastUpdated)

  return PatientItem(
    this.logicalId,
    name,
    gender,
    dob,
    html,
    phone,
    logicalId,
    risk,
    lastSeen = lastSeen
  )
}
