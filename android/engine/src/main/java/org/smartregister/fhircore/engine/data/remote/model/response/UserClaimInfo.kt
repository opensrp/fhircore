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

package org.smartregister.fhircore.engine.data.remote.model.response

import io.jsonwebtoken.Claims

data class UserClaimInfo(
  var questionnairePublisher: String? = null,
  var organization: String? = null,
  var location: String? = null,
  var keycloakUuid: String? = null,
  var givenName: String? = null,
  var fullName: String? = null,
  var practitionerId: String? = null,
) {
  companion object {
    fun parseFromClaims(claim: Claims): UserClaimInfo {
      return UserClaimInfo(
        keycloakUuid = claim["sub"].toString(),
        questionnairePublisher = claim["questionnaire_publisher"].toString(),
        organization = claim["organization"].toString(),
        location = claim["location"].toString(),
        fullName = claim["name"].toString(),
        givenName = claim["given_name"].toString(),
      )
    }
  }
}
