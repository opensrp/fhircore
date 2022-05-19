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

package org.smartregister.fhircore.eir.ui.adverseevent

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.eir.data.PatientRepository
import org.smartregister.fhircore.eir.ui.patient.details.AdverseEventItem
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DispatcherProvider

@HiltViewModel
class AdverseEventViewModel
@Inject
constructor(
  val defaultRepository: DefaultRepository,
  val patientRepository: PatientRepository,
  val dispatcherProvider: DispatcherProvider
) : ViewModel() {

  fun getPatientImmunizations(patientId: String): LiveData<List<Immunization>> {
    val mutableLiveData: MutableLiveData<List<Immunization>> = MutableLiveData()
    viewModelScope.launch(dispatcherProvider.io()) {
      val immunizations = patientRepository.getPatientImmunizations(patientId = patientId)
      if (!immunizations.isNullOrEmpty()) {
        mutableLiveData.postValue(immunizations)
      } else mutableLiveData.postValue(emptyList())
    }
    return mutableLiveData
  }

  fun getAdverseEvents(
    immunizations: List<Immunization>
  ): LiveData<List<Pair<String, List<AdverseEventItem>>>> {
    val mutableLiveData: MutableLiveData<List<Pair<String, List<AdverseEventItem>>>> =
      MutableLiveData()

    val immunizationAdverseEvents = mutableListOf<Pair<String, List<AdverseEventItem>>>()
    viewModelScope.launch(dispatcherProvider.io()) {
      immunizations.forEach { immunization ->
        immunizationAdverseEvents.add(
          Pair(immunization.idElement.idPart, patientRepository.getAdverseEvents(immunization))
        )
      }
      mutableLiveData.postValue(immunizationAdverseEvents)
    }
    return mutableLiveData
  }

  suspend fun getPopulationResources(intent: Intent): Array<Resource> {
    val resourcesList = mutableListOf<Resource>()

    intent.getStringExtra(AdverseEventQuestionnaireActivity.ADVERSE_EVENT_IMMUNIZATION_ITEM_KEY)
      ?.let { immunizationId ->
        defaultRepository.loadImmunization(immunizationId).run {
          resourcesList.add(this as Immunization)
        }
      }

    return resourcesList.toTypedArray()
  }

  fun loadImmunization(immunizationId: String): LiveData<Immunization?> {
    val mutableLiveData: MutableLiveData<Immunization> = MutableLiveData()
    viewModelScope.launch(dispatcherProvider.io()) {
      val immunization: Immunization? = defaultRepository.loadResource(immunizationId)
      if (immunization != null) {
        mutableLiveData.postValue(immunization)
      } else mutableLiveData.postValue(Immunization())
    }
    return mutableLiveData
  }
}
