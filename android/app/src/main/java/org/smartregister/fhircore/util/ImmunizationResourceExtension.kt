package org.smartregister.fhircore.util

import android.content.Context
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.PositiveIntType
import org.smartregister.fhircore.R
import org.smartregister.fhircore.model.ImmunizationItem
import org.smartregister.fhircore.util.Utils.ordinalOf

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
