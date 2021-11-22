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

package org.smartregister.fhircore.anc

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.configuration.app.applicationConfigurationOf

class AncAuthenticationServiceTest : RobolectricTest() {

  private lateinit var ancAuthenticationService: AncAuthenticationService

  @Before
  fun setUp() {
    ReflectionHelpers.setField(
      ApplicationProvider.getApplicationContext(),
      "applicationConfiguration",
      applicationConfigurationOf().apply {
        clientId = "clientId"
        clientSecret = "clientSecret"
        scope = "openid"
        languages = listOf("en", "sw")
      }
    )
    ancAuthenticationService = AncAuthenticationService(ApplicationProvider.getApplicationContext())
  }

  @Test
  fun testThatConfigsAreNotNull() {
    Assert.assertNotNull(ancAuthenticationService.getApplicationConfigurations())

    Assert.assertNotNull(ancAuthenticationService.clientId())
    Assert.assertEquals("clientId", ancAuthenticationService.clientId())

    Assert.assertNotNull(ancAuthenticationService.clientSecret())
    Assert.assertEquals("clientSecret", ancAuthenticationService.clientSecret())

    Assert.assertNotNull(ancAuthenticationService.providerScope())
    Assert.assertEquals("openid", ancAuthenticationService.providerScope())

    Assert.assertEquals(
      ancAuthenticationService.getLoginActivityClass().simpleName,
      LoginActivity::class.simpleName
    )

    Assert.assertEquals("org.smartregister.fhircore.anc", ancAuthenticationService.getAccountType())
  }
}
