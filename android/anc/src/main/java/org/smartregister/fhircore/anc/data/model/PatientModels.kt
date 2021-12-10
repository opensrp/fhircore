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

enum class VisitStatus {
  DUE,
  OVERDUE,
  PLANNED
}

@Stable
data class PatientItem(
  val patientIdentifier: String = "",
  val name: String = "",
  val gender: String = "",
  val age: String = "",
  val demographics: String = "",
  val atRisk: String = "",
  val address: String = "",
  val isPregnant: Boolean = true,
  val visitStatus: VisitStatus = VisitStatus.PLANNED,
  val familyName: String = "",
  val isHouseHoldHead: Boolean = false
)

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
  var height: String = "",
  var sp02: String = "",
  var BG: String = "",
  var BPS: String = "",
  var BPDS: String = "",
  var pulse: String = ""
)
