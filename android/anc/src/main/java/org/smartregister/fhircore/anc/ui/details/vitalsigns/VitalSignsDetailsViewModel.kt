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

package org.smartregister.fhircore.anc.ui.details.vitalsigns

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.smartregister.fhircore.anc.data.model.AncOverviewItem
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.ui.anccare.details.EncounterItemMapper
import org.smartregister.fhircore.anc.util.computeBMIViaStandardUnits
import org.smartregister.fhircore.engine.util.DispatcherProvider

@HiltViewModel
class VitalSignsDetailsViewModel
@Inject
constructor(val patientRepository: PatientRepository, var dispatcher: DispatcherProvider) :
  ViewModel() {

  fun fetchEncounters(patientId: String): LiveData<List<EncounterItem>> {
    val patientEncounters = MutableLiveData<List<EncounterItem>>()
    viewModelScope.launch(dispatcher.io()) {
      val listEncounters =
        patientRepository.fetchEncounters(patientId = patientId).map {
          EncounterItemMapper.mapToDomainModel(it)
        }
      patientEncounters.postValue(listEncounters)
    }
    return patientEncounters
  }

  fun fetchObservation(patientId: String): LiveData<AncOverviewItem> {
    val patientAncOverviewItem = MutableLiveData<AncOverviewItem>()
    val ancOverviewItem = AncOverviewItem()
    viewModelScope.launch(dispatcher.io()) {
      val listObservationHeight =
        patientRepository.fetchObservations(patientId = patientId, "body-height")
      val listObservationWeight =
        patientRepository.fetchObservations(patientId = patientId, "body-weight")
      listObservationWeight
        .valueQuantity
        ?.value
        ?.setScale(2, BigDecimal.ROUND_HALF_EVEN)
        ?.toPlainString()
        ?.let { ancOverviewItem.weight = it }
      listObservationHeight
        .valueQuantity
        ?.value
        ?.setScale(2, BigDecimal.ROUND_HALF_EVEN)
        ?.toPlainString()
        ?.let { ancOverviewItem.height = it }
      if (ancOverviewItem.height.isNotEmpty() && ancOverviewItem.weight.isNotEmpty()) {
        ancOverviewItem.bmi =
          computeBMIViaStandardUnits(
              ancOverviewItem.height.toDouble(),
              ancOverviewItem.weight.toDouble()
            )
            .toString()
      }

      patientAncOverviewItem.postValue(ancOverviewItem)
    }
    return patientAncOverviewItem
  }
}
