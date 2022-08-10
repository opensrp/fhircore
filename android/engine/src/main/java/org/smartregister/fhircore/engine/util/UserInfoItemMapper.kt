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

import javax.inject.Inject
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.model.practitioner.PractitionerDetails

class UserInfoItemMapper @Inject constructor(val sharedPreferencesHelper: SharedPreferencesHelper) :
  DomainMapper<PractitionerDetails, UserInfo> {

  override fun mapToDomainModel(dto: PractitionerDetails): UserInfo {
    val userData = dto.userDetail.userBioData
    var location = ""
    var familyName = ""
    var givenName = ""
    var name = ""
    var preferredUsername = ""

    if (!dto.fhirPractitionerDetails.locations.isNullOrEmpty())
      location = dto.fhirPractitionerDetails.locations.joinToString()

    if (userData.familyName.hasValue()) familyName = userData.familyName.valueAsString

    if (userData.givenName.hasValue()) givenName = userData.givenName.valueAsString

    if (userData.userName.hasValue()) name = userData.userName.valueAsString

    if (userData.preferredName.hasValue()) preferredUsername = userData.preferredName.valueAsString

    val domainModelSource =
      sharedPreferencesHelper.read(USER_INFO_SHARED_PREFERENCE_KEY, null)?.decodeJson<UserInfo>()

    return UserInfo(
      questionnairePublisher = domainModelSource!!.questionnairePublisher,
      organization = domainModelSource.organization,
      location = location,
      familyName = familyName,
      givenName = givenName,
      name = name,
      preferredUsername = preferredUsername,
      keyclockuuid = domainModelSource.keyclockuuid
    )
  }
}
