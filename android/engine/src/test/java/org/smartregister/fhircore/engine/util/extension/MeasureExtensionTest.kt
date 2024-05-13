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

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.slot
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
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
import org.smartregister.fhircore.engine.configuration.report.measure.ReportConfiguration
import org.smartregister.fhircore.engine.domain.model.RoundingStrategy
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
class MeasureExtensionTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @Inject lateinit var fhirEngine: FhirEngine

  private val measureReport =
    MeasureReport().apply {
      addGroup().apply {
        this.addPopulation().apply {
          this.code.addCoding(
            MeasurePopulationType.NUMERATOR.let { Coding(it.system, it.toCode(), it.display) },
          )
          this.count = 1
        }

        this.addPopulation().apply {
          this.code.addCoding(
            MeasurePopulationType.DENOMINATOR.let { Coding(it.system, it.toCode(), it.display) },
          )
          this.count = 2
        }

        this.addStratifier().addStratum().apply {
          this.addPopulation().apply {
            this.code.addCoding(
              MeasurePopulationType.NUMERATOR.let { Coding(it.system, it.toCode(), it.display) },
            )
            this.count = 3
          }

          this.addPopulation().apply {
            this.code.addCoding(
              MeasurePopulationType.DENOMINATOR.let { Coding(it.system, it.toCode(), it.display) },
            )
            this.count = 4
          }
        }
      }
    }

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
        MeasurePopulationType.DENOMINATOR,
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
  fun `findPercentage should return zero for stratum when given denominator is zero`() {
    val reportConfiguration =
      ReportConfiguration(
        roundingStrategy = RoundingStrategy.ROUND_UP,
      )
    val result =
      measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.findPercentage(
        0,
        reportConfiguration,
      )

    assertEquals("0", result)
  }

  @Test
  fun `findPercentage should return correct percentage for stratum with given denominator and rounding strategy`() {
    val resultTruncate =
      measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.findPercentage(
        9,
        ReportConfiguration(
          roundingStrategy = RoundingStrategy.TRUNCATE,
        ),
      )
    val resultRoundUp =
      measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.findPercentage(
        9,
        ReportConfiguration(
          roundingStrategy = RoundingStrategy.ROUND_UP,
        ),
      )
    val resultRoundOffLower =
      measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.findPercentage(
        9,
        ReportConfiguration(
          roundingStrategy = RoundingStrategy.ROUND_OFF,
        ),
      )
    val resultRoundOffUpper =
      measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.findPercentage(
        8,
        ReportConfiguration(
          roundingStrategy = RoundingStrategy.ROUND_OFF,
        ),
      )

    assertEquals("33", resultTruncate)
    assertEquals("34", resultRoundUp)
    assertEquals("33", resultRoundOffLower)
    assertEquals("38", resultRoundOffUpper)
  }

  @Test
  fun `findPercentage should return correct percentage for stratum with given denominator and rounding precision`() {
    val resultPrecision2Truncate =
      measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.findPercentage(
        9,
        ReportConfiguration(
          roundingStrategy = RoundingStrategy.TRUNCATE,
          roundingPrecision = 2,
        ),
      )
    val resultPrecision2RoundUp =
      measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.findPercentage(
        9,
        ReportConfiguration(
          roundingStrategy = RoundingStrategy.ROUND_UP,
          roundingPrecision = 2,
        ),
      )
    val resultPrecision2RoundOff =
      measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.findPercentage(
        7,
        ReportConfiguration(
          roundingStrategy = RoundingStrategy.ROUND_OFF,
          roundingPrecision = 2,
        ),
      )
    val resultPrecision0RoundUp =
      measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.findPercentage(
        9,
        ReportConfiguration(
          roundingStrategy = RoundingStrategy.ROUND_UP,
        ),
      )

    assertEquals("33.33", resultPrecision2Truncate)
    assertEquals("33.34", resultPrecision2RoundUp)
    assertEquals("42.86", resultPrecision2RoundOff)
    assertEquals("34", resultPrecision0RoundUp)
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

  @Test
  fun testRetrievePreviouslyGeneratedMeasureReportsProducesCorrectSearchObject() {
    runTest {
      val newMeasureReport =
        measureReport.copy().apply {
          id = "new-measure-report"
          period.apply {
            this.start = DateType("2023-08-01").value
            this.end = DateType("2023-08-30").value
          }
          measure = "https://testmeasureurl.com"
        }

      // Create measure to be searched instead of mocking
      fhirEngine.create(newMeasureReport)

      val result =
        fhirEngine.retrievePreviouslyGeneratedMeasureReports(
          startDateFormatted = "2023-08-01",
          endDateFormatted = "2023-08-30",
          measureUrl = "https://testmeasureurl.com",
          subjects = emptyList(),
        )
      assertEquals(1, result.size)
      assertEquals(newMeasureReport.logicalId, result[0].logicalId)

      coVerify { fhirEngine.search<MeasureReport>(capture(slot())) }
    }
  }

  @Test
  fun testAlreadyGeneratedMeasureReports() {
    coEvery { fhirEngine.search<MeasureReport>(any()) } returns emptyList()
    runBlocking {
      val result =
        fhirEngine.retrievePreviouslyGeneratedMeasureReports(
          startDateFormatted = "2022-02-02",
          endDateFormatted = "2022-04-04",
          measureUrl = "http://nourl.com",
          subjects = emptyList(),
        )
      assertTrue(result.isEmpty())
    }
  }
}
