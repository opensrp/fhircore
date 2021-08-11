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

package org.smartregister.fhircore.eir.util

import android.content.Context
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.PositiveIntType
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.model.ImmunizationItem
import org.smartregister.fhircore.eir.util.Utils.ordinalOf

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
      Utils.hasPastDays(this.occurrenceDateTimeType, DAYS_IN_MONTH + OVERDUE_DAYS_IN_MONTH)

    val dueDate = Utils.addDays(this.occurrenceDateTimeType.toHumanDisplay(), DAYS_IN_MONTH)
    val nextDoseNumber = doseNumber + 1
    val doseLabel =
      if (isDue) context.getString(R.string.immunization_due, nextDoseNumber.ordinalOf(), dueDate)
      else context.getString(R.string.immunization_overdue, nextDoseNumber.ordinalOf(), dueDate)

    return Pair(doseLabel, if (fullyImmunized) R.color.black else R.color.not_immune)
  }
}
