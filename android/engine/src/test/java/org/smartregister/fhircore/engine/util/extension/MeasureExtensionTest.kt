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

package org.smartregister.fhircore.engine.util.extension

import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.MeasureReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
class MeasureExtensionTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @Inject lateinit var fhirEngine: FhirEngine

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
  }

  @Test
  fun `findPopulation should return correct population component for group with given type`() {
    val result = measureReport.groupFirstRep.findPopulation(MeasurePopulationType.DENOMINATOR)!!

    assertEquals(2, result.count)
    assertEquals(MeasurePopulationType.DENOMINATOR.toCode(), result.code.codingFirstRep.code)
  }

  @Test
  fun `findPopulation should return correct population component for stratum with given type`() {
    val result =
      measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.findPopulation(
        MeasurePopulationType.DENOMINATOR
      )!!

    assertEquals(4, result.count)
    assertEquals(MeasurePopulationType.DENOMINATOR.toCode(), result.code.codingFirstRep.code)
  }

  @Test
  fun `findRatio should return correct ratio display for group`() {
    val result = measureReport.groupFirstRep.findRatio()

    assertEquals("1/2", result)
  }

  @Test
  fun `findRatio should return correct ratio display for stratum with given denominator`() {
    val result = measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.findRatio(12)

    assertEquals("3/12", result)
  }

  @Test
  fun `findPercentage should return correct percentage for stratum with given denominator`() {
    val result = measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.findPercentage(10)

    assertEquals(30, result)
  }

  @Test
  fun `findPercentage should return zero for stratum when given denominator is zero`() {
    val result = measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.findPercentage(0)

    assertEquals(0, result)
  }

  @Test
  fun `displayText should return capitalized display for stratum when value has text`() {
    measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.value =
      CodeableConcept().apply { text = "stratum 1" }

    val result = measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.displayText

    assertEquals("Stratum 1", result)
  }

  @Test
  fun `displayText should return capitalized display for stratum when value has coding`() {
    measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.value =
      CodeableConcept().apply {
        addCoding(Coding("http://code.org", "stratum-c1", "Stratum Code 1"))
      }

    val result = measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.displayText

    assertEquals("Stratum Code 1", result)
  }

  @Test
  fun `displayText should return NA for stratum when value has no coding and no text`() {
    measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.value = null

    val result = measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.displayText

    assertEquals("N/A", result)
  }

  @Test
  fun `isMonthlyReport should return true when report has coding for monthly report`() {
    measureReport.groupFirstRep.code.addCoding(Coding("http://code.org", "MONTHLY_REPORT", ""))

    val result = measureReport.groupFirstRep.isMonthlyReport()

    assertTrue(result)
  }

  @Test
  fun `isMonthlyReport should return true when report does not have coding for monthly report`() {
    measureReport.groupFirstRep.code.addCoding(Coding("http://code.org", "test code", "other code"))

    val result = measureReport.groupFirstRep.isMonthlyReport()

    assertFalse(result)
  }

  @Test
  fun `reportingPeriodMonthsSpan should return all months falling between given measure period`() {
    measureReport.period.apply {
      this.start = DateType("2021-11-01").value
      this.end = DateType("2022-02-28").value
    }

    val result = measureReport.reportingPeriodMonthsSpan

    assertEquals("Nov-2021", result.elementAt(0))
    assertEquals("Dec-2021", result.elementAt(1))
    assertEquals("Jan-2022", result.elementAt(2))
    assertEquals("Feb-2022", result.elementAt(3))
  }

  @Test
  fun `reportingPeriodMonthsSpan should return correct stratum for given year and month`() {
    val group =
      MeasureReport.MeasureReportGroupComponent().apply {
        this.addStratifier().apply {
          addStratum().apply { this.value = CodeableConcept().apply { text = "2021-Nov" } }
          addStratum().apply { this.value = CodeableConcept().apply { text = "2021-Dec" } }
          addStratum().apply { this.value = CodeableConcept().apply { text = "2022-Jan" } }
        }
      }

    val result = group.findStratumForMonth("Dec-2021")

    assertEquals("2021-Dec", result!!.value.text)
  }

  @Test
  fun `reportingPeriodMonthsSpan should return correct stratum for given month and year`() {
    val group =
      MeasureReport.MeasureReportGroupComponent().apply {
        this.addStratifier().apply {
          addStratum().apply { this.value = CodeableConcept().apply { text = "2021-Nov" } }
          addStratum().apply { this.value = CodeableConcept().apply { text = "2021-Dec" } }
          addStratum().apply { this.value = CodeableConcept().apply { text = "2022-Jan" } }
        }
      }

    val result = group.findStratumForMonth("2021-Dec")

    assertEquals("2021-Dec", result!!.value.text)
  }

  private val measureReport =
    MeasureReport().apply {
      addGroup().apply {
        this.addPopulation().apply {
          this.code.addCoding(
            MeasurePopulationType.NUMERATOR.let { Coding(it.system, it.toCode(), it.display) }
          )
          this.count = 1
        }

        this.addPopulation().apply {
          this.code.addCoding(
            MeasurePopulationType.DENOMINATOR.let { Coding(it.system, it.toCode(), it.display) }
          )
          this.count = 2
        }

        this.addStratifier().addStratum().apply {
          this.addPopulation().apply {
            this.code.addCoding(
              MeasurePopulationType.NUMERATOR.let { Coding(it.system, it.toCode(), it.display) }
            )
            this.count = 3
          }

          this.addPopulation().apply {
            this.code.addCoding(
              MeasurePopulationType.DENOMINATOR.let { Coding(it.system, it.toCode(), it.display) }
            )
            this.count = 4
          }
        }
      }
    }

  @Test
  fun testAlreadyGeneratedMeasureReports() {
    runBlocking {
      val result =
        retrievePreviouslyGeneratedMeasureReports(
          fhirEngine = fhirEngine,
          "2022-02-02",
          "2022-04-04",
          "http://nourl.com"
        )
      assertTrue(result.isNullOrEmpty())
    }
  }
}
