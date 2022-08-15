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

object PractitionerDetailsUtil {

  private const val PRACTITIONER_DETAILS_ENDPOINT = "practitioner-details"
  private const val PRACTITIONER_DETAILS_KEYCLOAK_UUID_PARAMETER = "keycloak-uuid"

  fun getUrl(keycloakUuid: String) =
    "$PRACTITIONER_DETAILS_ENDPOINT?$PRACTITIONER_DETAILS_KEYCLOAK_UUID_PARAMETER=$keycloakUuid"
}
