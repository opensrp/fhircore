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
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Observation
import org.smartregister.fhircore.anc.data.anc.AncPatientRepository
import org.smartregister.fhircore.anc.data.anc.model.AncPatientDetailItem
import org.smartregister.fhircore.anc.data.anc.model.CarePlanItem
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider

class AncDetailsViewModel(
    val ancPatientRepository: AncPatientRepository,
    var dispatcher: DispatcherProvider = DefaultDispatcherProvider,
    val patientId: String
) : ViewModel() {

    fun fetchDemographics(): LiveData<AncPatientDetailItem> {
        val patientDemographics = MutableLiveData<AncPatientDetailItem>()
        viewModelScope.launch(dispatcher.io()) {
            val ancPatientDetailItem = ancPatientRepository.fetchDemographics(patientId = patientId)
            patientDemographics.postValue(ancPatientDetailItem)
        }
        return patientDemographics
    }

    fun fetchCarePlan(): LiveData<List<CarePlanItem>> {
        val patientCarePlan = MutableLiveData<List<CarePlanItem>>()
        viewModelScope.launch(dispatcher.io()) {
            val listCarePlan = ancPatientRepository.searchCarePlan(id = patientId)
            val listCarePlanItem = ancPatientRepository.fetchCarePlanItem(listCarePlan,patientId)
            patientCarePlan.postValue(listCarePlanItem)
        }
        return patientCarePlan
    }

    fun fetchObservation(): LiveData<List<Observation>> {
        val patientObservation = MutableLiveData<List<Observation>>()
        viewModelScope.launch(dispatcher.io()) {
            val listObservation = ancPatientRepository.fetchObservations(patientId = patientId)
            patientObservation.postValue(listObservation)
        }
        return patientObservation
    }

    fun fetchEncounters(): LiveData<List<Encounter>> {
        val patientEncounters = MutableLiveData<List<Encounter>>()
        viewModelScope.launch(dispatcher.io()) {
            val listEncounters = ancPatientRepository.fetchEncounters(patientId = patientId)
            patientEncounters.postValue(listEncounters)
        }
        return patientEncounters
    }
}
