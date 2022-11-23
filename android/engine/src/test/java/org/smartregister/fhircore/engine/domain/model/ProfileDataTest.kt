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
import org.hl7.fhir.r4.model.CarePlan
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
      Assert.assertEquals("familyName", familyName)
      Assert.assertEquals("givenName", givenName)
      Assert.assertEquals("testIdentifier()", identifier)
      Assert.assertEquals("testAddress", address)
      Assert.assertEquals("5y", age)
      Assert.assertEquals(Enumerations.AdministrativeGender.MALE, gender)
      Assert.assertTrue(services.size == 2)
      Assert.assertTrue(conditions.isEmpty())
      Assert.assertTrue(practitioners.isEmpty())
      Assert.assertEquals("referenceKey", chwAssigned.reference)
    }
  }

  @Test
  fun testMapToDomainModelHomeTracing() {
    val tracingProfileDto =
      buildProfileData(HealthModule.HOME_TRACING) as ProfileData.DefaultProfileData
    with(tracingProfileDto) {
      Assert.assertEquals("logicalId", logicalId)
      Assert.assertEquals("testName", name)
      Assert.assertEquals("testIdentifier()", identifier)
      Assert.assertEquals("testAddress", address)
      Assert.assertEquals("5y", age)
      Assert.assertEquals(Enumerations.AdministrativeGender.MALE, gender)
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
          familyName = "familyName",
          givenName = "givenName",
          identifier = "testIdentifier()",
          address = "testAddress",
          age = "5y",
          gender = Enumerations.AdministrativeGender.MALE,
          birthdate = SimpleDateFormat("yyyy-MM-dd").parse("2021-05-25"),
          chwAssigned = Reference("referenceKey"),
          healthStatus = HealthStatus.EXPOSED_INFANT,
          services = buildCarePlanServices(),
          conditions = emptyList(),
          practitioners = emptyList()
        )
      HealthModule.HOME_TRACING, HealthModule.PHONE_TRACING ->
        ProfileData.DefaultProfileData(
          logicalId = "logicalId",
          name = "testName",
          identifier = "testIdentifier()",
          address = "testAddress",
          age = "5y",
          gender = Enumerations.AdministrativeGender.MALE,
          birthdate = SimpleDateFormat("yyyy-MM-dd").parse("2021-05-25")
        )
      HealthModule.APPOINTMENT ->
        ProfileData.DefaultProfileData(
          logicalId = "logicalId",
          name = "testName",
          identifier = "testIdentifier()",
          address = "testAddress",
          age = "5y",
          gender = Enumerations.AdministrativeGender.MALE,
          birthdate = SimpleDateFormat("yyyy-MM-dd").parse("2021-05-25")
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

  fun buildCarePlanServices(): List<CarePlan> {
    val carePlan1 = CarePlan()
    carePlan1.id = "CarePlan/cp1"
    carePlan1.careTeam = listOf(Reference("Ref11"), Reference("Ref12"))

    val carePlan2 = CarePlan()
    carePlan2.id = "CarePlan/cp2"
    carePlan2.careTeam = listOf(Reference("Ref21"), Reference("Ref22"))

    return listOf(carePlan1, carePlan2)
  }
}
