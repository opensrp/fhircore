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
import java.text.SimpleDateFormat
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Reference
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
class ProfileDataTest : RobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)

  @Before
  fun setup() {
    hiltRule.inject()
  }

  @Test
  fun testMapToDomainModelHiv() {
    val hivProfileDto = buildProfileData(HealthModule.HIV) as ProfileData.HivProfileData
    with(hivProfileDto) {
      Assert.assertEquals("logicalId", logicalId)
      Assert.assertEquals("testName", name)
      Assert.assertEquals("testIdentifier()", identifier)
      Assert.assertEquals("testAddress", address)
      Assert.assertEquals("5y", age)
      Assert.assertEquals(Enumerations.AdministrativeGender.MALE, gender)
      //      Assert.assertEquals(Date("12345678"), birthdate)
      Assert.assertEquals("referenceKey", chwAssigned.reference)
    }
  }

  @Test
  fun testMapToDomainModelHomeTracing() {
    val tracingProfileDto =
      buildProfileData(HealthModule.HOME_TRACING) as ProfileData.AppointmentProfileData
    with(tracingProfileDto) {
      Assert.assertEquals("logicalId", logicalId)
      Assert.assertEquals("testName", name)
      Assert.assertEquals("testIdentifier()", identifier)
      Assert.assertEquals("testAddress", address)
      Assert.assertEquals("5y", age)
      Assert.assertEquals(Enumerations.AdministrativeGender.MALE, gender)
      //      Assert.assertEquals(Date("12345678"), birthdate)
      Assert.assertEquals("referenceKey", chwAssigned.reference)
    }
  }

  @Test
  fun testMapToDomainModelAnc() {
    val ancProfileDto = buildProfileData(HealthModule.ANC) as ProfileData.AncProfileData
    with(ancProfileDto) {
      Assert.assertEquals("logicalId", logicalId)
      Assert.assertEquals("testName", name)
      Assert.assertEquals("testIdentifier()", identifier)
      Assert.assertEquals("testAddress", address)
      Assert.assertEquals("5y", age)
      //      Assert.assertEquals(Date("12345678"), birthdate)
      Assert.assertEquals(Enumerations.AdministrativeGender.MALE, gender)
      Assert.assertEquals(VisitStatus.DUE, visitStatus)
      Assert.assertEquals(true, services.isEmpty())
      Assert.assertEquals(true, tasks.isEmpty())
      Assert.assertEquals(true, conditions.isEmpty())
      Assert.assertEquals(true, flags.isEmpty())
      Assert.assertEquals(true, visits.isEmpty())
    }
  }

  @Test
  fun testMapToDomainModelFamily() {
    val familyProfileDto = buildProfileData(HealthModule.FAMILY) as ProfileData.FamilyProfileData
    with(familyProfileDto) {
      Assert.assertEquals("logicalId", logicalId)
      Assert.assertEquals("testName", name)
      Assert.assertEquals("testIdentifier()", identifier)
      Assert.assertEquals("testAddress", address)
      Assert.assertEquals("5y", age)
      Assert.assertEquals(true, services.isEmpty())
      Assert.assertEquals(true, tasks.isEmpty())
      Assert.assertEquals(true, members.isEmpty())
    }
  }

  private fun buildProfileData(healthModule: HealthModule): ProfileData {
    return when (healthModule) {
      HealthModule.HIV ->
        ProfileData.HivProfileData(
          logicalId = "logicalId",
          name = "testName",
          identifier = "testIdentifier()",
          address = "testAddress",
          age = "5y",
          gender = Enumerations.AdministrativeGender.MALE,
          birthdate = SimpleDateFormat("yyyy-MM-dd").parse("2021-05-25"),
          chwAssigned = Reference("referenceKey"),
          patientType = PatientType.EXPOSED_INFANT
        )
      HealthModule.HOME_TRACING, HealthModule.PHONE_TRACING ->
        ProfileData.AppointmentProfileData(
          logicalId = "logicalId",
          name = "testName",
          identifier = "testIdentifier()",
          address = "testAddress",
          age = "5y",
          gender = Enumerations.AdministrativeGender.MALE,
          birthdate = SimpleDateFormat("yyyy-MM-dd").parse("2021-05-25"),
          chwAssigned = Reference("referenceKey")
        )
      HealthModule.APPOINTMENT ->
        ProfileData.AppointmentProfileData(
          logicalId = "logicalId",
          name = "testName",
          identifier = "testIdentifier()",
          address = "testAddress",
          age = "5y",
          gender = Enumerations.AdministrativeGender.MALE,
          birthdate = SimpleDateFormat("yyyy-MM-dd").parse("2021-05-25"),
          chwAssigned = Reference("referenceKey")
        )
      HealthModule.ANC ->
        ProfileData.AncProfileData(
          logicalId = "logicalId",
          name = "testName",
          identifier = "testIdentifier()",
          address = "testAddress",
          age = "5y",
          gender = Enumerations.AdministrativeGender.MALE,
          birthdate = SimpleDateFormat("yyyy-MM-dd").parse("2021-05-25"),
          visitStatus = VisitStatus.DUE,
          services = emptyList(),
          tasks = emptyList(),
          conditions = emptyList(),
          flags = emptyList(),
          visits = emptyList()
        )
      HealthModule.FAMILY ->
        ProfileData.FamilyProfileData(
          logicalId = "logicalId",
          name = "testName",
          identifier = "testIdentifier()",
          address = "testAddress",
          age = "5y",
          services = emptyList(),
          tasks = emptyList(),
          members = emptyList()
        )
      HealthModule.FAMILY_PLANNING ->
        ProfileData.FamilyProfileData(
          logicalId = "logicalId",
          name = "testName",
          identifier = "testIdentifier()",
          address = "testAddress",
          age = "5y",
          services = emptyList(),
          tasks = emptyList(),
          members = emptyList()
        )
      HealthModule.RDT ->
        ProfileData.FamilyProfileData(
          logicalId = "logicalId",
          name = "testName",
          identifier = "testIdentifier()",
          address = "testAddress",
          age = "5y",
          services = emptyList(),
          tasks = emptyList(),
          members = emptyList()
        )
      HealthModule.PNC ->
        ProfileData.FamilyProfileData(
          logicalId = "logicalId",
          name = "testName",
          identifier = "testIdentifier()",
          address = "testAddress",
          age = "5y",
          services = emptyList(),
          tasks = emptyList(),
          members = emptyList()
        )
      HealthModule.CHILD ->
        ProfileData.AncProfileData(
          logicalId = "logicalId",
          name = "testName",
          identifier = "testIdentifier()",
          address = "testAddress",
          age = "5y",
          gender = Enumerations.AdministrativeGender.MALE,
          birthdate = SimpleDateFormat("yyyy-MM-dd").parse("2021-05-25"),
          visitStatus = VisitStatus.DUE,
          services = emptyList(),
          tasks = emptyList(),
          conditions = emptyList(),
          flags = emptyList(),
          visits = emptyList()
        )
      HealthModule.DEFAULT ->
        ProfileData.DefaultProfileData(
          logicalId = "logicalId",
          name = "testName",
          identifier = "testIdentifier()",
          address = "testAddress",
          age = "5y",
          gender = Enumerations.AdministrativeGender.MALE,
          birthdate = SimpleDateFormat("yyyy-MM-dd").parse("2021-05-25"),
          services = emptyList(),
          tasks = emptyList(),
          conditions = emptyList(),
          flags = emptyList(),
          visits = emptyList()
        )
    }
  }
}
