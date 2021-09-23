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
import org.hl7.fhir.r4.model.Coding
import org.smartregister.fhircore.anc.data.anc.AncPatientRepository
import org.smartregister.fhircore.anc.data.anc.model.AncOverviewItem
import org.smartregister.fhircore.anc.data.anc.model.AncPatientDetailItem
import org.smartregister.fhircore.anc.data.anc.model.CarePlanItem
import org.smartregister.fhircore.anc.data.anc.model.UpcomingServiceItem
import org.smartregister.fhircore.engine.util.DateUtils.makeItReadable
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider

class AncDetailsViewModel(
    val ancPatientRepository: AncPatientRepository,
    var dispatcher: DispatcherProvider = DefaultDispatcherProvider,
    val patientId: String
) : ViewModel() {

    lateinit var patientDemographics: MutableLiveData<AncPatientDetailItem>

    fun fetchDemographics(): LiveData<AncPatientDetailItem> {
        patientDemographics = MutableLiveData<AncPatientDetailItem>()
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
            val listCarePlanItem = ancPatientRepository.fetchCarePlanItem(listCarePlan, patientId)
            patientCarePlan.postValue(listCarePlanItem)
        }
        return patientCarePlan
    }

    fun fetchObservation(): LiveData<AncOverviewItem> {
        val patientAncOverviewItem = MutableLiveData<AncOverviewItem>()
        val ancOverviewItem = AncOverviewItem()
        viewModelScope.launch(dispatcher.io()) {
            val listObservation = ancPatientRepository.fetchObservations(patientId = patientId)
            if (listObservation.isNotEmpty()) {
                for (i in listObservation.indices) {
                    if (listObservation[i].code != null)
                        if (listObservation[i].code.coding != null)
                            if (listObservation[i].code.coding.isNotEmpty())
                                for (j in listObservation[i].code.coding.indices) {
                                    val coding = listObservation[i].code.coding[j] as Coding
                                    if (coding.display != null) {
                                        if (coding.display.isNotEmpty()) {
                                            when {
                                                coding.display.lowercase()
                                                    .contains("edd") -> ancOverviewItem.EDD =
                                                    listObservation[i].valueDateTimeType.value.makeItReadable()
                                                coding.display.lowercase()
                                                    .contains("ga") -> ancOverviewItem.GA =
                                                    listObservation[i].valueIntegerType.valueAsString
                                                coding.display.lowercase()
                                                    .contains("fetuses") -> ancOverviewItem.noOfFetusses =
                                                    listObservation[i].valueIntegerType.valueAsString
                                                coding.display.lowercase()
                                                    .contains("risk") -> ancOverviewItem.risk =
                                                    listObservation[i].valueStringType.valueNotNull
                                            }
                                        }
                                    }
                                }
                }
            }
            patientAncOverviewItem.postValue(ancOverviewItem)
        }
        return patientAncOverviewItem
    }

    fun fetchUpcomingServices(): LiveData<List<UpcomingServiceItem>> {
        val patientEncounters = MutableLiveData<List<UpcomingServiceItem>>()
        viewModelScope.launch(dispatcher.io()) {
            val listEncounters = ancPatientRepository.fetchCarePlan(patientId = patientId)
            val listEncountersItem =
                ancPatientRepository.fetchUpcomingServiceItem(
                    patientId = patientId,
                    listEncounters
                )
            patientEncounters.postValue(listEncountersItem)
        }
        return patientEncounters
    }

    fun fetchLastSeen(): LiveData<List<UpcomingServiceItem>> {
        val patientEncounters = MutableLiveData<List<UpcomingServiceItem>>()
        viewModelScope.launch(dispatcher.io()) {
            val listEncounters = ancPatientRepository.fetchEncounters(patientId = patientId)
            val listEncountersItem =
                ancPatientRepository.fetchLastSeenItem(
                    patientId = patientId,
                    listEncounters
                )
            patientEncounters.postValue(listEncountersItem)
        }
        return patientEncounters
    }


}
