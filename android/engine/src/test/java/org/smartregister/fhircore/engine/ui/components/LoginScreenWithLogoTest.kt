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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.view.loginViewConfigurationOf
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.ui.login.APP_LOGO_TAG
import org.smartregister.fhircore.engine.ui.login.LoginErrorState
import org.smartregister.fhircore.engine.ui.login.LoginScreen
import org.smartregister.fhircore.engine.ui.login.LoginViewModel

class LoginScreenWithLogoTest : RobolectricTest() {

  @get:Rule val composeRule = createComposeRule()

  private lateinit var loginViewModelWithLogo: LoginViewModel
  private val username = MutableLiveData("")
  private val password = MutableLiveData("")
  private val loadingConfig = MutableLiveData(false)
  private val loginErrorState: LiveData<LoginErrorState?> = MutableLiveData(null)
  private val showProgressBar = MutableLiveData(false)
  private val loginConfig = loginViewConfigurationOf(showLogo = true)

  @Before
  fun setUp() {
    loginViewModelWithLogo =
      mockk {
        every { loginViewConfiguration } returns MutableLiveData(loginConfig)
        every { username } returns this@LoginScreenWithLogoTest.username
        every { password } returns this@LoginScreenWithLogoTest.password
        every { loginErrorState } returns this@LoginScreenWithLogoTest.loginErrorState
        every { showProgressBar } returns this@LoginScreenWithLogoTest.showProgressBar
        every { loadingConfig } returns this@LoginScreenWithLogoTest.loadingConfig
        every { onUsernameUpdated(any()) } answers
          {
            this@LoginScreenWithLogoTest.username.value = firstArg()
          }
        every { onPasswordUpdated(any()) } answers
          {
            this@LoginScreenWithLogoTest.password.value = firstArg()
          }
        every { attemptRemoteLogin() } returns Unit
      }
  }

  @Test
  fun testLoginScreenComponentsWithLogo() {

    composeRule.setContent { LoginScreen(loginViewModelWithLogo) }

    // verifying app logo properties
    composeRule.onNodeWithTag(APP_LOGO_TAG).assertExists()
    composeRule.onNodeWithTag(APP_LOGO_TAG).assertIsDisplayed()
  }
}
