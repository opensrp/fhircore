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
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.anc.data.model.PatientVitalItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.ui.anccare.details.EncounterItemMapper
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

  fun fetchVitalSigns(patientId: String): LiveData<PatientVitalItem> {
    val patientAncOverviewItem = MutableLiveData<PatientVitalItem>()
    val patientVitalItem = PatientVitalItem()
    viewModelScope.launch(dispatcher.io()) {
      val listObservationWeight =
        patientRepository.fetchVitalSigns(patientId = patientId, "body-weight")
      val listObservationHeight =
        patientRepository.fetchVitalSigns(patientId = patientId, "body-height")
      val listObservationBPS = patientRepository.fetchVitalSigns(patientId = patientId, "bp-s")
      val listObservationBPDS = patientRepository.fetchVitalSigns(patientId = patientId, "bp-d")
      val listObservationPulseRate =
        patientRepository.fetchVitalSigns(patientId = patientId, "pulse-rate")
      val listObservationBG = patientRepository.fetchVitalSigns(patientId = patientId, "bg")
      val listObservationsp02 = patientRepository.fetchVitalSigns(patientId = patientId, "sp02")

      if (listObservationWeight.valueQuantity != null &&
          listObservationWeight.valueQuantity.value.toPlainString() != null &&
          listObservationWeight.valueQuantity.unit != null
      )
        patientVitalItem.weight =
          listObservationWeight.valueQuantity.value.toPlainString() +
            " " +
            listObservationWeight.valueQuantity.unit

      if (listObservationHeight.valueQuantity != null &&
          listObservationHeight.valueQuantity.value.toPlainString() != null &&
          listObservationHeight.valueQuantity.unit != null
      )
        patientVitalItem.height =
          listObservationHeight.valueQuantity.value.toPlainString() +
            " " +
            listObservationHeight.valueQuantity.unit

      if (listObservationBPS.valueIntegerType != null &&
          listObservationBPS.valueIntegerType.valueAsString != null
      )
        patientVitalItem.BPS = listObservationBPS.valueIntegerType.valueAsString

      if (listObservationBPDS.valueIntegerType != null &&
          listObservationBPDS.valueIntegerType.valueAsString != null
      )
        patientVitalItem.BPDS = listObservationBPDS.valueIntegerType.valueAsString

      if (listObservationPulseRate.valueIntegerType != null &&
          listObservationPulseRate.valueIntegerType.valueAsString != null
      )
        patientVitalItem.pulse = listObservationPulseRate.valueIntegerType.valueAsString

      if (listObservationBG.valueIntegerType != null &&
          listObservationBG.valueIntegerType.valueAsString != null
      )
        patientVitalItem.BG = listObservationBG.valueIntegerType.valueAsString

      if (listObservationsp02.valueIntegerType != null &&
          listObservationsp02.valueIntegerType.valueAsString != null
      )
        patientVitalItem.sp02 = listObservationsp02.valueIntegerType.valueAsString

      patientAncOverviewItem.postValue(patientVitalItem)
    }
    return patientAncOverviewItem
  }
}
