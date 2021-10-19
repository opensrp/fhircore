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
data class AncPatientItem(
  var patientIdentifier: String = "",
  var name: String = "",
  var gender: String = "",
  var age: String = "",
  var demographics: String = "",
  var atRisk: String = "",
  val address: String = "",
  val visitStatus: VisitStatus = VisitStatus.PLANNED
)

@Stable
data class AncPatientDetailItem(
  var patientDetails: AncPatientItem = AncPatientItem(),
  var patientDetailsHead: AncPatientItem = AncPatientItem(),
)

@Stable
data class CarePlanItem(
  var carePlanIdentifier: String = "",
  var title: String = "",
  var due: Boolean,
  var overdue: Boolean
)

@Stable
data class UpcomingServiceItem(
  var encounterIdentifier: String = "",
  var title: String = "",
  var date: String = ""
)

@Stable
data class EncounterItem(
  val id: String = "",
  val status: Encounter.EncounterStatus,
  val display: String = "",
  val periodStartDate: Date = Date()
)

@Stable data class AllergiesItem(var allergiesIdentifier: String = "", var title: String = "")

@Stable data class ConditionItem(var conditionIdentifier: String = "", var title: String = "")
