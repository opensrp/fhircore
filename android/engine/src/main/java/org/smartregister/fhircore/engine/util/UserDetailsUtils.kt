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

import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton
import org.smartregister.fhircore.engine.data.remote.model.response.KeycloakUserDetails
import org.smartregister.fhircore.engine.data.remote.model.response.PractitionerDetails
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.util.extension.encodeJson

@Singleton
class UserDetailsUtils @Inject constructor(val sharedPreferences: SharedPreferencesHelper) {

  private fun storeUserPreferences(userInfo: UserInfo) {
    sharedPreferences.write(USER_INFO_SHARED_PREFERENCE_KEY, userInfo.encodeJson())
  }

  fun updateUserDetailsFromPractitionerDetails(
    practitionerDetails: PractitionerDetails,
    userResponse: UserInfo
  ) {
    storeUserPreferences(
      userInfo =
        UserInfoItemMapper.mapToDomainModel(practitionerDetails, domainModelSource = userResponse)
    )
  }

  fun storeKeyClockInfo(practitionerDetails: PractitionerDetails) {
    val userData = practitionerDetails.userDetail as KeycloakUserDetails
    val gson = Gson()
    val json = gson.toJson(userData)
    sharedPreferences.write(KEY_CLOCK_INFO_SHARED_PREFERENCE_KEY, json)
  }

  fun retrieveKeyClockInfo(): KeycloakUserDetails {
    val keycloakUserDetailsString =
      sharedPreferences.read(KEY_CLOCK_INFO_SHARED_PREFERENCE_KEY, "")!!
    val gson = Gson()

    return gson.fromJson(keycloakUserDetailsString, KeycloakUserDetails::class.java)
  }
}
