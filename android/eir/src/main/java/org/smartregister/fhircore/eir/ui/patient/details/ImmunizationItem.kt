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

package org.smartregister.fhircore.eir.ui.patient.details

import android.content.Context
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.PositiveIntType
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.engine.util.DateUtils

const val DAYS_IN_MONTH: Int = 28
const val OVERDUE_DAYS_IN_MONTH: Int = 14

data class ImmunizationItem(val vaccine: String, val doses: List<Pair<String, Int>>)

fun List<Immunization>.toImmunizationItems(context: Context): MutableList<ImmunizationItem> {
  if (this.isEmpty()) return mutableListOf()
  val immunizationItems = mutableListOf<ImmunizationItem>()
  val immunizationsMap = this.groupBy { it.vaccineCode.text }

  immunizationsMap.forEach { vaccine ->
    val doses: List<Pair<String, Int>> =
      vaccine.value.sortedBy { it.protocolApplied.first().doseNumberPositiveIntType.value }.map {
        it.getDoseLabel(context, this.size >= 2)
      }
    immunizationItems.add(ImmunizationItem(vaccine.key, doses = doses))
  }

  return immunizationItems
}

fun Immunization.getDoseLabel(context: Context, fullyImmunized: Boolean): Pair<String, Int> {
  val doseNumber = (this.protocolApplied[0].doseNumber as PositiveIntType).value
  if (fullyImmunized) {
    return Pair(
      context.getString(
        R.string.immunization_given,
        doseNumber.ordinalOf(),
        this.occurrenceDateTimeType.toHumanDisplay()
      ),
      R.color.black
    )
  } else {
    val isDue =
      DateUtils.hasPastDays(this.occurrenceDateTimeType, DAYS_IN_MONTH + OVERDUE_DAYS_IN_MONTH)

    val dueDate = DateUtils.addDays(this.occurrenceDateTimeType.toHumanDisplay(), DAYS_IN_MONTH)
    val nextDoseNumber = doseNumber + 1
    val doseLabel =
      if (isDue) context.getString(R.string.immunization_due, nextDoseNumber.ordinalOf(), dueDate)
      else context.getString(R.string.immunization_overdue, nextDoseNumber.ordinalOf(), dueDate)

    return Pair(doseLabel, if (fullyImmunized) R.color.black else R.color.not_immune)
  }
}

fun Int.ordinalOf() =
  "$this" +
    if (this % 100 in 11..13) "th"
    else
      when (this % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
      }
