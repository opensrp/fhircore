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
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.anc.data.model.PatientVitalItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.ui.anccare.details.EncounterItemMapper
import org.smartregister.fhircore.anc.util.computeBMIViaMetricUnits
import org.smartregister.fhircore.anc.util.computeBMIViaUSCUnits
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
    val patientVitalOverviewItem = MutableLiveData<PatientVitalItem>()
    val patientVitalItem = PatientVitalItem()
    viewModelScope.launch(dispatcher.io()) {
      val listObservationWeight =
        patientRepository.fetchVitalSigns(patientId = patientId, "body-weight")
      val listObservationHeight =
        patientRepository.fetchVitalSigns(patientId = patientId, "body-height")

      listObservationHeight
        .valueQuantity
        ?.value
        ?.setScale(2, BigDecimal.ROUND_HALF_EVEN)
        ?.toPlainString()
        ?.let { patientVitalItem.height = it }

      listObservationWeight
        .valueQuantity
        ?.value
        ?.setScale(2, BigDecimal.ROUND_HALF_EVEN)
        ?.toPlainString()
        ?.let { patientVitalItem.weight = it }

      listObservationHeight.valueQuantity?.unit?.let { patientVitalItem.heightUnit = it }
      listObservationWeight.valueQuantity?.unit?.let { patientVitalItem.weightUnit = it }

      if (patientVitalItem.height.isNotEmpty() &&
          patientVitalItem.weight.isNotEmpty() &&
          patientVitalItem.height.toDouble() > 0 &&
          patientVitalItem.weight.toDouble() > 0
      ) {
        if (patientVitalItem.weightUnit.isNotEmpty()) {
          if (patientVitalItem.weightUnit.equals("kg", true)) {
            patientVitalItem.bmi =
              computeBMIViaMetricUnits(
                  patientVitalItem.height.toDouble(),
                  patientVitalItem.weight.toDouble()
                )
                .toString()
            patientVitalItem.bmiUnit = "kg/m2"
          } else {
            patientVitalItem.bmi =
              computeBMIViaUSCUnits(
                  patientVitalItem.height.toDouble(),
                  patientVitalItem.weight.toDouble()
                )
                .toString()
            patientVitalItem.bmiUnit = "lbs/in2"
          }
        }
      }
      patientVitalOverviewItem.postValue(patientVitalItem)
    }
    return patientVitalOverviewItem
  }
}
