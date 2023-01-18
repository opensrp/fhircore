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

package org.smartregister.fhircore.engine.auth

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.BaseUnitTest

class AuthAndroidServiceTest : BaseUnitTest() {

  private val authAndroidService: org.smartregister.fhircore.quest.ui.login.AuthAndroidService =
    spyk(org.smartregister.fhircore.quest.ui.login.AuthAndroidService())

  private val accountAuthenticator: org.smartregister.fhircore.quest.ui.login.AccountAuthenticator =
    mockk(relaxed = true)

  @Test
  fun testOnBindFunctionShouldCallAuthenticatorBinder() {
    every { authAndroidService.accountAuthenticator } returns accountAuthenticator
    Assert.assertEquals(authAndroidService.onBind(null), accountAuthenticator.iBinder)
  }
}
