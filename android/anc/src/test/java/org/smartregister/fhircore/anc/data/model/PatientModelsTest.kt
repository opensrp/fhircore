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
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

class PatientModelsTest : RobolectricTest() {

  private lateinit var ancPatientItem: AncPatientItem
  private lateinit var ancPatientItemHead: AncPatientItem
  private lateinit var encounterItem: EncounterItem
  private lateinit var upcomingServiceItem: UpcomingServiceItem
  private lateinit var ancPatientDetailItem: AncPatientDetailItem
  private lateinit var patientBmiItem: PatientBmiItem
  private lateinit var reportItem: ReportItem

  @Before
  fun setUp() {
    ancPatientItem =
      AncPatientItem("111", "anb", "M", "25", "PD", "none", "xyz", VisitStatus.PLANNED)
    ancPatientItemHead =
      AncPatientItem("111", "anb", "M", "25", "PD", "none", "xyz", VisitStatus.PLANNED)
    encounterItem = EncounterItem("111", status = Encounter.EncounterStatus.ARRIVED, "abc", Date())
    upcomingServiceItem = UpcomingServiceItem("111", "1bc", "2020-02-12")
    ancPatientDetailItem = AncPatientDetailItem(ancPatientItem, ancPatientItemHead)
    patientBmiItem = PatientBmiItem("1111", "testBMI1", "5'7", "50lbs", "22.22")
    reportItem = ReportItem("1111", "test report ANC", "women having test report ANC", "4")
  }

  @Test
  fun testAncPatientItem() {
    Assert.assertEquals("111", ancPatientItem.patientIdentifier)
    Assert.assertEquals("anb", ancPatientItem.name)
    Assert.assertEquals("M", ancPatientItem.gender)
    Assert.assertEquals("PD", ancPatientItem.demographics)
    Assert.assertEquals("none", ancPatientItem.atRisk)
    Assert.assertEquals("xyz", ancPatientItem.address)
    Assert.assertEquals(VisitStatus.PLANNED, ancPatientItem.visitStatus)
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
    Assert.assertEquals("111", ancPatientDetailItem.patientDetails.patientIdentifier)
    Assert.assertEquals("anb", ancPatientDetailItem.patientDetails.name)
    Assert.assertEquals("M", ancPatientDetailItem.patientDetails.gender)
    Assert.assertEquals("PD", ancPatientDetailItem.patientDetails.demographics)
    Assert.assertEquals("none", ancPatientDetailItem.patientDetails.atRisk)
    Assert.assertEquals("xyz", ancPatientDetailItem.patientDetails.address)
    Assert.assertEquals(VisitStatus.PLANNED, ancPatientDetailItem.patientDetails.visitStatus)
    Assert.assertEquals("111", ancPatientDetailItem.patientDetailsHead.patientIdentifier)
    Assert.assertEquals("anb", ancPatientDetailItem.patientDetailsHead.name)
    Assert.assertEquals("M", ancPatientDetailItem.patientDetailsHead.gender)
    Assert.assertEquals("PD", ancPatientDetailItem.patientDetailsHead.demographics)
    Assert.assertEquals("none", ancPatientDetailItem.patientDetailsHead.atRisk)
    Assert.assertEquals("xyz", ancPatientDetailItem.patientDetailsHead.address)
    Assert.assertEquals(VisitStatus.PLANNED, ancPatientDetailItem.patientDetailsHead.visitStatus)
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
  }
}
