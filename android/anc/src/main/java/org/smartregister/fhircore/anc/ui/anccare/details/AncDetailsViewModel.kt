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
import ca.uhn.fhir.parser.IParser
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.smartregister.fhircore.anc.data.anc.model.AncOverviewItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.data.model.AncPatientDetailItem
import org.smartregister.fhircore.anc.data.model.CarePlanItem
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.anc.data.model.UpcomingServiceItem
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.util.DateUtils.makeItReadable
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider

class AncDetailsViewModel(
  val patientRepository: PatientRepository,
  var dispatcher: DispatcherProvider = DefaultDispatcherProvider,
  val patientId: String
) : ViewModel() {

  lateinit var patientDemographics: MutableLiveData<AncPatientDetailItem>

  fun fetchDemographics(): LiveData<AncPatientDetailItem> {
    patientDemographics = MutableLiveData<AncPatientDetailItem>()
    viewModelScope.launch(dispatcher.io()) {
      val ancPatientDetailItem = patientRepository.fetchDemographics(patientId = patientId)
      patientDemographics.postValue(ancPatientDetailItem)
    }
    return patientDemographics
  }

  fun fetchCarePlan(): LiveData<List<CarePlanItem>> {
    val patientCarePlan = MutableLiveData<List<CarePlanItem>>()
    viewModelScope.launch(dispatcher.io()) {
      val listCarePlan = patientRepository.searchCarePlan(id = patientId)
      val listCarePlanItem = patientRepository.fetchCarePlanItem(listCarePlan)
      patientCarePlan.postValue(listCarePlanItem)
    }
    return patientCarePlan
  }

  fun fetchObservation(): LiveData<AncOverviewItem> {
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

  fun fetchUpcomingServices(): LiveData<List<UpcomingServiceItem>> {
    val patientEncounters = MutableLiveData<List<UpcomingServiceItem>>()
    viewModelScope.launch(dispatcher.io()) {
      val listEncounters = patientRepository.fetchCarePlan(patientId = patientId)
      val listEncountersItem = patientRepository.fetchUpcomingServiceItem(listEncounters)
      patientEncounters.postValue(listEncountersItem)
    }
    return patientEncounters
  }

  fun fetchLastSeen(): LiveData<List<EncounterItem>> {
    val patientEncounters = MutableLiveData<List<EncounterItem>>()
    viewModelScope.launch(dispatcher.io()) {
      val listEncounters = patientRepository.fetchEncounters(patientId = patientId)
      val listEncountersItem = patientRepository.fetchLastSeenItem(listEncounters)
      patientEncounters.postValue(listEncountersItem)
    }
    return patientEncounters
  }

  fun fetchCQLLibraryData(
    parser: IParser,
    fhirResourceDataSource: FhirResourceDataSource,
    libraryURL: String
  ): LiveData<String> {
    var libraryData = MutableLiveData<String>()
    viewModelScope.launch(dispatcher.io()) {
      val auxCQLLibraryData =
        parser.encodeResourceToString(fhirResourceDataSource.loadData(libraryURL).entry[0].resource)
      libraryData.postValue(auxCQLLibraryData)
    }
    return libraryData
  }

  fun fetchCQLFhirHelperData(
    parser: IParser,
    fhirResourceDataSource: FhirResourceDataSource,
    helperURL: String
  ): LiveData<String> {
    var helperData = MutableLiveData<String>()
    viewModelScope.launch(dispatcher.io()) {
      val auxCQLHelperData =
        parser.encodeResourceToString(fhirResourceDataSource.loadData(helperURL).entry[0].resource)
      helperData.postValue(auxCQLHelperData)
    }
    return helperData
  }

  fun fetchCQLValueSetData(
    parser: IParser,
    fhirResourceDataSource: FhirResourceDataSource,
    valueSetURL: String
  ): LiveData<String> {
    var valueSetData = MutableLiveData<String>()
    viewModelScope.launch(dispatcher.io()) {
      val auxCQLValueSetData =
        parser.encodeResourceToString(fhirResourceDataSource.loadData(valueSetURL))
      valueSetData.postValue(auxCQLValueSetData)
    }
    return valueSetData
  }

  fun fetchCQLPatientData(
    parser: IParser,
    fhirResourceDataSource: FhirResourceDataSource,
    patientURL: String
  ): LiveData<String> {
    var patientData = MutableLiveData<String>()
    viewModelScope.launch(dispatcher.io()) {
      val auxCQLPatientData =
        parser.encodeResourceToString(fhirResourceDataSource.loadData(patientURL))
      patientData.postValue(auxCQLPatientData)
    }
    return patientData
  }

  fun fetchCQLMeasureEvaluateLibraryAndValueSets(
    parser: IParser,
    fhirResourceDataSource: FhirResourceDataSource,
    libAndValueSetURL: String,
    measureURL: String,
    cqlMeasureReportLibInitialString: String
  ): LiveData<String> {
    var valueSetData = MutableLiveData<String>()
    val equalsIndexUrl: Int = libAndValueSetURL.indexOf("=")

    var libStrAfterEquals = libAndValueSetURL.substring(libAndValueSetURL.lastIndexOf("=") + 1)
    var libList = libStrAfterEquals.split(",").map { it.trim() }

    var libURLStrBeforeEquals = libAndValueSetURL.substring(0, equalsIndexUrl) + "="
    var initialStr = cqlMeasureReportLibInitialString
    viewModelScope.launch(dispatcher.io()) {
      val measureObject =
        parser.encodeResourceToString(fhirResourceDataSource.loadData(measureURL).entry[0].resource)
      var jsonObjectResource = JSONObject()
      var jsonObjectResourceType = JSONObject(measureObject)
      jsonObjectResource.put("resource", jsonObjectResourceType)
      initialStr += jsonObjectResource

      for (lib in libList) {
        val auxCQLValueSetData =
          parser.encodeResourceToString(
            fhirResourceDataSource.loadData(libURLStrBeforeEquals + lib).entry[0].resource
          )
        var jsonObjectResource = JSONObject()
        var jsonObjectResourceType = JSONObject(auxCQLValueSetData)
        jsonObjectResource.put("resource", jsonObjectResourceType)

        initialStr = "$initialStr,$jsonObjectResource"
      }
      var sb = StringBuffer(initialStr)
      sb.deleteCharAt(sb.length - 1)
      sb.append("}]}")
      valueSetData.postValue(sb.toString())
    }
    return valueSetData
  }
}
