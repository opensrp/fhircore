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

package org.smartregister.fhircore.engine.util

import android.os.Looper
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.fhir.FhirEngine
import com.google.gson.Gson
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.CareTeam
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Shadows
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.encodeJson
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.model.practitioner.FhirPractitionerDetails
import org.smartregister.model.practitioner.KeycloakUserDetails
import org.smartregister.model.practitioner.PractitionerDetails
import org.smartregister.model.practitioner.UserBioData

@ExperimentalCoroutinesApi
@HiltAndroidTest
class PractitionerDetailsUtilsTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  private lateinit var practitionerDetailsUtils: PractitionerDetailsUtils

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  private val careTeamList: List<CareTeam> = getCareTeams()

  private val organizationList: List<Organization> = getOrganizations()

  private val locationList: List<Location> = getLocations()

  val fhirEngine = spyk<FhirEngine>()

  val gson = spyk<Gson>()

  lateinit var userInfoItemMapper: UserInfoItemMapper

  @Before
  fun setUp() {
    hiltRule.inject()
    // Spy needed to control interaction with the real injected dependency
    val fhirEngine = spyk<FhirEngine>()

    sharedPreferencesHelper = mockk()

    every { sharedPreferencesHelper.read(USER_INFO_SHARED_PREFERENCE_KEY, null) } returns
      getUserInfo().encodeJson()

    userInfoItemMapper = UserInfoItemMapper(sharedPreferencesHelper = sharedPreferencesHelper)

    practitionerDetailsUtils =
      PractitionerDetailsUtils(
        fhirEngine = fhirEngine,
        sharedPreferences = sharedPreferencesHelper,
        gson = gson,
        userInfoItemMapper = userInfoItemMapper
      )
  }

  @Test
  fun addParametersShouldAddParameters() {
    val parameters = Parameters()
    parameters.addParameter().apply {
      name = ResourceType.Practitioner.name
      value = StringType("123")
    }
    Assert.assertEquals(parameters.parameter.size, 1)
    practitionerDetailsUtils.addParameters(
      careTeamList,
      parameters = parameters,
      ResourceType.CareTeam.name
    )
    Assert.assertEquals(parameters.parameter.size, 2)
    practitionerDetailsUtils.addParameters(
      organizationList,
      parameters = parameters,
      ResourceType.Organization.name
    )
    Assert.assertEquals(parameters.parameter.size, 3)
    practitionerDetailsUtils.addParameters(
      locationList,
      parameters = parameters,
      ResourceType.Location.name
    )
    Assert.assertEquals(parameters.parameter.size, 4)
  }

  private fun getCareTeams(): List<CareTeam> {
    val listOfCareTeams = arrayListOf<CareTeam>()
    listOfCareTeams.add(
      CareTeam().apply {
        this.name = "abc"
        this.id = "204"
      }
    )
    return listOfCareTeams
  }

  private fun getOrganizations(): List<Organization> {
    val listOfOrganization = arrayListOf<Organization>()
    listOfOrganization.add(
      Organization().apply {
        this.name = "abc"
        this.id = "24"
      }
    )
    return listOfOrganization
  }

  private fun getLocations(): List<Location> {
    val listOfLocation = arrayListOf<Location>()
    listOfLocation.add(
      Location().apply {
        this.name = "abc"
        this.id = "20"
      }
    )
    return listOfLocation
  }

  @Test
  fun saveParameterShoutWriteParametersInSharedPreferences() {
    every { sharedPreferencesHelper.write(any(), any<String>()) } just runs

    practitionerDetailsUtils.saveParameter(
      practitionerId = "123",
      careTeamList = careTeamList,
      organizationList = organizationList,
      locationList = locationList
    )

    Shadows.shadowOf(Looper.getMainLooper()).idle()

    verify {
      sharedPreferencesHelper.write(
        PRACTITIONER_PARAMETERS_SHARED_PREFERENCE_KEY,
        getParameters().encodeResourceToString()
      )
    }
  }

  private fun getParameters(): Parameters {
    val parameters = Parameters()
    parameters.addParameter().apply {
      name = ResourceType.Practitioner.name
      value = StringType("123")
    }
    practitionerDetailsUtils.addParameters(
      careTeamList,
      parameters = parameters,
      ResourceType.CareTeam.name
    )
    practitionerDetailsUtils.addParameters(
      organizationList,
      parameters = parameters,
      ResourceType.Organization.name
    )
    practitionerDetailsUtils.addParameters(
      locationList,
      parameters = parameters,
      ResourceType.Location.name
    )
    return parameters
  }

  @Test
  fun storeUserPreferencesShoutWriteUserInfoInSharedPreferences() {
    every { sharedPreferencesHelper.write(any(), any<String>()) } just runs

    practitionerDetailsUtils.storeUserPreferences(userInfo = getUserInfo())

    Shadows.shadowOf(Looper.getMainLooper()).idle()

    verify {
      sharedPreferencesHelper.write(USER_INFO_SHARED_PREFERENCE_KEY, getUserInfo().encodeJson())
    }
  }

  @Test
  fun storeKeyClockInfoShoutWriteKeyClockInfoInSharedPreferences() {
    every { sharedPreferencesHelper.write(any(), any<String>()) } just runs

    practitionerDetailsUtils.storeKeyClockInfo(getPractitionerDetails())

    val gson = Gson()
    val keycloakUserDetails = gson.toJson(getKeycloakUserDetails())

    Shadows.shadowOf(Looper.getMainLooper()).idle()

    verify {
      sharedPreferencesHelper.write(KEY_CLOCK_INFO_SHARED_PREFERENCE_KEY, keycloakUserDetails)
    }
  }

  @Test
  fun testGetResourcesList() {
    coEvery { fhirEngine.load(CareTeam::class.java, any()) } returns careTeamList[0]
    coEvery { fhirEngine.load(Organization::class.java, any()) } returns organizationList[0]
    coEvery { fhirEngine.load(Location::class.java, any()) } returns locationList[0]

    runBlocking {
      val listCareTeam =
        practitionerDetailsUtils.getResourcesList(
          getParameters(),
          ResourceType.CareTeam.name,
          CareTeam::class.java
        )
      val listOrganization =
        practitionerDetailsUtils.getResourcesList(
          getParameters(),
          ResourceType.Organization.name,
          Organization::class.java
        )
      val listLocation =
        practitionerDetailsUtils.getResourcesList(
          getParameters(),
          ResourceType.Location.name,
          Location::class.java
        )
      Assert.assertNotNull(listCareTeam)
      Assert.assertNotNull(listOrganization)
      Assert.assertNotNull(listLocation)
    }
  }

  @Test
  fun testGetResourceFromPractitionerDetails() {
    every {
      sharedPreferencesHelper.read(PRACTITIONER_PARAMETERS_SHARED_PREFERENCE_KEY, "")
    } returns getParameters().encodeResourceToString()

    coEvery { fhirEngine.load(CareTeam::class.java, any()) } returns careTeamList[0]
    coEvery { fhirEngine.load(Organization::class.java, any()) } returns organizationList[0]
    coEvery { fhirEngine.load(Location::class.java, any()) } returns locationList[0]

    runBlocking {
      val listCareTeam =
        practitionerDetailsUtils.getResourceFromPractitionerDetails(ResourceType.CareTeam.name)
      val listOrganization =
        practitionerDetailsUtils.getResourceFromPractitionerDetails(ResourceType.Organization.name)
      val listLocation =
        practitionerDetailsUtils.getResourceFromPractitionerDetails(ResourceType.Location.name)
      val listUnknownType =
        practitionerDetailsUtils.getResourceFromPractitionerDetails(ResourceType.Practitioner.name)
      Assert.assertNotNull(listCareTeam)
      Assert.assertNotNull(listOrganization)
      Assert.assertNotNull(listLocation)
      Assert.assertNotNull(listUnknownType)
      Assert.assertEquals(listUnknownType.size, 0)
    }
  }

  @Test
  fun testRetrieveKeyClockDetails() {
    val gson = Gson()
    val keycloakUserDetails = gson.toJson(getKeycloakUserDetails())
    every { sharedPreferencesHelper.read(KEY_CLOCK_INFO_SHARED_PREFERENCE_KEY, "") } returns
      keycloakUserDetails

    runBlocking {
      val keycloakUserInfo = practitionerDetailsUtils.retrieveKeyClockInfo()

      Assert.assertNotNull(keycloakUserInfo)
    }
  }

  private fun getUserInfo(): UserInfo {
    val userInfo =
      UserInfo().apply {
        familyName = "abc"
        givenName = "xyx"
        questionnairePublisher = "ab"
        organization = "lm"
        name = "ab"
        keyclockuuid = "123"
      }
    return userInfo
  }

  private fun getKeycloakUserDetails(): KeycloakUserDetails {
    val userBioDataData =
      UserBioData().apply {
        familyName = StringType("abc")
        givenName = StringType("xyz")
        preferredName = StringType("ab")
        userName = StringType("axyz")
      }
    return KeycloakUserDetails().apply {
      id = "id_1"
      userBioData = userBioDataData
      roles = arrayListOf(StringType("abc"), StringType("xyz"))
    }
  }

  private fun getFhirPractitionerDetailsInfo(): FhirPractitionerDetails {
    return FhirPractitionerDetails().apply {
      id = "id_1"
      locations = locationList
      careTeams = careTeamList
      organizations = organizationList
    }
  }

  private fun getPractitionerDetails(): PractitionerDetails {
    return PractitionerDetails().apply {
      userDetail = getKeycloakUserDetails()
      fhirPractitionerDetails = getFhirPractitionerDetailsInfo()
    }
  }
}
