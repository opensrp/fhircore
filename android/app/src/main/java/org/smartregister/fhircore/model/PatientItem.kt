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

package org.smartregister.fhircore.model

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
  var vaccineSummary: PatientVaccineSummary? = null
) {
  override fun toString(): String = name
}
