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
import org.smartregister.fhircore.anc.ui.login.LoginActivity
import org.smartregister.fhircore.engine.configuration.app.applicationConfigurationOf

class AncAuthenticationServiceTest : RobolectricTest() {

  private lateinit var eirAuthenticationService: AncAuthenticationService

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
    eirAuthenticationService = AncAuthenticationService(ApplicationProvider.getApplicationContext())
  }

  @Test
  fun testThatConfigsAreNotNull() {
    Assert.assertNotNull(eirAuthenticationService.getApplicationConfigurations())

    Assert.assertNotNull(eirAuthenticationService.clientId())
    Assert.assertEquals("clientId", eirAuthenticationService.clientId())

    Assert.assertNotNull(eirAuthenticationService.clientSecret())
    Assert.assertEquals("clientSecret", eirAuthenticationService.clientSecret())

    Assert.assertNotNull(eirAuthenticationService.providerScope())
    Assert.assertEquals("openid", eirAuthenticationService.providerScope())

    Assert.assertEquals(
      eirAuthenticationService.getLoginActivityClass().simpleName,
      LoginActivity::class.simpleName
    )

    Assert.assertEquals("org.smartregister.fhircore.anc", eirAuthenticationService.getAccountType())
  }
}
