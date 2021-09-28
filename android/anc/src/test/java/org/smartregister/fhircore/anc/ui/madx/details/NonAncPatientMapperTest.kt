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

package org.smartregister.fhircore.anc.ui.madx.details

import io.mockk.spyk
import java.util.Date
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

class NonAncPatientMapperTest : RobolectricTest() {
  private lateinit var patient: Patient

  @Before
  fun setUp() {
    patient = getPatient()
  }

  @Test
  fun testMapToDomainModel() {
    val dto: Patient = patient
    val patientItem = NonAncPatientItemMapper.mapToDomainModel(dto = dto)
    with(patientItem) {
      Assert.assertEquals(this.age, "0")
      Assert.assertEquals(this.name, "Nelson Mandela")
      Assert.assertEquals(this.patientIdentifier, "samplePatientId")
    }
  }

  fun getPatient(): Patient {
    val patient =
      spyk<Patient>().apply {
        id = "samplePatientId"
        gender = Enumerations.AdministrativeGender.MALE
        name =
          listOf(HumanName().setFamily("Mandela").setGiven(mutableListOf(StringType("Nelson"))))
        birthDate = Date()
      }
    return patient
  }
}
