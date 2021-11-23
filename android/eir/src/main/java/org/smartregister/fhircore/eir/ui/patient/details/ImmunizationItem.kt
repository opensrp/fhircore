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
import org.smartregister.fhircore.engine.util.extension.plusDaysAsString

const val VACCINE_GAP_DAYS: Int = 21
const val OVERDUE_GAP_DAYS: Int = VACCINE_GAP_DAYS + 1

data class ImmunizationItem(val vaccine: String, val doses: List<Pair<String, Int>>)

data class ImmunizationAdverseEventItem(
  var immunizationIds: List<String>,
  var vaccine: String,
  var dosesWithAdverseEvents: List<Pair<String, List<AdverseEventItem>>>
)

data class AdverseEventItem(val date: String, val detail: String)

fun List<Immunization>.toImmunizationItems(context: Context): MutableList<ImmunizationItem> {
  if (this.isEmpty()) return mutableListOf()
  val immunizationItems = mutableListOf<ImmunizationItem>()
  val immunizationsMap = this.groupBy { it.vaccineCode.codingFirstRep.code }

  immunizationsMap.forEach { vaccine ->
    val doses: List<Pair<String, Int>> =
      vaccine.value.sortedBy { it.protocolAppliedFirstRep.doseNumberPositiveIntType.value }.map {
        it.getDoseLabel(context, this.size >= 2)
      }
    immunizationItems.add(ImmunizationItem(vaccine.key, doses = doses))
  }

  return immunizationItems
}

fun Immunization.getDoseLabel(context: Context, fullyImmunized: Boolean): Pair<String, Int> {
  val doseNumber = this.protocolAppliedFirstRep.doseNumberPositiveIntType?.value ?: 0
  val vaccineDate = this.occurrenceDateTimeType

  if (fullyImmunized) {
    return Pair(
      context.getString(
        R.string.immunization_given,
        doseNumber.ordinalOf(),
        vaccineDate?.toHumanDisplay() ?: "-"
      ),
      R.color.black
    )
  } else {
    val isOverDue = this.isOverdue()

    val dueDate = this.dueDateFmt()
    val nextDoseNumber = doseNumber + 1
    val doseLabel =
      if (!isOverDue)
        context.getString(R.string.immunization_due, nextDoseNumber.ordinalOf(), dueDate)
      else context.getString(R.string.immunization_overdue, nextDoseNumber.ordinalOf(), dueDate)

    return Pair(doseLabel, if (fullyImmunized) R.color.black else R.color.not_immune)
  }
}

fun List<Immunization>.toImmunizationAdverseEventItem(
  context: Context,
  immunizationAdverseEvents: List<Pair<String, List<AdverseEventItem>>>? = null
): MutableList<ImmunizationAdverseEventItem> {
  if (this.isEmpty()) return mutableListOf()
  val immunizationAdverseEventItems = mutableListOf<ImmunizationAdverseEventItem>()
  val immunizationsMap = this.groupBy { it.vaccineCode.text }

  immunizationsMap.forEach { vaccine ->
    val doses: List<Pair<String, List<AdverseEventItem>>> =
      vaccine.value.sortedBy { it.protocolApplied.first().doseNumberPositiveIntType.value }.map {
        immunization ->
        immunization.getDoseLabelWithAdverseEvent(
          context,
          this.isNotEmpty(),
          immunizationAdverseEvents?.filter { it.first == immunization.idElement.idPart }
        )
      }

    val immunizationIds: List<String> =
      vaccine.value.sortedBy { it.protocolApplied.first().doseNumberPositiveIntType.value }.map {
        it.idElement.idPart
      }

    immunizationAdverseEventItems.add(
      ImmunizationAdverseEventItem(immunizationIds, vaccine.key, doses)
    )
  }
  return immunizationAdverseEventItems
}

fun Immunization.getDoseLabelWithAdverseEvent(
  context: Context,
  receivedDoses: Boolean = true,
  immunizationAdverseEvents: List<Pair<String, List<AdverseEventItem>>>? = null
): Pair<String, List<AdverseEventItem>> {
  val doseNumber = (this.protocolApplied[0].doseNumber as PositiveIntType).value
  return if (receivedDoses) {
    Pair(
      context.getString(
        R.string.immunization_given,
        doseNumber.ordinalOf(),
        this.occurrenceDateTimeType.toHumanDisplay()
      ),
      when (immunizationAdverseEvents) {
        null -> listOf()
        else -> immunizationAdverseEvents.first().second
      }
    )
  } else {
    Pair("", listOf())
  }
}

fun Immunization.isOverdue() =
  if (!this.occurrenceDateTimeType.hasValue()) false
  else DateUtils.hasPastDays(this.occurrenceDateTimeType, OVERDUE_GAP_DAYS)

fun Immunization.dueDateFmt() =
  if (!this.occurrenceDateTimeType.hasValue()) ""
  else this.occurrenceDateTimeType.plusDaysAsString(VACCINE_GAP_DAYS)

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
