/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.util.extension

import ca.uhn.fhir.rest.param.ParamPrefixEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Operation
import com.google.android.fhir.search.Search
import org.apache.commons.lang3.StringUtils
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.MeasureReport
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.report.measure.ReportConfiguration
import org.smartregister.fhircore.engine.configuration.report.measure.ReportConfiguration.Companion.DEFAULT_ROUNDING_PRECISION
import org.smartregister.fhircore.engine.configuration.report.measure.ReportConfiguration.Companion.DEFAULT_ROUNDING_STRATEGY
import org.smartregister.p2p.utils.capitalize

// TODO: Enhancement - use FhirPathEngine evaluator for data extraction

enum class MeasurePopulationType(
  private val code: String,
) {
  INITIALPOPULATION("initial-population"),
  NUMERATOR("numerator"),
  DENOMINATOR("denominator"),
  ;

  val system: String
    get() = "http://hl7.org/fhir/measure-population"

  val display: String
    get() = code.capitalize()

  fun toCode(): String {
    return this.code
  }
}

fun MeasureReport.StratifierGroupComponent.findPopulation(
  id: MeasurePopulationType,
): MeasureReport.StratifierGroupPopulationComponent? {
  return this.population.find { it.id == id.toCode() || it.code.codingFirstRep.code == id.toCode() }
}

fun MeasureReport.StratifierGroupComponent.findCount():
  MeasureReport.StratifierGroupPopulationComponent? {
  return this.findPopulation(MeasurePopulationType.NUMERATOR)
    ?: this.findPopulation(MeasurePopulationType.INITIALPOPULATION)
}

fun MeasureReport.MeasureReportGroupComponent.findPopulation(
  id: MeasurePopulationType,
): MeasureReport.MeasureReportGroupPopulationComponent? {
  return this.population.find { it.id == id.toCode() || it.code.codingFirstRep.code == id.toCode() }
}

fun MeasureReport.MeasureReportGroupComponent.findCount():
  MeasureReport.MeasureReportGroupPopulationComponent? {
  return this.findPopulation(MeasurePopulationType.NUMERATOR)
    ?: this.findPopulation(MeasurePopulationType.INITIALPOPULATION)
}

fun MeasureReport.MeasureReportGroupComponent.isMonthlyReport(): Boolean {
  return this.code.coding.any { it.code.contains("month", true) }
}

fun MeasureReport.MeasureReportGroupComponent.findRatio(): String {
  return "${this.findPopulation(MeasurePopulationType.NUMERATOR)?.count}/${this.findPopulation(MeasurePopulationType.DENOMINATOR)?.count}"
}

fun MeasureReport.StratifierGroupComponent.findRatio(denominator: Int?): String {
  return "${this.findCount()?.count}/$denominator"
}

fun MeasureReport.StratifierGroupComponent.findPercentage(
  denominator: Int,
  reportConfiguration: ReportConfiguration?,
): String {
  return if (denominator == 0) {
    "0"
  } else {
    findPopulation(MeasurePopulationType.NUMERATOR)
      ?.count
      ?.toBigDecimal()
      ?.times(100.toBigDecimal())
      ?.divide(
        denominator.toBigDecimal(),
        reportConfiguration?.roundingPrecision ?: DEFAULT_ROUNDING_PRECISION,
        reportConfiguration?.roundingStrategy?.value ?: DEFAULT_ROUNDING_STRATEGY.value,
      )
      .toString()
  }
}

val MeasureReport.StratifierGroupComponent.displayText
  get() =
    when {
      this.value.hasText() -> StringUtils.capitalize(this.value.text)
      this.value.hasCoding() -> this.value.codingFirstRep.display
      else -> "N/A"
    }

/**
 * Returns a list of month-year for for all months falling in given measure period Example: Jan-2021
 * -> Apr-2021 = [(Jan-2021), (Feb-2021), (Mar-2021), (Apr-2021)]
 */
val MeasureReport.reportingPeriodMonthsSpan
  get() =
    this.period.let {
      val yearMonths = mutableListOf<String>()
      var currentDate = it.copy().start.firstDayOfMonth()

      while (currentDate.before(it.end)) {
        yearMonths.add(currentDate.formatDate(SDF_MMM_YYYY))
        currentDate = currentDate.plusMonths(1)
      }
      yearMonths.toList()
    }

fun MeasureReport.MeasureReportGroupComponent.findStratumForMonth(reportingMonth: String) =
  this.stratifier
    .flatMap { it.stratum }
    .find { it.hasValue() && it.value.text.compare(reportingMonth) }

/**
 * @param startDateFormatted
 * @param endDateFormatted
 * @param measureUrl
 * @param fhirEngine suspend inline fun<reified R: Resource> resourceExists(startDate: Date,
 *   endDate: Date, operation: Operation = Operation.AND)
 * @return list of already generatedMeasureReports
 */
suspend inline fun FhirEngine.retrievePreviouslyGeneratedMeasureReports(
  startDateFormatted: String,
  endDateFormatted: String,
  measureUrl: String,
  subjects: List<String>,
): List<MeasureReport> {
  val search = Search(ResourceType.MeasureReport)
  search.filter(
    MeasureReport.PERIOD,
    {
      value = of(DateTimeType(startDateFormatted))
      prefix = ParamPrefixEnum.GREATERTHAN_OR_EQUALS
    },
    {
      value = of(DateTimeType(endDateFormatted))
      prefix = ParamPrefixEnum.LESSTHAN_OR_EQUALS
    },
    operation = Operation.AND,
  )
  search.filter(MeasureReport.MEASURE, { value = measureUrl })
  subjects.forEach { search.filter(MeasureReport.SUBJECT, { value = it }) }

  return this.batchedSearch<MeasureReport>(search).map { it.resource }
}
