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

package org.smartregister.fhircore.eir.ui.base.model

import org.smartregister.fhircore.eir.util.Utils

/** The Patient's details for display purposes. */
data class PatientItem(
    val id: String,
    val name: String,
    val gender: String,
    val dob: String,
    val html: String,
    val phone: String,
    val logicalId: String,
    val risk: String,
    var vaccineStatus: PatientStatus? = null,
    var vaccineSummary: PatientVaccineSummary? = null,
    val lastSeen: String
) {
  override fun toString(): String = name
}

fun PatientItem.getPatientDemographics(): String {
  val (age, gender) = getPatientAgeGender()
  val names = this.name.split(' ')
  return listOf(names[1], names[0], gender, "$age").joinToString()
}

fun PatientItem.getPatientAgeGender(): PatientAgeGender {
  val age = Utils.getAgeFromDate(this.dob)
  val gender = if (this.gender == "male") 'M' else 'F'
  return PatientAgeGender(age, gender)
}

data class PatientAgeGender(val age: Int, val genderAbbr: Char)

data class PatientStatus(val status: VaccineStatus, val details: String)

data class PatientVaccineSummary(val doseNumber: Int, val initialDose: String)
