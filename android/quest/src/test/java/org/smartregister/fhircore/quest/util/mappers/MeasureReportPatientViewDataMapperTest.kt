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

package org.smartregister.fhircore.quest.util.mappers

import com.google.android.fhir.logicalId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.hl7.fhir.r4.model.Patient
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class MeasureReportPatientViewDataMapperTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var measureReportPatientViewDataMapper: MeasureReportSubjectViewDataMapper

  @Before
  fun setup() {
    hiltRule.inject()
  }

  @Test
  fun testMapToOutputModelPatient() {
    val samplePatient =
      "patient-registration-questionnaire/sample/patient.json".parseSampleResourceFromFile()
        as Patient
    val dto =
      ResourceData(
        baseResourceId = samplePatient.logicalId,
        baseResourceType = samplePatient.resourceType,
        computedValuesMap = emptyMap(),
      )
    val profileViewDataHiv = measureReportPatientViewDataMapper.transformInputToOutputModel(dto)
    with(profileViewDataHiv) {
      // TODO Update expected values once refactors in
      //  MeasureReportPatientViewDataMapper#transformInputToOutputModel() are complete
      Assert.assertEquals("TEST_PATIENT", logicalId)
      Assert.assertEquals("", display)
      Assert.assertEquals("", getTestPatientAge())
      Assert.assertEquals("", family)
    }
    Assert.assertEquals("TEST_PATIENT", profileViewDataHiv.logicalId)
  }

  private fun getTestPatientAge(): String {
    // Update this according to value in patient-registration-questionnaire/sample/patient.json file
    // val dob: LocalDate = LocalDate.of(1998, 12, 14)
    // val period: Period = Period.between(dob, LocalDate.now())
    // TODO Update expected values once refactors in
    //  MeasureReportPatientViewDataMapper#transformInputToOutputModel() are complete
    // return "${period.years}y"
    return ""
  }
}
