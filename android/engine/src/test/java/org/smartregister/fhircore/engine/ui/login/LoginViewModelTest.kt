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

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import okhttp3.ResponseBody
import okhttp3.internal.http.RealResponseBody
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.auth.AuthenticationService
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.configuration.view.loginViewConfigurationOf
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.shadow.FakeKeyStore
import org.smartregister.fhircore.engine.util.USER_INFO_SHARED_PREFERENCE_KEY
import org.smartregister.fhircore.engine.util.extension.encodeJson
import retrofit2.Response

@ExperimentalCoroutinesApi
internal class LoginViewModelTest : RobolectricTest() {

  @get:Rule val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

  @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

  private val application: Application = ApplicationProvider.getApplicationContext()

  private lateinit var loginViewModel: LoginViewModel

  private lateinit var configurableApplication: ConfigurableApplication

  private lateinit var authenticationService: AuthenticationService

  companion object {
    @JvmStatic
    @BeforeClass
    fun resetMocks() {
      FakeKeyStore.setup
    }
  }

  @Before
  fun setUp() {
    configurableApplication = application as ConfigurableApplication
    authenticationService = spyk(configurableApplication.authenticationService)
    loginViewModel =
      spyk(
        objToCopy =
          LoginViewModel(
            application = ApplicationProvider.getApplicationContext(),
            authenticationService = authenticationService,
            loginViewConfiguration = loginViewConfigurationOf(),
            dispatcher = coroutineTestRule.testDispatcherProvider
          ),
        recordPrivateCalls = true
      )
  }

  @After
  fun tearDown() {
    // Reset defaults after every test
    loginViewModel.run {
      updateViewConfigurations(loginViewConfigurationOf())
      onUsernameUpdated("")
      onPasswordUpdated("")
    }
  }

  @Test
  fun testLoginUserNavigateToHomeWithActiveSession() =
    coroutineTestRule.runBlockingTest {
      every { authenticationService.hasActiveSession() } returns true
      every { authenticationService.skipLogin() } returns true

      loginViewModel.loginUser()

      // Navigate home is set to true
      Assert.assertNotNull(loginViewModel.navigateToHome.value)
      Assert.assertTrue(loginViewModel.navigateToHome.value!!)
    }

  @Test
  fun testLoginUserShouldTryLoadActiveWithNonActiveSession() =
    coroutineTestRule.runBlockingTest {
      every { authenticationService.hasActiveSession() } returns false
      every { authenticationService.skipLogin() } returns false

      loginViewModel.loginUser()

      verify { authenticationService.loadActiveAccount(any(), any()) }
    }

  @Test
  fun testThatViewModelIsInitialized() {
    Assert.assertNotNull(loginViewModel)
  }

  @Test
  fun testOnPasswordChanged() {
    val newPassword = "NewP455W0rd"
    loginViewModel.onPasswordUpdated(newPassword)
    Assert.assertNotNull(loginViewModel.password.value)
    Assert.assertEquals(newPassword, loginViewModel.password.value)
  }

  @Test
  fun testOnUsernameChanged() {
    val username = "username"
    loginViewModel.onUsernameUpdated(username)
    Assert.assertNotNull(loginViewModel.username.value)
    Assert.assertEquals(username, loginViewModel.username.value)
  }

  @Test
  fun testApplicationConfiguration() {
    val coolAppName = "Cool App"
    val versionCode = 4
    val versionName = "0.1.0-preview"
    loginViewModel.updateViewConfigurations(
      loginViewConfigurationOf(
        applicationName = coolAppName,
        applicationVersion = versionName,
        applicationVersionCode = versionCode
      )
    )
    Assert.assertNotNull(loginViewModel.loginViewConfiguration.value)
    Assert.assertEquals(coolAppName, loginViewModel.loginViewConfiguration.value?.applicationName)
    Assert.assertEquals(
      versionCode,
      loginViewModel.loginViewConfiguration.value?.applicationVersionCode
    )
    Assert.assertEquals(
      versionName,
      loginViewModel.loginViewConfiguration.value?.applicationVersion
    )
  }

  @Test
  fun testResponseBodyHandlerWithSuccessfulResponse() {
    val realResponseBody = spyk(RealResponseBody("", 10, spyk()))
    val userResponse = UserInfo("G6PD")
    every { realResponseBody.string() } returns userResponse.encodeJson()
    val response: Response<ResponseBody> = spyk(Response.success(realResponseBody))
    loginViewModel.responseBodyHandler.handleResponse(spyk(), response)

    // Shared preference saved G6PD
    Assert.assertEquals(
      userResponse.questionnairePublisher,
      loginViewModel.sharedPreferences.read(
        USER_INFO_SHARED_PREFERENCE_KEY,
        null
      )
    )
    Assert.assertNotNull(loginViewModel.showProgressBar.value)
    Assert.assertFalse(loginViewModel.showProgressBar.value!!)
  }

  @Test
  fun testResponseBodyHandlerWithFailedResponse() {
    val errorMessage = "We have a problem"
    loginViewModel.responseBodyHandler.handleFailure(spyk(), IllegalStateException(errorMessage))
    // Login error shared
    Assert.assertNotNull(loginViewModel.loginError.value)
    Assert.assertTrue(loginViewModel.loginError.value!!.isNotEmpty())
    Assert.assertEquals(errorMessage, loginViewModel.loginError.value)
    Assert.assertNotNull(loginViewModel.showProgressBar.value)
    Assert.assertFalse(loginViewModel.showProgressBar.value!!)
  }

  @Test
  fun testOauthResponseHandlerWithFailureWithSuccessfulPreviousLogin() {
    // set username = 'demo' and password = 'Amani123' for local login
    loginViewModel.onUsernameUpdated("demo")
    loginViewModel.onPasswordUpdated("Amani123")

    every { authenticationService.validLocalCredentials(any(), any()) } returns true

    val errorMessage = "We have a problem login you in"
    loginViewModel.oauthResponseHandler.handleFailure(spyk(), IllegalStateException(errorMessage))

    // Direct user to register screen instead
    Assert.assertNotNull(loginViewModel.navigateToHome.value)
    Assert.assertTrue(loginViewModel.navigateToHome.value!!)
  }

  @Test
  fun testOauthResponseHandlerWithFailureWithFailedPreviousLogin() {
    // set username = 'demo' and password = 'Amani123' for local login
    loginViewModel.onUsernameUpdated("demo")
    loginViewModel.onPasswordUpdated("Amani123")

    val errorMessage = "We have a problem login you in"
    loginViewModel.oauthResponseHandler.handleFailure(spyk(), IllegalStateException(errorMessage))

    // Show error message
    Assert.assertNotNull(loginViewModel.loginError.value)
    Assert.assertTrue(loginViewModel.loginError.value!!.isNotEmpty())
    Assert.assertEquals(errorMessage, loginViewModel.loginError.value)
    Assert.assertNotNull(loginViewModel.showProgressBar.value)
    Assert.assertFalse(loginViewModel.showProgressBar.value!!)
  }

  @Test
  fun testAttemptRemoteLoginTrimsWhiteSpaceForCredentialsEntered() =
    coroutineTestRule.runBlockingTest {
      val username = "test "
      val password = "Test123"

      every { loginViewModel.username.value } returns username
      every { loginViewModel.password.value } returns password

      every { authenticationService.fetchToken(any(), any()) } returns mockk(relaxed = true)

      loginViewModel.attemptRemoteLogin()

      verify { authenticationService.fetchToken("test", "Test123".toCharArray()) }
    }
}
