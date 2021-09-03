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

package org.smartregister.fhircore.eir.data.model

import androidx.compose.runtime.Stable

@Stable
data class PatientItem(
  val patientIdentifier: String = "",
  val name: String = "",
  val gender: String = "",
  val age: String = "",
  val demographics: String = "",
  val lastSeen: String = "",
  val vaccineStatus: PatientVaccineStatus = PatientVaccineStatus(VaccineStatus.NOT_VACCINATED, ""),
  val atRisk: String = ""
)

enum class VaccineStatus {
  VACCINATED,
  PARTIAL,
  OVERDUE,
  DUE,
  NOT_VACCINATED
}

data class PatientVaccineStatus(val status: VaccineStatus, val date: String)
