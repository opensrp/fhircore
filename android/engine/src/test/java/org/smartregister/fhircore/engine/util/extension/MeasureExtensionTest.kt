/*
 * Copyright 2021-2023 Ona Systems, Inc
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
import com.google.android.fhir.search.Search
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.MeasureReport
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType
import org.smartregister.fhircore.engine.domain.model.RoundingStrategy
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
class MeasureExtensionTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)
  private var fhirEngine = mockk<FhirEngine>()

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
  fun `findPercentage should return zero for stratum when given denominator is zero`() {
    val result =
      measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.findPercentage(
        0,
        RoundingStrategy.ROUND_UP,
        0
      )

    assertEquals("0", result)
  }

  @Test
  fun `findPercentage should return correct percentage for stratum with given denominator and rounding strategy`() {
    val resultTruncate =
      measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.findPercentage(
        9,
        RoundingStrategy.TRUNCATE,
        0,
      )
    val resultRoundUp =
      measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.findPercentage(
        9,
        RoundingStrategy.ROUND_UP,
        0,
      )
    val resultRoundOffLower =
      measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.findPercentage(
        9,
        RoundingStrategy.ROUND_OFF,
        0,
      )
    val resultRoundOffUpper =
      measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.findPercentage(
        8,
        RoundingStrategy.ROUND_OFF,
        0,
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
        RoundingStrategy.TRUNCATE,
        2,
      )
    val resultPrecision2RoundUp =
      measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.findPercentage(
        9,
        RoundingStrategy.ROUND_UP,
        2,
      )
    val resultPrecision2RoundOff =
      measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.findPercentage(
        7,
        RoundingStrategy.ROUND_OFF,
        2,
      )
    val resultPrecision0RoundUp =
      measureReport.groupFirstRep.stratifierFirstRep.stratumFirstRep.findPercentage(
        9,
        RoundingStrategy.ROUND_UP,
        0,
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
    coEvery { fhirEngine.search<MeasureReport>(any<Search>()) } returns emptyList()
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

  @Test
  fun `belongToSubject should return true for same subject`() {
    val report = MeasureReport().apply { subject = Reference().apply { reference = "Patient/123" } }
    assertTrue(report.belongToSubject("123".asReference(ResourceType.Patient)))
  }

  @Test
  fun `belongToSubject should return true for same subject with history`() {
    val report =
      MeasureReport().apply { subject = Reference().apply { reference = "Patient/123/_history/5" } }
    assertTrue(report.belongToSubject("123/_history/4".asReference(ResourceType.Patient)))
  }

  @Test
  fun `belongToSubject should return false for null subject`() {
    val report = MeasureReport()
    assertFalse(report.belongToSubject("123".asReference(ResourceType.Patient)))
  }

  @Test
  fun `belongToSubject should return false for subject with null reference`() {
    val report = MeasureReport().apply { subject = Reference() }
    assertFalse(report.belongToSubject("123".asReference(ResourceType.Patient)))
  }

  @Test
  fun `belongToSubject should return false for subject with null param`() {
    val report = MeasureReport().apply { subject = Reference().apply { reference = "Patient/123" } }
    assertFalse(report.belongToSubject(null as Reference?))
  }

  @Test
  fun `belongToSubject should return false for subject with different resource type`() {
    val report = MeasureReport().apply { subject = Reference().apply { reference = "Patient/123" } }
    assertFalse(report.belongToSubject("123".asReference(ResourceType.Practitioner)))
  }

  @Test
  fun `hasParams should return true for same params`() {
    val report = MeasureReport().apply { addParams(mapOf("test" to "123", "check" to "2211")) }

    assertTrue(report.hasParams(mapOf("test" to "123", "check" to "2211")))
  }

  @Test
  fun `hasParams should return true for same params with different order`() {
    val report = MeasureReport().apply { addParams(mapOf("test" to "123", "check" to "2211")) }
    assertTrue(report.hasParams(mapOf("test" to "123", "check" to "2211")))
  }

  @Test
  fun `hasParams should return false for different params values`() {
    val report = MeasureReport().apply { addParams(mapOf("test" to "123", "check" to "111")) }
    assertFalse(report.hasParams(mapOf("test" to "123", "check" to "2211")))
  }

  @Test
  fun `hasParams should return false for different filter params list size`() {
    val report = MeasureReport().apply { addParams(mapOf("test" to "123", "check" to "111")) }
    assertFalse(report.hasParams(mapOf("test" to "123")))
  }

  @Test
  fun `hasParams should return false for different report params list size`() {
    val report = MeasureReport().apply { addParams(mapOf("test" to "123")) }
    assertFalse(report.hasParams(mapOf("test" to "123", "check" to "111")))
  }

  @Test
  fun `addParams should return add Parameters resource`() {
    val report = MeasureReport()
    report.addParams(mapOf("test" to "123", "check" to "111"))
    assertInstanceOf(Parameters::class.java, report.contained.first())

    val param1 = (report.contained.single() as Parameters).parameter.elementAt(0)
    assertEquals(param1.let { it.name to it.value.valueToString() }, Pair("test", "123"))

    val param2 = (report.contained.single() as Parameters).parameter.elementAt(1)
    assertEquals(param2.let { it.name to it.value.valueToString() }, Pair("check", "111"))
  }

  @Test
  fun `addParams should return update Parameters resource`() {
    val report = MeasureReport().apply { addParams(mapOf("test" to "123", "check" to "111")) }

    report.addParams(mapOf("test" to "123", "another" to "333"))

    assertEquals(1, report.contained.size)
    assertEquals(3, (report.contained.first() as Parameters).parameter.size)

    val param1 = (report.contained.single() as Parameters).parameter.elementAt(0)
    assertEquals(param1.let { it.name to it.value.valueToString() }, Pair("test", "123"))

    val param2 = (report.contained.single() as Parameters).parameter.elementAt(1)
    assertEquals(param2.let { it.name to it.value.valueToString() }, Pair("check", "111"))

    val param3 = (report.contained.single() as Parameters).parameter.elementAt(2)
    assertEquals(param3.let { it.name to it.value.valueToString() }, Pair("another", "333"))
  }

  @Test
  fun `extractParameters should return return all params map`() {
    val report =
      MeasureReport().apply {
        addParams(mapOf("test1" to "111", "test2" to "222"))
        addParams(mapOf("test3" to "333", "test4" to "444"))
      }
    assertEquals(
      mapOf("test1" to "111", "test2" to "222", "test3" to "333", "test4" to "444"),
      report.extractParameters()
    )
  }

  @Test
  fun `isSameAs should return return true for same reports`() {
    val report1 =
      MeasureReport().apply {
        subject = Reference().apply { reference = "Patient/123" }
        measure = "Measure/000"
        addParams(mapOf("test1" to "111", "test2" to "222"))
      }

    val report2 =
      MeasureReport().apply {
        subject = Reference().apply { reference = "Patient/123/_history/2" }
        measure = "Measure/000"
        addParams(mapOf("test2" to "222", "test1" to "111"))
      }

    assertTrue(report1.isSameAs(report2))
  }

  @Test
  fun `isSameAs should return return true for same reports with no subject`() {
    val report1 =
      MeasureReport().apply {
        measure = "Measure/000"
        addParams(mapOf("test1" to "111", "test2" to "222"))
      }

    val report2 =
      MeasureReport().apply {
        measure = "Measure/000"
        addParams(mapOf("test2" to "222", "test1" to "111"))
      }

    assertTrue(report1.isSameAs(report2))
  }

  @Test
  fun `isSameAs should return return true for same reports with no params`() {
    val report1 =
      MeasureReport().apply {
        subject = Reference().apply { reference = "Patient/123" }
        measure = "Measure/000"
      }

    val report2 =
      MeasureReport().apply {
        subject = Reference().apply { reference = "Patient/123" }
        measure = "Measure/000"
      }

    assertTrue(report1.isSameAs(report2))
  }

  @Test
  fun `isSameAs should return return false for reports with different subject`() {
    val report1 =
      MeasureReport().apply {
        subject = Reference().apply { reference = "Patient/123" }
        measure = "Measure/000"
        addParams(mapOf("test1" to "111", "test2" to "222"))
      }

    val report2 =
      MeasureReport().apply {
        subject = Reference().apply { reference = "Patient/345" }
        measure = "Measure/000"
        addParams(mapOf("test2" to "222", "test1" to "111"))
      }

    assertFalse(report1.isSameAs(report2))
  }

  @Test
  fun `isSameAs should return return false for reports with different measure`() {
    val report1 =
      MeasureReport().apply {
        subject = Reference().apply { reference = "Patient/123" }
        measure = "Measure/000"
        addParams(mapOf("test1" to "111", "test2" to "222"))
      }

    val report2 =
      MeasureReport().apply {
        subject = Reference().apply { reference = "Patient/123" }
        measure = "Measure/001"
        addParams(mapOf("test2" to "222", "test1" to "111"))
      }

    assertFalse(report1.isSameAs(report2))
  }

  @Test
  fun `isSameAs should return return false for reports with different params`() {
    val report1 =
      MeasureReport().apply {
        subject = Reference().apply { reference = "Patient/123" }
        measure = "Measure/000"
        addParams(mapOf("test1" to "111", "test2" to "222"))
      }

    val report2 =
      MeasureReport().apply {
        subject = Reference().apply { reference = "Patient/123" }
        measure = "Measure/000"
        addParams(mapOf("test2" to "222", "test1" to "000"))
      }

    assertFalse(report1.isSameAs(report2))
  }

  @Test
  fun `isSameAs should return return false for reports with one missing params`() {
    val report1 =
      MeasureReport().apply {
        subject = Reference().apply { reference = "Patient/123" }
        measure = "Measure/000"
      }

    val report2 =
      MeasureReport().apply {
        subject = Reference().apply { reference = "Patient/123" }
        measure = "Measure/000"
        addParams(mapOf("test2" to "222", "test1" to "000"))
      }

    assertFalse(report1.isSameAs(report2))
    assertFalse(report2.isSameAs(report1))
  }
}
