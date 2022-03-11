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

import io.mockk.every
import io.mockk.mockk
import javax.inject.Inject
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.encodeJson
import org.smartregister.model.practitioner.FhirPractitionerDetails
import org.smartregister.model.practitioner.KeycloakUserDetails
import org.smartregister.model.practitioner.PractitionerDetails
import org.smartregister.model.practitioner.UserBioData

class UserInfoItemMapperTest : RobolectricTest() {

  lateinit var userInfoItemMapper: UserInfoItemMapper

  private val keycloakUserDetails: KeycloakUserDetails = getKeycloakUserDetails()

  private val keycloakUserDetailsEmptyNames: KeycloakUserDetails = getKeycloakUserDetailsEmptyName()

  private val fhirPractitionerDetail: FhirPractitionerDetails = getFhirPractitionerDetails()

  private val fhirPractitionerDetailEmptyLocation: FhirPractitionerDetails =
    getFhirPractitionerDetailsEmptyLocation()

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @Before
  fun setUp() {

    sharedPreferencesHelper = mockk()

    every { sharedPreferencesHelper.read(USER_INFO_SHARED_PREFERENCE_KEY, null) } returns
      getUserInfo().encodeJson()

    userInfoItemMapper = UserInfoItemMapper(sharedPreferencesHelper = sharedPreferencesHelper)
  }

  @Test
  fun testMapToDomainModel() {
    val practitionerDetails =
      PractitionerDetails().apply {
        userDetail = keycloakUserDetails
        fhirPractitionerDetails = fhirPractitionerDetail
      }
    val userInfo = userInfoItemMapper.mapToDomainModel(dto = practitionerDetails)
    verifyUserDetails(userInfo)
  }

  @Test
  fun testMapToDomainModelWithEmptyDate() {
    val practitionerDetails =
      PractitionerDetails().apply {
        userDetail = keycloakUserDetailsEmptyNames
        fhirPractitionerDetails = fhirPractitionerDetailEmptyLocation
      }
    val userInfo = userInfoItemMapper.mapToDomainModel(dto = practitionerDetails)
    verifyUserDetailsEmpty(userInfo)
  }

  private fun verifyUserDetailsEmpty(userInfo: UserInfo) {
    with(userInfo) {
      Assert.assertEquals("123", keyclockuuid)
      Assert.assertEquals("", name)
      Assert.assertEquals("", familyName)
      Assert.assertEquals("", givenName)
      Assert.assertEquals("", preferredUsername)
      Assert.assertEquals("", location)
    }
  }

  private fun verifyUserDetails(userInfo: UserInfo) {
    with(userInfo) {
      Assert.assertEquals("123", keyclockuuid)
      Assert.assertEquals("axyz", name)
      Assert.assertEquals("abc", familyName)
      Assert.assertEquals("xyz", givenName)
      Assert.assertEquals("ab", preferredUsername)
      Assert.assertEquals(fhirPractitionerDetail.locations.joinToString(), location)
    }
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
  private fun getFhirPractitionerDetails(): FhirPractitionerDetails {
    val location =
      Location().apply {
        description = "hello"
        address = Address()
      }
    return FhirPractitionerDetails().apply {
      id = "id_1"
      locations = arrayListOf(location)
    }
  }

  private fun getUserInfo(): UserInfo {
    return UserInfo().apply {
      familyName = "abc"
      givenName = "xyx"
      questionnairePublisher = "ab"
      organization = "lm"
      name = "ab"
      keyclockuuid = "123"
    }
  }

  private fun getFhirPractitionerDetailsEmptyLocation(): FhirPractitionerDetails {
    return FhirPractitionerDetails().apply {
      id = "id_1"
      locations = arrayListOf()
    }
  }

  private fun getKeycloakUserDetailsEmptyName(): KeycloakUserDetails {
    val userBioDataData =
      UserBioData().apply {
        familyName = StringType("")
        givenName = StringType("")
        preferredName = StringType("")
        userName = StringType("")
      }
    return KeycloakUserDetails().apply {
      id = "id_1"
      userBioData = userBioDataData
      roles = arrayListOf(StringType("abc"), StringType("xyz"))
    }
  }
}
