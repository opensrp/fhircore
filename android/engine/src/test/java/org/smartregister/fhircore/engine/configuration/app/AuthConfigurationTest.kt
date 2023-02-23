/*
 * Copyright 2021-2023 Ona Systems, Inc
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

import org.junit.Assert
import org.junit.Test

class AuthConfigurationTest {

  @Test
  fun testAuthConfiguration() {
    val authConfiguration =
      AuthConfiguration(
        oauthServerBaseUrl = "https://keycloak-sampleorg",
        fhirServerBaseUrl = "https://fhir.sample.org/",
        clientId = "fhir client",
        clientSecret = "client secret",
        accountType = "openid"
      )
    Assert.assertEquals("https://keycloak-sampleorg", authConfiguration.oauthServerBaseUrl)
    Assert.assertEquals("https://fhir.sample.org/", authConfiguration.fhirServerBaseUrl)
    Assert.assertEquals("fhir client", authConfiguration.clientId)
    Assert.assertEquals("client secret", authConfiguration.clientSecret)
    Assert.assertEquals("openid", authConfiguration.accountType)
  }
}
