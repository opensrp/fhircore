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

package org.smartregister.fhircore.quest

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.configuration.app.applicationConfigurationOf
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.shadow.QuestApplicationShadow
import org.smartregister.fhircore.quest.ui.login.LoginActivity

@Config(shadows = [QuestApplicationShadow::class])
class QuestAuthenticationServiceTest : RobolectricTest() {

  private lateinit var authenticationService: QuestAuthenticationService

  @Before
  fun setUp() {
    ReflectionHelpers.setField(
      ApplicationProvider.getApplicationContext(),
      "applicationConfiguration",
      applicationConfigurationOf().apply {
        clientId = "clientId"
        clientSecret = "clientSecret"
        scope = "openid"
      }
    )
    authenticationService = QuestAuthenticationService(ApplicationProvider.getApplicationContext())
  }

  @Test
  fun testThatConfigsAreNotNull() {
    Assert.assertNotNull(authenticationService.getApplicationConfigurations())

    Assert.assertNotNull(authenticationService.clientId())
    Assert.assertEquals("clientId", authenticationService.clientId())

    Assert.assertNotNull(authenticationService.clientSecret())
    Assert.assertEquals("clientSecret", authenticationService.clientSecret())

    Assert.assertNotNull(authenticationService.providerScope())
    Assert.assertEquals("openid", authenticationService.providerScope())

    Assert.assertEquals(
      authenticationService.getLoginActivityClass().simpleName,
      LoginActivity::class.simpleName
    )

    Assert.assertEquals("org.smartregister.fhircore.quest", authenticationService.getAccountType())

    Assert.assertFalse(authenticationService.skipLogin())
  }
}
