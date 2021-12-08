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

package org.smartregister.fhircore.anc.ui.anccare.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.smartregister.fhircore.anc.data.model.AncOverviewItem
import org.smartregister.fhircore.anc.data.model.CarePlanItem
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.anc.data.model.PatientDetailItem
import org.smartregister.fhircore.anc.data.model.UpcomingServiceItem
import org.smartregister.fhircore.anc.data.patient.DeletionReason
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.makeItReadable

@HiltViewModel
class AncDetailsViewModel
@Inject
constructor(val patientRepository: PatientRepository, var dispatcher: DispatcherProvider) :
  ViewModel() {

  fun fetchDemographics(patientId: String): LiveData<PatientDetailItem> {
    val patientDemographics = MutableLiveData<PatientDetailItem>()
    viewModelScope.launch(dispatcher.io()) {
      val ancPatientDetailItem = patientRepository.fetchDemographics(patientId = patientId)
      patientDemographics.postValue(ancPatientDetailItem)
    }
    return patientDemographics
  }

  fun fetchCarePlan(patientId: String): LiveData<List<CarePlanItem>> {
    val patientCarePlan = MutableLiveData<List<CarePlanItem>>()
    viewModelScope.launch(dispatcher.io()) {
      val listCarePlan = patientRepository.searchCarePlan(patientId = patientId)
      val listCarePlanItem = patientRepository.fetchCarePlanItem(listCarePlan)
      patientCarePlan.postValue(listCarePlanItem)
    }
    return patientCarePlan
  }

  fun fetchObservation(patientId: String): LiveData<AncOverviewItem> {
    val patientAncOverviewItem = MutableLiveData<AncOverviewItem>()
    val ancOverviewItem = AncOverviewItem()
    viewModelScope.launch(dispatcher.io()) {
      val listObservationEDD = patientRepository.fetchObservations(patientId = patientId, "edd")
      val listObservationGA = patientRepository.fetchObservations(patientId = patientId, "ga")
      val listObservationFetuses =
        patientRepository.fetchObservations(patientId = patientId, "fetuses")
      val listObservationRisk = patientRepository.fetchObservations(patientId = patientId, "risk")

      if (listObservationEDD.valueDateTimeType != null &&
          listObservationEDD.valueDateTimeType.value != null
      )
        ancOverviewItem.edd = listObservationEDD.valueDateTimeType.value.makeItReadable()
      if (listObservationGA.valueIntegerType != null &&
          listObservationGA.valueIntegerType.valueAsString != null
      )
        ancOverviewItem.ga = listObservationGA.valueIntegerType.valueAsString
      if (listObservationFetuses.valueIntegerType != null &&
          listObservationFetuses.valueIntegerType.valueAsString != null
      )
        ancOverviewItem.noOfFetuses = listObservationFetuses.valueIntegerType.valueAsString
      if (listObservationRisk.valueIntegerType != null &&
          listObservationRisk.valueIntegerType.valueAsString != null
      )
        ancOverviewItem.risk = listObservationRisk.valueIntegerType.valueAsString

      patientAncOverviewItem.postValue(ancOverviewItem)
    }
    return patientAncOverviewItem
  }

  fun fetchUpcomingServices(patientId: String): LiveData<List<UpcomingServiceItem>> {
    val patientEncounters = MutableLiveData<List<UpcomingServiceItem>>()
    viewModelScope.launch(dispatcher.io()) {
      val listEncounters = patientRepository.fetchCarePlan(patientId = patientId)
      val listEncountersItem = patientRepository.fetchUpcomingServiceItem(listEncounters)
      patientEncounters.postValue(listEncountersItem)
    }
    return patientEncounters
  }

  fun fetchLastSeen(patientId: String): LiveData<List<EncounterItem>> {
    val patientEncounters = MutableLiveData<List<EncounterItem>>()
    viewModelScope.launch(dispatcher.io()) {
      val listEncounters = patientRepository.fetchEncounters(patientId = patientId)
      val listEncountersItem = patientRepository.fetchLastSeenItem(listEncounters)
      patientEncounters.postValue(listEncountersItem)
    }
    return patientEncounters
  }

  fun deletePatient(patientId: String, reason: DeletionReason): LiveData<Boolean> {
    val changed = MutableLiveData(false)
    viewModelScope.launch(dispatcher.io()) {
      patientRepository.deletePatient(patientId, reason)
      changed.postValue(true)
    }
    return changed
  }

  fun markDeceased(patientId: String, deathDate: Date): LiveData<Boolean> {
    val changed = MutableLiveData(false)
    viewModelScope.launch(dispatcher.io()) {
      patientRepository.markDeceased(patientId, deathDate)
      changed.postValue(true)
    }
    return changed
  }
}
