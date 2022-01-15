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

package org.smartregister.fhircore.anc.data.model

import androidx.compose.runtime.Stable
import java.util.Date
import org.hl7.fhir.r4.model.Encounter
import org.smartregister.fhircore.engine.util.extension.toAgeDisplay
import org.smartregister.fhircore.engine.util.extension.yearsPassed

enum class VisitStatus {
  DUE,
  OVERDUE,
  PLANNED
}

@Stable
data class PatientItem(
  val patientIdentifier: String = "",
  val name: String = "",
  val familyName: String = "",
  val gender: String = "",
  val birthDate: Date? = null,
  val atRisk: String = "",
  val address: String = "",
  val isPregnant: Boolean? = null,
  val visitStatus: VisitStatus = VisitStatus.PLANNED,
  val isHouseHoldHead: Boolean? = null,
  val headId: String? = null
)

fun PatientItem.demographics() = "$name, $gender, ${birthDate.toAgeDisplay()}"

fun PatientItem.nonPregnantEligibleWoman() = this.isPregnant != true && this.gender.startsWith("F")

fun PatientItem.eligibleWoman() =
  this.gender.startsWith("F") && this.birthDate?.let { it.yearsPassed() > 10 } ?: true

@Stable
data class PatientDetailItem(
  val patientDetails: PatientItem = PatientItem(),
  val patientDetailsHead: PatientItem = PatientItem(),
)

@Stable
data class CarePlanItem(
  val carePlanIdentifier: String = "",
  val title: String = "",
  val due: Boolean,
  val overdue: Boolean
)

@Stable
data class UpcomingServiceItem(
  val encounterIdentifier: String = "",
  val title: String = "",
  val date: String = ""
)

@Stable
data class EncounterItem(
  val id: String = "",
  val status: Encounter.EncounterStatus,
  val display: String = "",
  val periodStartDate: Date?
)

@Stable data class AllergiesItem(val allergiesIdentifier: String = "", val title: String = "")

@Stable data class ConditionItem(val conditionIdentifier: String = "", val title: String = "")

@Stable
data class PatientBmiItem(
  var patientIdentifier: String = "",
  var name: String = "",
  var height: String = "",
  var weight: String = "",
  var bmi: String = ""
)

@Stable
data class PatientVitalItem(
  var weight: String = "",
  var weightUnit: String = "",
  var height: String = "",
  var heightUnit: String = "",
  var spO2: String = "",
  var spO2Unit: String = "",
  var bg: String = "",
  var bgUnit: String = "",
  var bps: String = "",
  var bpsUnit: String = "",
  var bpds: String = "",
  var bpdsUnit: String = "",
  var pulse: String = "",
  var pulseUnit: String = "",
  var bmi: String = "",
  var bmiUnit: String = ""
) {
  fun isValidWeightAndHeight(): Boolean {
    return weight.isNotEmpty() &&
      height.isNotEmpty() &&
      weight.toDouble() > 0 &&
      height.toDouble() > 0
  }

  fun isWeightAndHeightAreInMetricUnit(): Boolean {
    return weightUnit.equals(UnitConstants.UNIT_WEIGHT_METRIC, true) &&
      heightUnit.equals(UnitConstants.UNIT_HEIGHT_METRIC, true)
  }

  fun isWeightAndHeightAreInUscUnit(): Boolean {
    return weightUnit.equals(UnitConstants.UNIT_WEIGHT_USC, true) &&
      heightUnit.equals(UnitConstants.UNIT_HEIGHT_USC, true)
  }
}

object UnitConstants {
  const val UNIT_WEIGHT_METRIC = "kg"
  const val UNIT_HEIGHT_METRIC = "cm"
  const val UNIT_CODE_WEIGHT_METRIC = "kg"
  const val UNIT_CODE_HEIGHT_METRIC = "cm"
  const val UNIT_BMI_METRIC = "kg/m2"
  const val UNIT_WEIGHT_USC = "lb"
  const val UNIT_HEIGHT_USC = "in"
  const val UNIT_CODE_WEIGHT_USC = "[lb_av]"
  const val UNIT_CODE_HEIGHT_USC = "[in_i]"
  const val UNIT_BMI_USC = "kg/m2" // can be set as lb/in2
}
