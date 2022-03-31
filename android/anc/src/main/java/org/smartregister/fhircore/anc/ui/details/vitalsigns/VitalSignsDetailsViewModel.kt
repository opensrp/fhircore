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
import org.hl7.fhir.r4.model.Observation
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.anc.data.model.PatientVitalItem
import org.smartregister.fhircore.anc.data.model.UnitConstants
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.ui.anccare.details.EncounterItemMapper
import org.smartregister.fhircore.anc.util.computeBmiViaMetricUnits
import org.smartregister.fhircore.anc.util.computeBmiViaUscUnits
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
          EncounterItemMapper.transformInputToOutputModel(it)
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
      val listObservationBps = patientRepository.fetchVitalSigns(patientId = patientId, "bp-s")
      val listObservationBpds = patientRepository.fetchVitalSigns(patientId = patientId, "bp-d")
      val listObservationPulseRate =
        patientRepository.fetchVitalSigns(patientId = patientId, "pulse-rate")
      val listObservationBg = patientRepository.fetchVitalSigns(patientId = patientId, "bg")
      val listObservationspO2 = patientRepository.fetchVitalSigns(patientId = patientId, "spO2")

      patientVitalItem.weight = observationValueOrDefault(listObservationWeight)
      patientVitalItem.weightUnit = listObservationWeight.valueQuantity.unit ?: ""

      patientVitalItem.height = observationValueOrDefault(listObservationHeight)
      patientVitalItem.heightUnit = listObservationHeight.valueQuantity.unit ?: ""

      patientVitalItem.bps = observationValueOrDefault(listObservationBps)
      patientVitalItem.bpsUnit = listObservationBps.valueQuantity.unit ?: ""

      patientVitalItem.bpds = observationValueOrDefault(listObservationBpds)
      patientVitalItem.bpdsUnit = listObservationBpds.valueQuantity.unit ?: ""

      patientVitalItem.pulse = observationValueOrDefault(listObservationPulseRate)
      patientVitalItem.pulseUnit = listObservationPulseRate.valueQuantity.unit ?: ""

      patientVitalItem.bg = observationValueOrDefault(listObservationBg)
      patientVitalItem.bgUnit = listObservationBg.valueQuantity.unit ?: ""

      patientVitalItem.spO2 = observationValueOrDefault(listObservationspO2)
      patientVitalItem.spO2Unit = listObservationspO2.valueQuantity.unit ?: ""

      // Todo: confirm if BMI can be displayed from
      //  Add-Vitals height/weight if its of same Units
      if (patientVitalItem.bmi.isEmpty() && patientVitalItem.isValidWeightAndHeight()) {
        when {
          patientVitalItem.isWeightAndHeightAreInMetricUnit() -> {
            patientVitalItem.bmi =
              computeBmiViaMetricUnits(
                  patientVitalItem.height.toDouble(),
                  patientVitalItem.weight.toDouble()
                )
                .toString()
            patientVitalItem.bmiUnit = UnitConstants.UNIT_BMI_METRIC
          }
          patientVitalItem.isWeightAndHeightAreInUscUnit() -> {
            patientVitalItem.bmi =
              computeBmiViaUscUnits(
                  patientVitalItem.height.toDouble(),
                  patientVitalItem.weight.toDouble()
                )
                .toString()
            // Todo: confirm if bmi unit can be displayed in lb/in2 or only in kg/m2
            patientVitalItem.bmiUnit = UnitConstants.UNIT_BMI_USC
          }
          else -> {
            patientVitalItem.bmi = "N/A"
          }
        }
      }

      patientAncOverviewItem.postValue(patientVitalItem)
    }
    return patientAncOverviewItem
  }

  private fun observationValueOrDefault(
    observation: Observation,
    defaultString: String = ""
  ): String {
    return if (observation.valueQuantity != null && observation.valueQuantity.value != null)
      observation.valueQuantity.value.toPlainString()
    else defaultString
  }
}
