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
import java.text.SimpleDateFormat
import javax.inject.Inject
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Reference
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.domain.model.HealthStatus
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.VisitStatus
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.shared.models.PatientProfileRowItem
import org.smartregister.fhircore.quest.ui.shared.models.ProfileViewData

@HiltAndroidTest
class ProfileViewDataMapperTest : RobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var profileViewDataMapper: ProfileViewDataMapper

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
  fun testMapToDomainModelHiv() {
    val dto = buildProfileData(HealthModule.HIV)
    val profileViewDataHiv =
      profileViewDataMapper.transformInputToOutputModel(dto) as
        ProfileViewData.PatientProfileViewData
    with(profileViewDataHiv) {
      Assert.assertEquals("logicalId", logicalId)
      Assert.assertEquals("testName", name)
      Assert.assertEquals("familyName", familyName)
      Assert.assertEquals("givenName", givenName)
      Assert.assertEquals("testIdentifier()", identifier)
      Assert.assertEquals("HCC Number", identifierKey)
      Assert.assertEquals(true, showIdentifierInProfile)
      Assert.assertEquals("testAddress", address)
      Assert.assertEquals("5y", age)
      Assert.assertEquals(emptyList<PatientProfileRowItem>(), upcomingServices)
      Assert.assertEquals(emptyList<PatientProfileRowItem>(), tasks)
    }
  }

  @Test
  fun testMapToDomainModelHomeTracing() {
    val dto = buildProfileData(HealthModule.HOME_TRACING)
    val profileViewDataHiv =
      profileViewDataMapper.transformInputToOutputModel(dto) as
        ProfileViewData.PatientProfileViewData
    with(profileViewDataHiv) {
      Assert.assertEquals("logicalId", logicalId)
      Assert.assertEquals("testName", name)
      Assert.assertEquals("5y", age)
      Assert.assertEquals(emptyList<PatientProfileRowItem>(), upcomingServices)
      Assert.assertEquals(emptyList<PatientProfileRowItem>(), tasks)
      Assert.assertEquals(emptyList<PatientProfileRowItem>(), practitioners)
    }
  }

  @Test
  fun testMapToDomainModelAnc() {
    val dto = buildProfileData(HealthModule.ANC)
    val profileViewDataHiv =
      profileViewDataMapper.transformInputToOutputModel(dto) as
        ProfileViewData.PatientProfileViewData
    with(profileViewDataHiv) {
      Assert.assertEquals("logicalId", logicalId)
      Assert.assertEquals("testName", name)
      Assert.assertEquals("testIdentifier()", identifier)
      Assert.assertEquals("5y", age)
      Assert.assertEquals(true, tasks.isEmpty())
      Assert.assertEquals(true, upcomingServices.isEmpty())
      Assert.assertEquals(emptyList<PatientProfileRowItem>(), tasks)
      Assert.assertEquals(emptyList<PatientProfileRowItem>(), practitioners)
    }
  }

  @Test
  fun testMapToDomainModelFamily() {
    val dto = buildProfileData(HealthModule.FAMILY)
    val profileViewDataHiv =
      profileViewDataMapper.transformInputToOutputModel(dto) as
        ProfileViewData.FamilyProfileViewData
    with(profileViewDataHiv) {
      Assert.assertEquals("logicalId", logicalId)
      Assert.assertEquals("testName Family", name)
      Assert.assertEquals("testAddress", address)
      Assert.assertEquals("5y", age)
      Assert.assertEquals(true, familyMemberViewStates.isEmpty())
    }
  }

  @Test
  fun testMapToDomainModelDefault() {
    val dto = buildProfileData(HealthModule.DEFAULT)
    val profileViewDataHiv =
      profileViewDataMapper.transformInputToOutputModel(dto) as
        ProfileViewData.PatientProfileViewData
    with(profileViewDataHiv) {
      Assert.assertEquals("logicalId", logicalId)
      Assert.assertEquals("testName", name)
      Assert.assertEquals("testIdentifier()", identifier)
      Assert.assertEquals("5y", age)
      Assert.assertEquals(Enumerations.AdministrativeGender.MALE.display, sex)
      Assert.assertEquals(true, upcomingServices.isEmpty())
      Assert.assertEquals(true, tasks.isEmpty())
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
          tasks = emptyList(),
          showIdentifierInProfile = true
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
}
