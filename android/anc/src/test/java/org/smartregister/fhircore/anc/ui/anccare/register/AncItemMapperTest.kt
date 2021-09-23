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

package org.smartregister.fhircore.anc.ui.anccare.register

import java.util.Date
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.anc.data.anc.model.AncVisitStatus
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

class AncItemMapperTest : RobolectricTest() {

  private lateinit var patient: Patient

  @Before
  fun setUp() {
    patient = getPatient()
  }

  @Test
  fun testMapToDomainModel() {
    val patientItem = AncItemMapper.mapToDomainModel(dto = Anc(patient, null, listOf()))
    with(patientItem) {
      Assert.assertEquals("test_patient_id_1", patientIdentifier)
      Assert.assertEquals("Jane Mc", name)
      Assert.assertEquals("M", gender)
      Assert.assertEquals("0", age)
      Assert.assertEquals("Jane Mc, M, 0", demographics)
      Assert.assertEquals("", atRisk)
      Assert.assertEquals(AncVisitStatus.PLANNED, visitStatus)
    }
  }

  private fun getPatient(): Patient {
    return Patient().apply {
      id = "test_patient_id_1"
      gender = Enumerations.AdministrativeGender.MALE
      name =
        mutableListOf(
          HumanName().apply {
            addGiven("jane")
            family = "Mc"
          }
        )
      telecom = mutableListOf(ContactPoint().apply { value = "12345678" })
      address =
        mutableListOf(
          Address().apply {
            city = "Nairobi"
            country = "Kenya"
          }
        )
      active = true
      birthDate = Date()
    }
  }
}
