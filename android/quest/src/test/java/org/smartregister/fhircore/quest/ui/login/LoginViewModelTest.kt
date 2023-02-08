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

package org.smartregister.fhircore.quest.ui.login

import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.google.gson.Gson
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.smartregister.fhircore.engine.auth.AuthCredentials
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.auth.KeycloakService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.data.remote.model.response.OAuthResponse
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.data.remote.shared.TokenAuthenticator
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.isDeviceOnline
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.AccountManagerShadow
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.model.practitioner.FhirPractitionerDetails
import org.smartregister.model.practitioner.PractitionerDetails
import retrofit2.Response

@ExperimentalCoroutinesApi
@HiltAndroidTest
@Config(shadows = [AccountManagerShadow::class])
internal class LoginViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  @Inject lateinit var secureSharedPreference: SecureSharedPreference
  @Inject lateinit var configService: ConfigService
  @Inject lateinit var gson: Gson
  private lateinit var loginViewModel: LoginViewModel
  private lateinit var fhirResourceDataSource: FhirResourceDataSource
  private val accountAuthenticator: AccountAuthenticator = mockk()
  private val configurationRegistry = Faker.buildTestConfigurationRegistry()
  private val defaultRepository: DefaultRepository = mockk(relaxed = true)
  private val resourceService: FhirResourceService = mockk()
  private val tokenAuthenticator = mockk<TokenAuthenticator>()
  private val keycloakService = mockk<KeycloakService>()
  private val fhirResourceService = mockk<FhirResourceService>()
  private val thisUsername = "demo"
  private val thisPassword = "paswd"
  private val workManager = mockk<WorkManager>()

  @Before
  fun setUp() {
    hiltRule.inject()
    fhirResourceDataSource = spyk(FhirResourceDataSource(resourceService))

    every { workManager.enqueue(any<WorkRequest>()) } returns mockk()

    loginViewModel =
      spyk(
        LoginViewModel(
          configurationRegistry = configurationRegistry,
          accountAuthenticator = accountAuthenticator,
          sharedPreferences = sharedPreferencesHelper,
          defaultRepository = defaultRepository,
          configService = configService,
          keycloakService = keycloakService,
          fhirResourceService = fhirResourceService,
          tokenAuthenticator = tokenAuthenticator,
          secureSharedPreference = secureSharedPreference,
          dispatcherProvider = coroutineTestRule.testDispatcherProvider,
          workManager = workManager
        )
      )
  }

  @After
  override fun tearDown() {
    secureSharedPreference.deleteCredentials()
    super.tearDown()
  }

  @Test
  fun testOnUsernameUpdated() {
    loginViewModel.onUsernameUpdated("demo")
    Assert.assertNull(loginViewModel.loginErrorState.value)
    Assert.assertEquals("demo", loginViewModel.username.value)
  }

  @Test
  fun testOnPasswordUpdated() {
    loginViewModel.onPasswordUpdated("12345")
    Assert.assertNull(loginViewModel.loginErrorState.value)
    Assert.assertEquals("12345", loginViewModel.password.value)
  }

  @Test
  fun testSuccessfulOfflineLogin() {
    val activity = mockedActivity()

    updateCredentials()

    every {
      accountAuthenticator.validateLoginCredentials(thisUsername, thisPassword.toCharArray())
    } returns true

    every {
      tokenAuthenticator.validateSavedLoginCredentials(thisUsername, thisPassword.toCharArray())
    } returns true

    loginViewModel.login(activity)

    Assert.assertNull(loginViewModel.loginErrorState.value)
    Assert.assertFalse(loginViewModel.showProgressBar.value!!)
    Assert.assertTrue(loginViewModel.navigateToHome.value!!)
  }

  @Test
  fun testUnSuccessfulOfflineLogin() {
    val activity = mockedActivity()

    updateCredentials()

    every {
      accountAuthenticator.validateLoginCredentials(thisUsername, thisPassword.toCharArray())
    } returns false

    every {
      tokenAuthenticator.validateSavedLoginCredentials(thisUsername, thisPassword.toCharArray())
    } returns false

    loginViewModel.login(activity)

    Assert.assertNotNull(loginViewModel.loginErrorState.value)
    Assert.assertFalse(loginViewModel.showProgressBar.value!!)
    Assert.assertEquals(LoginErrorState.INVALID_CREDENTIALS, loginViewModel.loginErrorState.value!!)
  }

  @Test
  fun testSuccessfulOnlineLoginWithActiveSession() {
    updateCredentials()
    every { tokenAuthenticator.sessionActive() } returns true
    loginViewModel.login(mockedActivity(isDeviceOnline = true))
    Assert.assertFalse(loginViewModel.showProgressBar.value!!)
    Assert.assertTrue(loginViewModel.navigateToHome.value!!)
  }

  @Test
  fun testUnSuccessfulOnlineLoginUsingDifferentUsername() {
    updateCredentials()
    secureSharedPreference.saveCredentials(AuthCredentials("nativeUser", "n4t1veP5wd"))
    every { tokenAuthenticator.sessionActive() } returns false
    loginViewModel.login(mockedActivity(isDeviceOnline = true))
    Assert.assertFalse(loginViewModel.showProgressBar.value!!)
    Assert.assertEquals(
      LoginErrorState.MULTI_USER_LOGIN_ATTEMPT,
      loginViewModel.loginErrorState.value!!
    )
  }

  @Test
  fun testSuccessfulNewOnlineLoginShouldFetchUserInfoAndPractitioner() {
    updateCredentials()
    secureSharedPreference.saveCredentials(AuthCredentials(thisUsername, thisPassword))
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

    loginViewModel.login(mockedActivity(isDeviceOnline = true))

    Assert.assertFalse(loginViewModel.showProgressBar.value!!)
    Assert.assertTrue(loginViewModel.navigateToHome.value!!)

    // Login was successful savePractitionerDetails was called
    val bundleSlot = slot<Bundle>()
    verify { loginViewModel.savePractitionerDetails(capture(bundleSlot)) }

    Assert.assertNotNull(bundleSlot.captured)
    Assert.assertTrue(bundleSlot.captured.entry.isNotEmpty())
    Assert.assertTrue(bundleSlot.captured.entry[0].resource is PractitionerDetails)
  }

  @Test
  fun testUnSuccessfulOnlineLoginUserInfoNotFetched() {
    updateCredentials()
    secureSharedPreference.saveCredentials(AuthCredentials(thisUsername, thisPassword))
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

    loginViewModel.login(mockedActivity(isDeviceOnline = true))

    Assert.assertFalse(loginViewModel.showProgressBar.value!!)
    Assert.assertEquals(LoginErrorState.ERROR_FETCHING_USER, loginViewModel.loginErrorState.value!!)
  }

  @Test
  fun testUnSuccessfulOnlineLoginWhenAccessTokenNotReceived() {
    updateCredentials()
    secureSharedPreference.saveCredentials(AuthCredentials(thisUsername, thisPassword))
    every { tokenAuthenticator.sessionActive() } returns false
    coEvery {
      tokenAuthenticator.fetchAccessToken(thisUsername, thisPassword.toCharArray())
    } returns Result.failure(UnknownHostException())

    // Mock result for fetch user info via keycloak endpoint
    coEvery { keycloakService.fetchUserInfo() } returns
      Response.error(400, mockk<RealResponseBody>(relaxed = true))

    // Mock result for retrieving a FHIR resource using user's keycloak uuid
    coEvery { fhirResourceService.getResource(any()) } returns Bundle()

    loginViewModel.login(mockedActivity(isDeviceOnline = true))

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
  fun testSavePractitionerDetails() {
    coEvery { defaultRepository.create(true, any()) } returns listOf()
    loginViewModel.savePractitionerDetails(
      Bundle().addEntry(Bundle.BundleEntryComponent().apply { resource = practitionerDetails() })
    )
    Assert.assertNotNull(
      sharedPreferencesHelper.read(SharedPreferenceKey.PRACTITIONER_DETAILS.name)
    )
  }

  @Test
  fun testForgotPasswordLoadsContact() {
    loginViewModel.forgotPassword()
    Assert.assertEquals("tel:0123456789", loginViewModel.launchDialPad.value)
  }

  @Test
  fun testUpdateNavigateShouldUpdateLiveData() {
    loginViewModel.updateNavigateHome(true)
    Assert.assertNotNull(loginViewModel.navigateToHome.value)
    Assert.assertTrue(loginViewModel.navigateToHome.value!!)
  }

  @Test
  fun testIsPinEnableShouldReturnTrue() {
    Assert.assertTrue(loginViewModel.isPinEnabled())
  }

  @Test
  fun testDownloadNowWorkflowConfigs() {
    loginViewModel.downloadNowWorkflowConfigs()
    verify { workManager.enqueue(any<OneTimeWorkRequest>()) }
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
