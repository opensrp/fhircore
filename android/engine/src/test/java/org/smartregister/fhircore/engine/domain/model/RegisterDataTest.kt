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

package org.smartregister.fhircore.engine.domain.model

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hl7.fhir.r4.model.Enumerations
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
class RegisterDataTest : RobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)

  @Before
  fun setup() {
    hiltRule.inject()
  }

  @Test
  fun testMapToDomainModelHiv() {
    val hivRegisterDto = buildRegisterData(HealthModule.HIV) as RegisterData.HivRegisterData
    with(hivRegisterDto) {
      Assert.assertEquals("logicalId", logicalId)
      Assert.assertEquals("testName", name)
      Assert.assertEquals("testIdentifier()", identifier)
      Assert.assertEquals("testAddress", address)
      Assert.assertEquals("5y", age)
      Assert.assertEquals(Enumerations.AdministrativeGender.MALE, gender)
      //            Assert.assertEquals(Date("12345678"), birthdate)
      Assert.assertEquals("reference/Key", chwAssigned)
    }
  }

  @Test
  fun testMapToDomainModelTracing() {
    val tracingRegisterDto =
      buildRegisterData(HealthModule.TRACING) as RegisterData.AppointmentRegisterData
    with(tracingRegisterDto) {
      Assert.assertEquals("logicalId", logicalId)
      Assert.assertEquals("testName", name)
      Assert.assertEquals("testIdentifier()", identifier)
      Assert.assertEquals("testAddress", address)
      Assert.assertEquals("5y", age)
      Assert.assertEquals(Enumerations.AdministrativeGender.MALE, gender)
      //      Assert.assertEquals(Date("12345678"), birthdate)
      Assert.assertEquals("reference/Key", chwAssigned)
    }
  }

  @Test
  fun testMapToDomainModelAnc() {
    val ancRegisterDto = buildRegisterData(HealthModule.ANC) as RegisterData.AncRegisterData
    with(ancRegisterDto) {
      Assert.assertEquals("logicalId", logicalId)
      Assert.assertEquals("testName", name)
      Assert.assertEquals("testIdentifier()", identifier)
      Assert.assertEquals("testAddress", address)
      Assert.assertEquals("5y", age)
      //      Assert.assertEquals(Date("12345678"), birthdate)
      Assert.assertEquals(Enumerations.AdministrativeGender.MALE, gender)
      Assert.assertEquals(ServiceStatus.DUE, serviceStatus)
    }
  }

  @Test
  fun testMapToDomainModelFamily() {
    val familyRegisterDto =
      buildRegisterData(HealthModule.FAMILY) as RegisterData.FamilyRegisterData
    with(familyRegisterDto) {
      Assert.assertEquals("logicalId", logicalId)
      Assert.assertEquals("testName", name)
      Assert.assertEquals("testIdentifier()", identifier)
      Assert.assertEquals("testAddress", address)
      Assert.assertEquals(true, members.isEmpty())
    }
  }

  private fun buildRegisterData(healthModule: HealthModule): RegisterData {
    return when (healthModule) {
      HealthModule.HIV ->
        RegisterData.HivRegisterData(
          logicalId = "logicalId",
          name = "testName",
          identifier = "testIdentifier()",
          address = "testAddress",
          age = "5y",
          gender = Enumerations.AdministrativeGender.MALE,
          chwAssigned = "reference/Key",
          phoneContacts = emptyList()
        )
      HealthModule.TRACING ->
        RegisterData.AppointmentRegisterData(
          logicalId = "logicalId",
          name = "testName",
          identifier = "testIdentifier()",
          address = "testAddress",
          age = "5y",
          gender = Enumerations.AdministrativeGender.MALE,
          chwAssigned = "reference/Key"
        )
      HealthModule.APPOINTMENT ->
        RegisterData.AppointmentRegisterData(
          logicalId = "logicalId",
          name = "testName",
          identifier = "testIdentifier()",
          address = "testAddress",
          age = "5y",
          gender = Enumerations.AdministrativeGender.MALE,
          chwAssigned = "reference/Key"
        )
      HealthModule.ANC ->
        RegisterData.AncRegisterData(
          logicalId = "logicalId",
          name = "testName",
          identifier = "testIdentifier()",
          address = "testAddress",
          age = "5y",
          gender = Enumerations.AdministrativeGender.MALE,
          serviceStatus = ServiceStatus.DUE
        )
      HealthModule.FAMILY ->
        RegisterData.FamilyRegisterData(
          logicalId = "logicalId",
          name = "testName",
          identifier = "testIdentifier()",
          address = "testAddress",
          members = emptyList()
        )
      HealthModule.FAMILY_PLANNING ->
        RegisterData.FamilyRegisterData(
          logicalId = "logicalId",
          name = "testName",
          identifier = "testIdentifier()",
          address = "testAddress",
          members = emptyList()
        )
      HealthModule.RDT ->
        RegisterData.AncRegisterData(
          logicalId = "logicalId",
          name = "testName",
          identifier = "testIdentifier()",
          address = "testAddress",
          age = "5y",
          gender = Enumerations.AdministrativeGender.MALE,
          serviceStatus = ServiceStatus.DUE
        )
      HealthModule.PNC ->
        RegisterData.AncRegisterData(
          logicalId = "logicalId",
          name = "testName",
          identifier = "testIdentifier()",
          address = "testAddress",
          age = "5y",
          gender = Enumerations.AdministrativeGender.MALE,
          serviceStatus = ServiceStatus.DUE
        )
      HealthModule.CHILD ->
        RegisterData.AncRegisterData(
          logicalId = "logicalId",
          name = "testName",
          identifier = "testIdentifier()",
          address = "testAddress",
          age = "5y",
          gender = Enumerations.AdministrativeGender.MALE,
          serviceStatus = ServiceStatus.DUE
        )
      HealthModule.DEFAULT ->
        RegisterData.DefaultRegisterData(
          logicalId = "logicalId",
          name = "testName",
          age = "5y",
          gender = Enumerations.AdministrativeGender.MALE
        )
    }
  }
}
