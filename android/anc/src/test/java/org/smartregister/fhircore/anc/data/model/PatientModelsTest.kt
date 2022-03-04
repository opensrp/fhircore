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

package org.smartregister.fhircore.anc.data.model

import java.util.Date
import org.hl7.fhir.r4.model.Encounter
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.anc.data.report.model.ResultItem
import org.smartregister.fhircore.anc.data.report.model.ResultItemPopulation
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.plusYears

class PatientModelsTest : RobolectricTest() {

  private lateinit var patientItem: PatientItem
  private lateinit var patientItemHead: PatientItem
  private lateinit var encounterItem: EncounterItem
  private lateinit var upcomingServiceItem: UpcomingServiceItem
  private lateinit var patientDetailItem: PatientDetailItem
  private lateinit var patientBmiItem: PatientBmiItem
  private lateinit var reportItem: ReportItem
  private lateinit var resulttItem: ResultItem
  private lateinit var resulttItemPopulation: ResultItemPopulation
  private lateinit var patientVitalItem: PatientVitalItem

  @Before
  fun setUp() {
    patientItem =
      PatientItem(
        "111",
        "anb",
        "fam",
        "M",
        Date().plusYears(-25),
        "none",
        "xyz",
        true,
        VisitStatus.PLANNED,
        false
      )
    patientItemHead =
      PatientItem(
        "111",
        "anb",
        "fam",
        "M",
        Date().plusYears(-25),
        "none",
        "xyz",
        false,
        VisitStatus.PLANNED
      )
    encounterItem = EncounterItem("111", status = Encounter.EncounterStatus.ARRIVED, "abc", Date())
    upcomingServiceItem = UpcomingServiceItem("111", "1bc", "2020-02-12")
    patientDetailItem = PatientDetailItem(patientItem, patientItemHead)
    patientBmiItem = PatientBmiItem("1111", "testBMI1", "5'7", "50lbs", "22.22")
    reportItem =
      ReportItem(
        id = "1111",
        title = "test report ANC",
        description = "women having test report ANC",
        reportType = "4",
        name = "testReportANC"
      )
    resulttItem = ResultItem("True", true, "Test description")
    resulttItemPopulation =
      ResultItemPopulation(title = "testTitlePopulation", dataList = emptyList())
    patientVitalItem =
      PatientVitalItem(
        height = "160",
        weight = "60",
        bmi = "22",
        heightUnit = "cm",
        weightUnit = "kg",
        bmiUnit = "kg/m2"
      )
  }

  @Test
  fun testAncPatientItem() {
    Assert.assertEquals("111", patientItem.patientIdentifier)
    Assert.assertEquals("anb", patientItem.name)
    Assert.assertEquals("M", patientItem.gender)
    Assert.assertEquals("none", patientItem.atRisk)
    Assert.assertEquals("xyz", patientItem.address)
    Assert.assertEquals(VisitStatus.PLANNED, patientItem.visitStatus)
    Assert.assertEquals("fam", patientItem.familyName)
  }

  @Test
  fun testUpcomingServiceItem() {
    Assert.assertEquals("111", encounterItem.id)
    Assert.assertEquals(Encounter.EncounterStatus.ARRIVED, encounterItem.status)
    Assert.assertEquals("abc", encounterItem.display)
  }

  @Test
  fun testEncounterItem() {
    Assert.assertEquals("111", upcomingServiceItem.encounterIdentifier)
    Assert.assertEquals("1bc", upcomingServiceItem.title)
    Assert.assertEquals("2020-02-12", upcomingServiceItem.date)
  }

  @Test
  fun testAncPatientDetailItem() {
    Assert.assertEquals("111", patientDetailItem.patientDetails.patientIdentifier)
    Assert.assertEquals("anb", patientDetailItem.patientDetails.name)
    Assert.assertEquals("M", patientDetailItem.patientDetails.gender)
    Assert.assertEquals("anb, M, 25y", patientDetailItem.patientDetails.demographics())
    Assert.assertEquals("none", patientDetailItem.patientDetails.atRisk)
    Assert.assertEquals("xyz", patientDetailItem.patientDetails.address)
    Assert.assertEquals(VisitStatus.PLANNED, patientDetailItem.patientDetails.visitStatus)
    Assert.assertEquals("111", patientDetailItem.patientDetailsHead.patientIdentifier)
    Assert.assertEquals("anb", patientDetailItem.patientDetailsHead.name)
    Assert.assertEquals("M", patientDetailItem.patientDetailsHead.gender)
    Assert.assertEquals("anb, M, 25y", patientDetailItem.patientDetailsHead.demographics())
    Assert.assertEquals("none", patientDetailItem.patientDetailsHead.atRisk)
    Assert.assertEquals("xyz", patientDetailItem.patientDetailsHead.address)
    Assert.assertEquals(VisitStatus.PLANNED, patientDetailItem.patientDetailsHead.visitStatus)
  }

  @Test
  fun testPatientBMIItem() {
    Assert.assertEquals("1111", patientBmiItem.patientIdentifier)
    Assert.assertEquals("testBMI1", patientBmiItem.name)
    Assert.assertEquals("5'7", patientBmiItem.height)
    Assert.assertEquals("50lbs", patientBmiItem.weight)
    Assert.assertEquals("22.22", patientBmiItem.bmi)
  }

  @Test
  fun testReportItem() {
    Assert.assertEquals("1111", reportItem.id)
    Assert.assertEquals("test report ANC", reportItem.title)
    Assert.assertEquals("women having test report ANC", reportItem.description)
    Assert.assertEquals("4", reportItem.reportType)
    Assert.assertEquals("testReportANC", reportItem.name)
  }

  @Test
  fun testResultItem() {
    Assert.assertEquals("True", resulttItem.status)
    Assert.assertEquals(true, resulttItem.isMatchedIndicator)
    Assert.assertEquals("Test description", resulttItem.description)
  }

  @Test
  fun testResultItemPopulation() {
    Assert.assertEquals("testTitlePopulation", resulttItemPopulation.title)
    Assert.assertNotNull(resulttItemPopulation.dataList)
  }

  @Test
  fun testPatientVitalItem() {
    Assert.assertEquals("160", patientVitalItem.height)
    Assert.assertEquals("cm", patientVitalItem.heightUnit)
    Assert.assertEquals("60", patientVitalItem.weight)
    Assert.assertEquals("kg", patientVitalItem.weightUnit)
    Assert.assertEquals("22", patientVitalItem.bmi)
    Assert.assertEquals("kg/m2", patientVitalItem.bmiUnit)
    Assert.assertEquals(true, patientVitalItem.isValidWeightAndHeight())
    Assert.assertEquals(true, patientVitalItem.isWeightAndHeightAreInMetricUnit())
    Assert.assertEquals(false, patientVitalItem.isWeightAndHeightAreInUscUnit())
  }
}
