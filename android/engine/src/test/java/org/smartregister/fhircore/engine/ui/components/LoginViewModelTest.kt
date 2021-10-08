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

package org.smartregister.fhircore.engine.ui.components

import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.auth.AuthenticationService
import org.smartregister.fhircore.engine.configuration.view.loginViewConfigurationOf
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.ui.login.LoginViewModel

class LoginViewModelTest : RobolectricTest() {
  lateinit var loginViewModel: LoginViewModel
  lateinit var authenticationService: AuthenticationService

  @Before
  fun setup() {
    authenticationService = mockk()
    loginViewModel =
      LoginViewModel(
        ApplicationProvider.getApplicationContext(),
        authenticationService,
        loginViewConfigurationOf()
      )
  }

  @Test
  fun testAttemptLocalLoginShouldValidateLocalCredentials() {
    every {
      authenticationService.validLocalCredentials("testuser", "testpw".toCharArray())
    } returns true

    loginViewModel.onUsernameUpdated("testuser")
    loginViewModel.onPasswordUpdated("testpw")

    val result = ReflectionHelpers.callInstanceMethod<Boolean>(loginViewModel, "attemptLocalLogin")

    Assert.assertTrue(result)

    verify { authenticationService.validLocalCredentials(any(), any()) }
  }

  @Test
  fun testAttemptLocalLoginShouldReturnFalseForInvalidLocalCredentials() {
    every {
      authenticationService.validLocalCredentials("testuser", "invalid".toCharArray())
    } returns false

    loginViewModel.onUsernameUpdated("testuser")
    loginViewModel.onPasswordUpdated("invalid")

    val result = ReflectionHelpers.callInstanceMethod<Boolean>(loginViewModel, "attemptLocalLogin")

    Assert.assertFalse(result)

    verify { authenticationService.validLocalCredentials(any(), any()) }
  }

  @Test
  fun testLoginUserNavigateToHomeWithActiveSession() {
    every { authenticationService.hasActiveSession() } returns true
    every { authenticationService.skipLogin() } returns false

    loginViewModel.loginUser()

    verify(timeout = 2000) { authenticationService.hasActiveSession() }
    verify(inverse = true, timeout = 2000) { authenticationService.loadActiveAccount(any(), any()) }
  }

  @Test
  fun testLoginUserShouldTryLoadActiveWithNonActiveSession() {
    every { authenticationService.hasActiveSession() } returns false
    every { authenticationService.skipLogin() } returns false

    loginViewModel.loginUser()

    verify(timeout = 2000) { authenticationService.hasActiveSession() }
    verify(timeout = 2000) { authenticationService.loadActiveAccount(any(), any()) }
  }
}
