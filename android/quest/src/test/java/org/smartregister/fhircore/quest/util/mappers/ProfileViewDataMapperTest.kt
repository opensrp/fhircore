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
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Reference
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.ServiceStatus
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
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
    val dto =
      ProfileData.HivProfileData(
        logicalId = "logicalId",
        name = "testName",
        identifier = "testIdentifier()",
        address = "testAddress",
        age = "5y",
        gender = Enumerations.AdministrativeGender.MALE,
        birthdate = SimpleDateFormat("yyyy-MM-dd").parse("2021-05-25"),
        chwAssigned = Reference("referenceKey")
      )
    val profileViewDataHiv =
      profileViewDataMapper.transformInputToOutputModel(dto) as ProfileViewData.HivProfileViewData
    with(profileViewDataHiv) {
      Assert.assertEquals("logicalId", logicalId)
      Assert.assertEquals("testName", name)
      Assert.assertEquals("testAddress", address)
      Assert.assertEquals("5y", age)
    }
  }

  @Test
  fun testMapToDomainModelTracing() {
    val dto =
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
    val profileViewDataHiv =
      profileViewDataMapper.transformInputToOutputModel(dto) as
        ProfileViewData.AppointmentProfileViewData
    with(profileViewDataHiv) {
      Assert.assertEquals("logicalId", logicalId)
      Assert.assertEquals("testName", name)
      Assert.assertEquals("testAddress", address)
      Assert.assertEquals("5y", age)
    }
  }

  @Test
  fun testMapToDomainModelAnc() {
    val dto =
      ProfileData.AncProfileData(
        logicalId = "logicalId",
        name = "testName",
        identifier = "testIdentifier()",
        address = "testAddress",
        age = "5y",
        gender = Enumerations.AdministrativeGender.MALE,
        birthdate = SimpleDateFormat("yyyy-MM-dd").parse("2021-05-25"),
        serviceStatus = ServiceStatus.DUE,
        services = emptyList(),
        tasks = emptyList(),
        conditions = emptyList(),
        flags = emptyList(),
        visits = emptyList()
      )
    val profileViewDataHiv =
      profileViewDataMapper.transformInputToOutputModel(dto) as
        ProfileViewData.PatientProfileViewData
    with(profileViewDataHiv) {
      Assert.assertEquals("logicalId", logicalId)
      Assert.assertEquals("testName", name)
      Assert.assertEquals("testIdentifier()", identifier)
      Assert.assertEquals("5y", age)
      Assert.assertEquals(true, tasks.isEmpty())
    }
  }

  @Test
  fun testMapToDomainModelFamily() {
    val dto =
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
    val profileViewDataHiv =
      profileViewDataMapper.transformInputToOutputModel(dto) as
        ProfileViewData.FamilyProfileViewData
    with(profileViewDataHiv) {
      Assert.assertEquals("logicalId", logicalId)
      Assert.assertEquals("testName Family", name)
      Assert.assertEquals("testAddress", address)
      Assert.assertEquals("5y", age)
    }
  }
}
