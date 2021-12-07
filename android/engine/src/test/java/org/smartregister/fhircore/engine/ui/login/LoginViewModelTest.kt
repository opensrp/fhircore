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

package org.smartregister.fhircore.engine.ui.login

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.spyk
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.FakeModel.authCredentials
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@ExperimentalCoroutinesApi
@HiltAndroidTest
internal class LoginViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 2) val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

  @Inject lateinit var accountAuthenticator: AccountAuthenticator

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  private lateinit var loginViewModel: LoginViewModel

  private lateinit var accountAuthenticatorSpy: AccountAuthenticator

  @Before
  fun setUp() {
    hiltRule.inject()
    // Spy needed to control interaction with the real injected dependency
    accountAuthenticatorSpy = spyk(accountAuthenticator)

    loginViewModel =
      spyk(
        LoginViewModel(
          accountAuthenticator = accountAuthenticatorSpy,
          dispatcher = dispatcherProvider,
          sharedPreferences = sharedPreferencesHelper
        )
      )
  }

  @After
  fun tearDown() {
    accountAuthenticatorSpy.secureSharedPreference.deleteCredentials()
  }

  @Test
  fun testAttemptLocalLoginWithCorrectCredentials() {
    // Simulate saving of credentials prior to login
    accountAuthenticatorSpy.secureSharedPreference.saveCredentials(authCredentials)

    // Provide username and password (The saved password is hashed, actual one is needed)
    loginViewModel.run {
      onUsernameUpdated(authCredentials.username)
      onPasswordUpdated("51r1K4l1")
    }

    val successfulLocalLogin = loginViewModel.attemptLocalLogin()
    Assert.assertTrue(successfulLocalLogin)
  }
}
