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

package org.smartregister.fhircore.engine.configuration.app

import kotlinx.serialization.Serializable

@Serializable
data class ApplicationConfiguration(
  var oauthServerBaseUrl: String,
  var fhirServerBaseUrl: String,
  var clientId: String = "",
  var clientSecret: String = "",
  var scope: String = "openid",
  var languages: List<String> = listOf("en"),
  var syncInterval: Long = 30
)

/**
 * A function providing a DSL for configuring [ApplicationConfiguration] used in a FHIR application
 *
 * @param oauthServerBaseUrl Sets the base URL for the authentication server. Usually the keycloak
 * base URL plus the realm. e.g https://keycloak.domain.org/auth/realms/<<real>>/
 * @param fhirServerBaseUrl Sets the base FHIR server URL for the application
 * @param clientId Sets the client identifier issued to the client during the registration process
 * on keycloak
 * @param clientSecret Sets the client secret issued to the client during the registration process
 * on keycloak
 * @param scope Sets the scope of the access request. It may have multiple space delimited values
 * @param languages Sets the languages for the app
 * @param syncInterval Sets the periodic sync interval in seconds. Default 30.
 */
fun applicationConfigurationOf(
  oauthServerBaseUrl: String = "",
  fhirServerBaseUrl: String = "",
  clientId: String = "",
  clientSecret: String = "",
  scope: String = "openid",
  languages: List<String> = listOf("en"),
  syncInterval: Long = 30
): ApplicationConfiguration =
  ApplicationConfiguration(
    oauthServerBaseUrl = oauthServerBaseUrl,
    fhirServerBaseUrl = fhirServerBaseUrl,
    clientId = clientId,
    clientSecret = clientSecret,
    scope = scope,
    languages = languages,
    syncInterval = syncInterval
  )
