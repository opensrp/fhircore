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

package org.smartregister.fhircore.quest.ui.patient.register

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class PatientMapperTest : RobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var patientItemMapper: PatientItemMapper

  @Before
  fun setup() {
    hiltRule.inject()
  }

  @Test
  fun testMapToDomainModel() {

    val dto = buildPatient("123456", "123456", "Doe", "John", 12)
    val patientItem = patientItemMapper.mapToDomainModel(dto)
    with(patientItem) {
      assertEquals("12y", age)
      assertEquals("John Doe", name)
      assertEquals("123456", id)
      assertEquals("123456", identifier)
      assertEquals("Dist 1 City 1 State 1", displayAddress)
      assertEquals("+12345678", telecom)
      assertEquals("practitioner/1234", generalPractitionerReference)
      assertEquals("reference/5678", managingOrganizationReference)
      assertEquals("State 1", address!!.state)
      assertEquals("Dist 1", address!!.district)
      assertEquals("Location 1", address!!.text)
      assertEquals("Dist 1 City 1 State 1", address!!.fullAddress)
    }
  }

  @Test
  fun testMapToDomainModelWithoutIdentifier() {
    val dto = buildPatient("123456", null, "Doe", "John", 12)
    val patientItem = patientItemMapper.mapToDomainModel(dto)
    with(patientItem) {
      assertEquals("12y", age)
      assertEquals("John Doe", name)
      assertEquals("123456", id)
      assertEquals("", identifier)
      assertEquals("+12345678", telecom)
      assertEquals("practitioner/1234", generalPractitionerReference)
      assertEquals("reference/5678", managingOrganizationReference)
      assertEquals("Dist 1 City 1 State 1", displayAddress)
      assertEquals("State 1", address!!.state)
      assertEquals("Dist 1", address!!.district)
      assertEquals("Location 1", address!!.text)
      assertEquals("Dist 1 City 1 State 1", address!!.fullAddress)
    }
  }

  private fun buildPatient(
    id: String,
    identifier: String?,
    family: String,
    given: String,
    age: Int
  ): Patient {
    return Patient().apply {
      this.id = id
      this.identifierFirstRep.value = identifier
      this.addName().apply {
        this.family = family
        this.given.add(StringType(given))
      }
      this.birthDate = DateType(Date()).apply { add(Calendar.YEAR, -age) }.dateTimeValue().value

      this.addAddress().apply {
        district = "Dist 1"
        city = "City 1"
        state = "State 1"
        text = "Location 1"
      }

      this.addTelecom().apply { value = "+12345678" }
      this.generalPractitionerFirstRep.apply { reference = "practitioner/1234" }
      this.managingOrganization.apply { reference = "reference/5678" }
    }
  }
}
