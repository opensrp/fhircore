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

package org.smartregister.fhircore.quest.util.mappers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import javax.inject.Inject
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Patient
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class MeasureReportPatientViewDataMapperTest : RobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var measureReportPatientViewDataMapper: MeasureReportPatientViewDataMapper

  @Before
  fun setup() {
    hiltRule.inject()
    val mockedGender = mockk<Enumerations.AdministrativeGender>("MALE")
    every { mockedGender.translateGender(ApplicationProvider.getApplicationContext()) } returns
      "MALE"
  }

  fun Enumerations.AdministrativeGender.translateGender(context: Context) =
    when (this) {
      Enumerations.AdministrativeGender.MALE -> context.getString(R.string.male)
      Enumerations.AdministrativeGender.FEMALE -> context.getString(R.string.female)
      else -> context.getString(R.string.unknown)
    }

  @Test
  fun testMapToOutputModelPatient() {
    val dto =
      ResourceData(
        baseResource =
          "patient-registration-questionnaire/sample/patient.json".parseSampleResourceFromFile() as
            Patient
      )
    val profileViewDataHiv = measureReportPatientViewDataMapper.transformInputToOutputModel(dto)
    with(profileViewDataHiv) {
      Assert.assertEquals("TEST_PATIENT", logicalId)
      Assert.assertEquals("Bareera Hadi", name)
      Assert.assertEquals("23y", age)
      Assert.assertEquals("Hadi Family", family)
      Assert.assertEquals(
        Enumerations.AdministrativeGender.FEMALE.toString().first().uppercase(),
        gender
      )
    }
  }
}
