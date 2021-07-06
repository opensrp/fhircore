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

package org.smartregister.fhircore.activity

import android.app.Activity
import android.content.Intent
import android.os.Looper.getMainLooper
import android.widget.Button
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.auth.secure.Credentials
import org.smartregister.fhircore.auth.secure.FakeKeyStore
import org.smartregister.fhircore.auth.secure.SecureConfig
import org.smartregister.fhircore.shadow.FhirApplicationShadow

@Config(shadows = [FhirApplicationShadow::class])
class LoginActivityTest : ActivityRobolectricTest() {

  private lateinit var secureConfig: SecureConfig
  private lateinit var loginActivity: LoginActivity

  @Before
  fun setUp() {
    loginActivity =
      Robolectric.buildActivity(LoginActivity::class.java, null).create().resume().get()

    secureConfig = SecureConfig(loginActivity.baseContext)

    loginActivity.viewModel.secureConfig = secureConfig
  }

  @Test
  fun testLoginActivityShouldNotBeNull() {
    Assert.assertNotNull(loginActivity)
  }

  @Test
  fun testLoginButtonShouldBeDisabledWithEmptyCredentials() {
    shadowOf(getMainLooper()).idle()

    val loginButton = loginActivity.findViewById<Button>(R.id.login_button)
    Assert.assertFalse(loginButton.isEnabled)
  }

  @Test
  fun testShouldEnableLoginShouldReturnFalseOnEmptyCredentials() {
    loginActivity.viewModel.loginUser.password = charArrayOf()
    loginActivity.viewModel.loginUser.username = ""

    var result = loginActivity.viewModel.shouldEnableLogin()
    Assert.assertFalse(result)

    loginActivity.viewModel.loginUser.password = charArrayOf('p', 'w')
    loginActivity.viewModel.loginUser.username = ""

    result = loginActivity.viewModel.shouldEnableLogin()
    Assert.assertFalse(result)

    loginActivity.viewModel.loginUser.password = charArrayOf()
    loginActivity.viewModel.loginUser.username = "testuser"

    result = loginActivity.viewModel.shouldEnableLogin()
    Assert.assertFalse(result)
  }

  @Test
  fun testShouldEnableLoginShouldReturnTrueOnNonEmptyCredentials() {
    loginActivity.viewModel.loginUser.password = charArrayOf('p', 'w')
    loginActivity.viewModel.loginUser.username = "testuser"

    val result = loginActivity.viewModel.shouldEnableLogin()
    Assert.assertTrue(result)
  }

  @Test
  fun testAllowLocalLoginShouldReturnFalseOnNonExistentCredentials() {
    val result = loginActivity.viewModel.allowLocalLogin()
    Assert.assertFalse(result)
  }

  @Test
  fun testAllowLocalLoginShouldReturnFalseOnInvalidCredentials() {
    val savedCreds = Credentials("invaliduser", charArrayOf('x', 'y'), "any token")

    secureConfig.saveCredentials(savedCreds)

    loginActivity.viewModel.loginUser.password = charArrayOf('p', 'w')
    loginActivity.viewModel.loginUser.username = "testuser"

    val result = loginActivity.viewModel.allowLocalLogin()
    Assert.assertFalse(result)
  }

  @Test
  fun testAllowLocalLoginShouldReturnTrueOnValidCredentials() {
    shadowOf(getMainLooper()).idle()

    val savedCreds = Credentials("testuser", charArrayOf('p', 'w'), "any token")

    secureConfig.saveCredentials(savedCreds)

    val c = secureConfig.retrieveCredentials()

    loginActivity.viewModel.loginUser.password = charArrayOf('p', 'w')
    loginActivity.viewModel.loginUser.username = "testuser"

    val result = loginActivity.viewModel.allowLocalLogin()
    Assert.assertTrue(result)
  }

  @Test
  fun testVerifyStartedActivity() {
    loginActivity.viewModel.goHome.value = true
    val expectedIntent = Intent(loginActivity, PatientListActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<FhirApplication>()).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  override fun getActivity(): Activity {
    return loginActivity
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}
