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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import java.io.PrintStream
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.internal.http.RealResponseBody
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Organization
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.engine.HiltActivityForTest
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.auth.KeycloakService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.data.remote.model.response.OAuthResponse
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.data.remote.shared.TokenAuthenticator
import org.smartregister.fhircore.engine.robolectric.AccountManagerShadow
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.isDeviceOnline
import org.smartregister.model.practitioner.FhirPractitionerDetails
import org.smartregister.model.practitioner.PractitionerDetails
import retrofit2.HttpException
import retrofit2.Response

@ExperimentalCoroutinesApi
@HiltAndroidTest
@Config(shadows = [AccountManagerShadow::class])
internal class LoginViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 2) val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

  private val accountAuthenticator: AccountAuthenticator = mockk()

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @Inject lateinit var secureSharedPreference: SecureSharedPreference

  private lateinit var loginViewModel: LoginViewModel
  private val fhirResourceService = mockk<FhirResourceService>()
  private val keycloakService = mockk<KeycloakService>()
  private val resourceService: FhirResourceService = mockk()
  private val defaultRepository: DefaultRepository = mockk(relaxed = true)
  private lateinit var fhirResourceDataSource: FhirResourceDataSource
  private val tokenAuthenticator = mockk<TokenAuthenticator>()

  private val thisUsername = "demo"
  private val thisPassword = "paswd"

  @Before
  fun setUp() {
    hiltRule.inject()

    fhirResourceDataSource = spyk(FhirResourceDataSource(resourceService))

    loginViewModel =
      spyk(
        LoginViewModel(
          accountAuthenticator = accountAuthenticator,
          sharedPreferences = sharedPreferencesHelper,
          defaultRepository = defaultRepository,
          keycloakService = keycloakService,
          fhirResourceService = fhirResourceService,
          tokenAuthenticator = tokenAuthenticator,
          secureSharedPreference = secureSharedPreference,
          dispatcherProvider = coroutineTestRule.testDispatcherProvider,
          fhirResourceDataSource = fhirResourceDataSource
        )
      )
  }

  @After
  fun tearDown() {
    secureSharedPreference.deleteCredentials()
  }

  @Test
  fun testSuccessfulOfflineLogin() = runTest {
    val activity = mockedActivity()

    updateCredentials()

    every {
      accountAuthenticator.validateLoginCredentials(thisUsername, thisPassword.toCharArray())
    } returns true

    every {
      tokenAuthenticator.validateSavedLoginCredentials(thisUsername, thisPassword.toCharArray())
    } returns true

    loginViewModel.login(activity, scope = this)
    advanceUntilIdle()

    Assert.assertNull(loginViewModel.loginErrorState.value)
    Assert.assertFalse(loginViewModel.showProgressBar.value!!)
    Assert.assertTrue(loginViewModel.navigateToHome.value!!)
  }
  @Test
  fun testUnSuccessfulOfflineLogin() = runTest {
    val activity = mockedActivity()

    updateCredentials()

    every {
      accountAuthenticator.validateLoginCredentials(thisUsername, thisPassword.toCharArray())
    } returns false

    every {
      tokenAuthenticator.validateSavedLoginCredentials(thisUsername, thisPassword.toCharArray())
    } returns false

    loginViewModel.login(activity, scope = this)
    advanceUntilIdle()

    Assert.assertNotNull(loginViewModel.loginErrorState.value)
    Assert.assertFalse(loginViewModel.showProgressBar.value!!)
    Assert.assertEquals(LoginErrorState.INVALID_CREDENTIALS, loginViewModel.loginErrorState.value!!)
  }

  @Test
  fun testSuccessfulOnlineLoginWithActiveSessionWithSavedPractitionerDetails() = runTest {
    updateCredentials()
    sharedPreferencesHelper.write(
      SharedPreferenceKey.PRACTITIONER_ID.name,
      value = "9807A290-0572-40E7-9EE0-C60F729E9F09"
    )
    every { tokenAuthenticator.sessionActive() } returns true
    every {
      accountAuthenticator.validateLoginCredentials(thisUsername, thisPassword.toCharArray())
    } returns true

    every {
      tokenAuthenticator.validateSavedLoginCredentials(thisUsername, thisPassword.toCharArray())
    } returns true

    loginViewModel.login(mockedActivity(isDeviceOnline = true), scope = this)
    advanceUntilIdle()

    Assert.assertFalse(loginViewModel.showProgressBar.value!!)
    Assert.assertTrue(loginViewModel.navigateToHome.value!!)
  }

  @Test
  fun testSuccessfulOnlineLoginWithActiveSessionWithNoPractitionerDetailsSaved() = runTest {
    updateCredentials()
    every { tokenAuthenticator.sessionActive() } returns true
    coEvery {
      tokenAuthenticator.fetchAccessToken(thisUsername, thisPassword.toCharArray())
    } returns
      Result.success(
        OAuthResponse(
          accessToken = "very_new_top_of_the_class_access_token",
          tokenType = "you_guess_it",
          refreshToken = "another_very_refreshing_token",
          refreshExpiresIn = 540000,
          scope = "open_my_guy"
        )
      )
    coEvery { keycloakService.fetchUserInfo() } returns
      Response.success(UserInfo(keycloakUuid = "awesome_uuid"))
    val bundle = Bundle()
    val bundleEntry = Bundle.BundleEntryComponent().apply { resource = practitionerDetails() }
    coEvery { fhirResourceService.getResource(any()) } returns bundle.addEntry(bundleEntry)

    loginViewModel.login(mockedActivity(isDeviceOnline = true), scope = this)
    advanceUntilIdle()

    Assert.assertTrue(loginViewModel.navigateToHome.value!!)
  }

  @Test
  fun testUnSuccessfulOnlineLoginUsingDifferentUsername() = runTest {
    updateCredentials()
    secureSharedPreference.saveCredentials("nativeUser", "n4t1veP5wd".toCharArray())
    every { tokenAuthenticator.sessionActive() } returns false
    loginViewModel.login(mockedActivity(isDeviceOnline = true), scope = this)
    //    advanceUntilIdle()

    Assert.assertFalse(loginViewModel.showProgressBar.value!!)
    Assert.assertEquals(
      LoginErrorState.MULTI_USER_LOGIN_ATTEMPT,
      loginViewModel.loginErrorState.value!!
    )
  }

  @Test
  fun testUnSuccessfulOnlineLoginUsingDifferentUsernameWithActiveSession() = runTest {
    updateCredentials()
    secureSharedPreference.saveCredentials("nativeUser", "n4t1veP5wd".toCharArray())
    sharedPreferencesHelper.write(
      SharedPreferenceKey.PRACTITIONER_ID.name,
      value = "9807A290-0572-40E7-9EE0-C60F729E9F09"
    )
    every { tokenAuthenticator.sessionActive() } returns true
    loginViewModel.login(mockedActivity(isDeviceOnline = true), scope = this)
    advanceUntilIdle()

    Assert.assertFalse(loginViewModel.showProgressBar.value!!)
    Assert.assertEquals(
      LoginErrorState.MULTI_USER_LOGIN_ATTEMPT,
      loginViewModel.loginErrorState.value!!
    )
  }

  @Test
  fun testSuccessfulNewOnlineLoginShouldFetchUserInfoAndPractitioner() = runTest {
    updateCredentials()
    secureSharedPreference.saveCredentials(thisUsername, thisPassword.toCharArray())
    every { tokenAuthenticator.sessionActive() } returns false
    coEvery {
      tokenAuthenticator.fetchAccessToken(thisUsername, thisPassword.toCharArray())
    } returns
      Result.success(
        OAuthResponse(
          accessToken = "very_new_top_of_the_class_access_token",
          tokenType = "you_guess_it",
          refreshToken = "another_very_refreshing_token",
          refreshExpiresIn = 540000,
          scope = "open_my_guy"
        )
      )

    // Mock result for fetch user info via keycloak endpoint
    coEvery { keycloakService.fetchUserInfo() } returns
      Response.success(UserInfo(keycloakUuid = "awesome_uuid"))

    // Mock result for retrieving a FHIR resource using user's keycloak uuid
    val bundle = Bundle()
    val bundleEntry = Bundle.BundleEntryComponent().apply { resource = practitionerDetails() }
    coEvery { fhirResourceService.getResource(any()) } returns bundle.addEntry(bundleEntry)

    loginViewModel.login(mockedActivity(isDeviceOnline = true), scope = this)
    advanceUntilIdle()

    Assert.assertFalse(loginViewModel.showProgressBar.value!!)
    Assert.assertTrue(loginViewModel.navigateToHome.value!!)

    // Login was successful savePractitionerDetails was called
    val bundleSlot = slot<Bundle>()
    coVerify { loginViewModel.savePractitionerDetails(capture(bundleSlot)) }

    Assert.assertNotNull(bundleSlot.captured)
    Assert.assertTrue(bundleSlot.captured.entry.isNotEmpty())
    Assert.assertTrue(bundleSlot.captured.entry[0].resource is PractitionerDetails)
  }

  @Test
  fun testLoginFailsWithInvalidCredentialsForHttpException401() = runTest {
    every { tokenAuthenticator.sessionActive() } returns false
    val http401 = mockk<HttpException>()
    every { http401.code() } returns 401
    every { http401.printStackTrace(any<PrintStream>()) } just runs
    coEvery {
      tokenAuthenticator.fetchAccessToken(thisUsername, thisPassword.toCharArray())
    } returns Result.failure(http401)
    updateCredentials()

    loginViewModel.login(mockedActivity(isDeviceOnline = true), scope = this)
    advanceUntilIdle()

    Assert.assertFalse(loginViewModel.showProgressBar.value!!)
    Assert.assertEquals(LoginErrorState.INVALID_CREDENTIALS, loginViewModel.loginErrorState.value!!)
  }

  @Test
  fun testUnSuccessfulOnlineLoginUserInfoNotFetched() = runTest {
    updateCredentials()
    secureSharedPreference.saveCredentials(thisUsername, thisPassword.toCharArray())
    every { tokenAuthenticator.sessionActive() } returns false
    coEvery {
      tokenAuthenticator.fetchAccessToken(thisUsername, thisPassword.toCharArray())
    } returns
      Result.success(
        OAuthResponse(
          accessToken = "very_new_top_of_the_class_access_token",
          tokenType = "you_guess_it",
          refreshToken = "another_very_refreshing_token",
          refreshExpiresIn = 540000,
          scope = "open_my_guy"
        )
      )

    // Mock result for fetch user info via keycloak endpoint
    coEvery { keycloakService.fetchUserInfo() } returns
      Response.error(400, mockk<RealResponseBody>(relaxed = true))

    // Mock result for retrieving a FHIR resource using user's keycloak uuid
    coEvery { fhirResourceService.getResource(any()) } returns Bundle()

    loginViewModel.login(mockedActivity(isDeviceOnline = true), scope = this)
    advanceUntilIdle()

    Assert.assertFalse(loginViewModel.showProgressBar.value!!)
    Assert.assertEquals(LoginErrorState.ERROR_FETCHING_USER, loginViewModel.loginErrorState.value!!)
  }

  @Test
  fun testUnSuccessfulOnlineLoginPractitionerDetailsNotFetchedException() = runTest {
    updateCredentials()
    secureSharedPreference.saveCredentials(thisUsername, thisPassword.toCharArray())
    every { tokenAuthenticator.sessionActive() } returns false
    coEvery {
      tokenAuthenticator.fetchAccessToken(thisUsername, thisPassword.toCharArray())
    } returns
      Result.success(
        OAuthResponse(
          accessToken = "very_new_top_of_the_class_access_token",
          tokenType = "you_guess_it",
          refreshToken = "another_very_refreshing_token",
          refreshExpiresIn = 540000,
          scope = "open_my_guy"
        )
      )

    // Mock result for fetch user info via keycloak endpoint
    coEvery { keycloakService.fetchUserInfo() } returns
      Response.success(UserInfo(keycloakUuid = "awesome_uuid"))

    // Mock result for retrieving a FHIR resource using user's keycloak uuid
    coEvery { fhirResourceService.getResource(any()) } throws Exception()

    loginViewModel.login(mockedActivity(isDeviceOnline = true), scope = this)
    advanceUntilIdle()

    Assert.assertFalse(loginViewModel.showProgressBar.value!!)
    Assert.assertEquals(LoginErrorState.ERROR_FETCHING_USER, loginViewModel.loginErrorState.value!!)
  }

  @Test
  fun testUnSuccessfulOnlineLoginWhenAccessTokenNotReceived() = runTest {
    updateCredentials()
    secureSharedPreference.saveCredentials(thisUsername, thisPassword.toCharArray())
    every { tokenAuthenticator.sessionActive() } returns false
    coEvery {
      tokenAuthenticator.fetchAccessToken(thisUsername, thisPassword.toCharArray())
    } returns Result.failure(UnknownHostException())

    // Mock result for fetch user info via keycloak endpoint
    coEvery { keycloakService.fetchUserInfo() } returns
      Response.error(400, mockk<RealResponseBody>(relaxed = true))

    // Mock result for retrieving a FHIR resource using user's keycloak uuid
    coEvery { fhirResourceService.getResource(any()) } returns Bundle()

    loginViewModel.login(mockedActivity(isDeviceOnline = true), scope = this)
    advanceUntilIdle()

    Assert.assertFalse(loginViewModel.showProgressBar.value!!)
    Assert.assertEquals(LoginErrorState.UNKNOWN_HOST, loginViewModel.loginErrorState.value!!)
  }

  private fun practitionerDetails(): PractitionerDetails {
    return PractitionerDetails().apply {
      fhirPractitionerDetails =
        FhirPractitionerDetails().apply {
          organizations =
            listOf(
              Organization().apply {
                name = "the.org"
                id = "the.org.id"
              }
            )
        }
    }
  }

  @Test
  fun testSavePractitionerDetails() = runTest {
    coEvery { defaultRepository.create(true, any()) } returns listOf()
    loginViewModel.savePractitionerDetails(
      Bundle().addEntry(Bundle.BundleEntryComponent().apply { resource = practitionerDetails() })
    )
    Assert.assertNotNull(
      sharedPreferencesHelper.read(SharedPreferenceKey.PRACTITIONER_DETAILS.name)
    )
  }

  @Test
  fun testUpdateNavigateShouldUpdateLiveData() = runTest {
    loginViewModel.updateNavigateHome(true)
    Assert.assertNotNull(loginViewModel.navigateToHome.value)
    Assert.assertTrue(loginViewModel.navigateToHome.value!!)
  }

  @Test
  fun testForgotPasswordLoadsContact() = runTest {
    loginViewModel.forgotPassword()
    Assert.assertEquals("tel:0123456789", loginViewModel.launchDialPad.value)
  }

  private fun updateCredentials() {
    loginViewModel.run {
      onUsernameUpdated(thisUsername)
      onPasswordUpdated(thisPassword)
    }
  }
  private fun mockedActivity(isDeviceOnline: Boolean = false): HiltActivityForTest {
    val activity = mockk<HiltActivityForTest>(relaxed = true)
    every { activity.isDeviceOnline() } returns isDeviceOnline
    return activity
  }
}
