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

package org.smartregister.fhircore.fragment

import android.content.Context
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PositiveIntType
import org.smartregister.fhircore.R
import org.smartregister.fhircore.util.Utils
import org.smartregister.fhircore.util.Utils.makeItReadable

private const val DAYS_IN_MONTH: Int = 28
private const val OVERDUE_DAYS_IN_MONTH: Int = 14

/** * A wrapper class that displays a patient's historical activity */
data class PatientDetailsCard(
  val groupIndex: Int,
  val index: Int,
  val id: String,
  val type: String,
  val title: String,
  val details: String
)

fun Patient.toDetailsCard(context: Context, index: Int = 0) =
  PatientDetailsCard(
    groupIndex = 0,
    index = index,
    id = this.id,
    type = this.resourceType.name,
    title =
      "${context.getString(R.string.registered_date)} ${this.meta?.lastUpdated?.makeItReadable()}",
    context.getString(R.string.view_registration_details)
  )

fun Immunization.toDetailsCard(context: Context, index: Int = 0, hasNext: Boolean = false) =
  PatientDetailsCard(
    groupIndex = 1,
    index = index,
    id = this.id,
    type = this.resourceType.name,
    title =
      context.getString(
        R.string.immunization_brief_text,
        this.vaccineCode.text,
        (this.protocolApplied[0].doseNumber as PositiveIntType).value
      ),
    if (hasNext) {
      context.getString(
        getDetailsText(this.occurrenceDateTimeType),
        ((this.protocolApplied[0].doseNumber as PositiveIntType).value + 1),
        Utils.addDays(this.occurrenceDateTimeType.toHumanDisplay(), DAYS_IN_MONTH)
      )
    } else context.getString(R.string.fully_vaccinated)
  )

private fun getDetailsText(previousVaccineDatetime: DateTimeType): Int {
  val isOverDue = Utils.hasPastDays(previousVaccineDatetime, DAYS_IN_MONTH + OVERDUE_DAYS_IN_MONTH)
  return if (isOverDue) R.string.immunization_next_dose_text
  else R.string.immunization_next_overdue_dose_text
}
