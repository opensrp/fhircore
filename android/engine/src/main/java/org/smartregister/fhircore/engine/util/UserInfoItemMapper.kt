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

import org.smartregister.fhircore.engine.data.domain.util.DomainMapperWithDomainModelSource
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.model.practitioner.PractitionerDetails

object UserInfoItemMapper :
  DomainMapperWithDomainModelSource<PractitionerDetails, UserInfo, UserInfo> {

  override fun mapToDomainModel(dto: PractitionerDetails, domainModelSource: UserInfo): UserInfo {
    val userData = dto.userDetail.userBioData
    val location =
      if (dto.fhirPractitionerDetails.locationHierarchyList == null) ""
      else dto.fhirPractitionerDetails.locationHierarchyList.joinToString()
    val familyName = if (userData.familyName == null) "" else userData.familyName.valueAsString
    val givenName = if (userData.givenName == null) "" else userData.givenName.valueAsString
    val name = if (userData.userName == null) "" else userData.userName.valueAsString
    val preferredUsername =
      if (userData.preferredName == null) "" else userData.preferredName.valueAsString

    return UserInfo(
      questionnairePublisher = domainModelSource.questionnairePublisher,
      organization = domainModelSource.organization,
      location = location,
      familyName = familyName,
      givenName = givenName,
      name = name,
      preferredUsername = preferredUsername,
      sub = domainModelSource.sub
    )
  }
}
