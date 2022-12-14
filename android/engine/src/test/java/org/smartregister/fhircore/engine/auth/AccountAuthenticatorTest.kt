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

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManager.ERROR_CODE_NETWORK_ERROR
import android.accounts.AccountManager.KEY_ACCOUNT_NAME
import android.accounts.AccountManager.KEY_ACCOUNT_TYPE
import android.accounts.AccountManager.KEY_AUTHTOKEN
import android.accounts.AccountManager.KEY_ERROR_CODE
import android.accounts.AccountManager.KEY_ERROR_MESSAGE
import android.accounts.AccountManager.KEY_INTENT
import android.accounts.AccountManagerCallback
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import java.net.UnknownHostException
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import okhttp3.ResponseBody
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowIntent
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.auth.AccountAuthenticator.Companion.AUTH_TOKEN_TYPE
import org.smartregister.fhircore.engine.auth.AccountAuthenticator.Companion.IS_NEW_ACCOUNT
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.auth.OAuthService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.data.remote.model.response.OAuthResponse
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.ui.login.LoginActivity
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.toSha1
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@ExperimentalCoroutinesApi
@HiltAndroidTest
class AccountAuthenticatorTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) var instantTaskExecutorRule = InstantTaskExecutorRule()

  @Inject lateinit var configService: ConfigService

  @BindValue var secureSharedPreference: SecureSharedPreference = mockk()

  @BindValue var tokenManagerService: TokenManagerService = mockk()

  @Inject lateinit var sharedPreference: SharedPreferencesHelper

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  private lateinit var accountAuthenticator: AccountAuthenticator

  private val context = ApplicationProvider.getApplicationContext<HiltTestApplication>()

  private val authTokenType = "authTokenType"

  private val accountManager: AccountManager = mockk()

  private val oAuthService: OAuthService = mockk()

  private val fhirResourceService: FhirResourceService = mockk()

  @Before
  fun setUp() {
    hiltRule.inject()
    accountAuthenticator =
      spyk(
        AccountAuthenticator(
          context = context,
          accountManager = accountManager,
          oAuthService = oAuthService,
          fhirResourceService = fhirResourceService,
          parser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser(),
          configService = configService,
          secureSharedPreference = secureSharedPreference,
          tokenManagerService = tokenManagerService,
          sharedPreference = sharedPreference,
          dispatcherProvider = dispatcherProvider,
        )
      )
  }

  @Test
  fun testThatAccountIsAddedWithCorrectConfigs() {

    val bundle =
      accountAuthenticator.addAccount(
        response = mockk(relaxed = true),
        accountType = configService.provideAuthConfiguration().accountType,
        authTokenType = authTokenType,
        requiredFeatures = emptyArray(),
        options = bundleOf()
      )
    Assert.assertNotNull(bundle)
    val parcelable = bundle.getParcelable<Intent>(KEY_INTENT)
    Assert.assertNotNull(parcelable)
    Assert.assertNotNull(parcelable!!.extras)
    Assert.assertEquals(
      configService.provideAuthConfiguration().accountType,
      parcelable.getStringExtra(KEY_ACCOUNT_TYPE)
    )

    Assert.assertTrue(parcelable.extras!!.containsKey(AUTH_TOKEN_TYPE))
    Assert.assertEquals(authTokenType, parcelable.getStringExtra(AUTH_TOKEN_TYPE))
    Assert.assertTrue(parcelable.extras!!.containsKey(IS_NEW_ACCOUNT))
    Assert.assertTrue(parcelable.extras!!.getBoolean(IS_NEW_ACCOUNT))
  }

  @Test
  fun testThatEditPropertiesIsNotNull() {
    Assert.assertNotNull(
      accountAuthenticator.editProperties(
        response = null,
        accountType = configService.provideAuthConfiguration().accountType
      )
    )
  }

  @Test
  fun testThatConfirmCredentialsIsNotNull() {
    Assert.assertNotNull(
      accountAuthenticator.confirmCredentials(
        response = mockk(relaxed = true),
        account = mockk(relaxed = true),
        options = bundleOf()
      )
    )
  }

  @Test
  fun testThatAuthTokenLabelIsCapitalized() {
    val capitalizedAuthToken = authTokenType.uppercase(Locale.ROOT)
    Assert.assertEquals(capitalizedAuthToken, accountAuthenticator.getAuthTokenLabel(authTokenType))
  }

  @Test
  fun testThatCredentialsAreUpdated() {

    val account = spyk(Account("newAccName", "newAccType"))

    val bundle =
      accountAuthenticator.updateCredentials(
        response = mockk(relaxed = true),
        account = account,
        authTokenType = authTokenType,
        options = bundleOf()
      )
    Assert.assertNotNull(bundle)
    val parcelable = bundle.getParcelable<Intent>(KEY_INTENT)
    Assert.assertNotNull(parcelable)
    Assert.assertNotNull(parcelable!!.extras)
    Assert.assertEquals(account.type, parcelable.getStringExtra(KEY_ACCOUNT_TYPE))
    Assert.assertEquals(account.name, parcelable.getStringExtra(KEY_ACCOUNT_NAME))
    Assert.assertTrue(parcelable.extras!!.containsKey(AUTH_TOKEN_TYPE))
    Assert.assertEquals(authTokenType, parcelable.getStringExtra(AUTH_TOKEN_TYPE))
  }

  @Test
  fun testGetAuthToken() {
    every { tokenManagerService.getLocalSessionToken() } returns null
    every { tokenManagerService.isTokenActive(any()) } returns false
    every { secureSharedPreference.retrieveCredentials() } returns AuthCredentials("abc", "123")
    every { accountManager.notifyAccountAuthenticated(any()) } returns false

    val account = spyk(Account("newAccName", "newAccType"))
    val authToken = accountAuthenticator.getAuthToken(mockk(), account, authTokenType, bundleOf())
    val parcelable = authToken.getParcelable<Intent>(KEY_INTENT)
    Assert.assertNotNull(authToken)
    Assert.assertNotNull(parcelable)
    Assert.assertTrue(parcelable!!.hasExtra(KEY_ACCOUNT_NAME))
    Assert.assertTrue(parcelable.hasExtra(KEY_ACCOUNT_TYPE))
    Assert.assertEquals(account.name, parcelable.getStringExtra(KEY_ACCOUNT_NAME))
    Assert.assertEquals(account.type, parcelable.getStringExtra(KEY_ACCOUNT_TYPE))
  }

  @Test
  fun testGetAuthTokenWhenAccessTokenIsNullShouldReturnValidAccount() {
    val emptySessionToken = null
    every { tokenManagerService.getLocalSessionToken() } returns emptySessionToken

    val accountManager = mockk<AccountManager>()
    val isAuthAcknowledged = true
    every { accountManager.notifyAccountAuthenticated(any()) } returns isAuthAcknowledged

    val accountAuthenticator =
      spyk(
        AccountAuthenticator(
          context = context,
          accountManager = accountManager,
          oAuthService = spyk(oAuthService),
          fhirResourceService = mockk(),
          parser = mockk(),
          configService = configService,
          secureSharedPreference = secureSharedPreference,
          tokenManagerService = tokenManagerService,
          sharedPreference = sharedPreference,
          dispatcherProvider = dispatcherProvider,
        )
      )

    val refreshToken = "refreshToken"
    every { accountAuthenticator.getRefreshToken() } returns refreshToken

    val newAccessToken = "newAccessToken"
    val refreshExpiresIn = 2
    val expiresIn = 1
    val scope = "scope"

    val oAuthResponse =
      OAuthResponse(
        accessToken = newAccessToken,
        tokenType = authTokenType,
        refreshToken = refreshToken,
        refreshExpiresIn = refreshExpiresIn,
        expiresIn = expiresIn,
        scope = scope
      )
    every { accountAuthenticator.refreshToken(any()) } returns oAuthResponse

    val account = Account("newAccName", "newAccType")
    val authToken = accountAuthenticator.getAuthToken(mockk(), account, authTokenType, bundleOf())

    val actualAccountName = authToken[KEY_ACCOUNT_NAME]
    val actualAccountType = authToken[KEY_ACCOUNT_TYPE]
    val actualAccountAuthToken = authToken[KEY_AUTHTOKEN]

    Assert.assertEquals(account.name, actualAccountName)
    Assert.assertEquals(account.type, actualAccountType)
    Assert.assertEquals(newAccessToken, actualAccountAuthToken)
  }

  @Test
  fun testGetAuthTokenWhenAccessTokenIsBlankAndNewTokenResponseIsNullShouldReturnValidAccountFromAuthActivity() {
    val emptySessionToken = ""
    every { tokenManagerService.getLocalSessionToken() } returns emptySessionToken

    val accountManager = mockk<AccountManager>()
    val isAuthAcknowledged = true
    every { accountManager.notifyAccountAuthenticated(any()) } returns isAuthAcknowledged

    val accountAuthenticator =
      spyk(
        AccountAuthenticator(
          context = context,
          accountManager = accountManager,
          oAuthService = spyk(oAuthService),
          fhirResourceService = mockk(),
          parser = mockk(),
          configService = configService,
          secureSharedPreference = secureSharedPreference,
          tokenManagerService = tokenManagerService,
          sharedPreference = sharedPreference,
          dispatcherProvider = dispatcherProvider,
        )
      )

    val refreshToken = "refreshToken"
    every { accountAuthenticator.getRefreshToken() } returns refreshToken

    val oAuthResponse = null
    every { accountAuthenticator.refreshToken(any()) } returns oAuthResponse

    val account = Account("newAccName", "newAccType")
    val authToken = accountAuthenticator.getAuthToken(mockk(), account, authTokenType, bundleOf())
    val parcelable = authToken.getParcelable<Intent>(KEY_INTENT)

    val actualAccountName = parcelable?.getStringExtra(KEY_ACCOUNT_NAME)
    val actualAccountType = parcelable?.getStringExtra(KEY_ACCOUNT_TYPE)

    Assert.assertNotNull(authToken)
    Assert.assertNotNull(parcelable)
    Assert.assertTrue(parcelable!!.hasExtra(KEY_ACCOUNT_NAME))
    Assert.assertTrue(parcelable.hasExtra(KEY_ACCOUNT_TYPE))
    Assert.assertEquals(account.name, actualAccountName)
    Assert.assertEquals(account.type, actualAccountType)
  }

  @Test
  fun testHasFeatures() {
    Assert.assertNotNull(accountAuthenticator.hasFeatures(mockk(), mockk(), arrayOf()))
  }

  @Test
  fun testGetUserInfo() {
    every { oAuthService.userInfo() } returns mockk()
    Assert.assertNotNull(accountAuthenticator.getUserInfo())
  }

  @Test
  fun testFetchToken() {
    val callMock = mockk<Call<OAuthResponse>>()
    val mockResponse = Response.success<OAuthResponse?>(OAuthResponse("testToken"))
    every { callMock.execute() } returns mockResponse
    every { oAuthService.fetchToken(any()) } returns callMock
    val token =
      accountAuthenticator
        .fetchToken(Faker.authCredentials.username, Faker.authCredentials.password.toCharArray())
        .execute()
    Assert.assertEquals("testToken", token.body()!!.accessToken)
  }

  @Test
  fun testRefreshTokenShouldFetchToken() {
    val callMock = mockk<Call<OAuthResponse>>()
    val mockk = mockk<OAuthResponse>()
    val mockResponse = spyk(Response.success<OAuthResponse?>(mockk))
    every { callMock.execute() } returns mockResponse

    every { oAuthService.fetchToken(any()) } returns callMock
    val token = accountAuthenticator.refreshToken(Faker.authCredentials.refreshToken!!)
    Assert.assertNotNull(token)
  }

  @Test
  fun testGetPractitionerDetailsFromAssets() {
    val details = accountAuthenticator.getPractitionerDetailsFromAssets()
    Assert.assertNotNull(details)
  }

  @Test
  fun testGetPractitionerDetailsShouldReturnBundle() {
    val uuidSlot = slot<String>()
    coEvery { fhirResourceService.getResource(capture(uuidSlot)) } returns mockk()

    runBlocking {
      accountAuthenticator.getPractitionerDetails("12345")
      Assert.assertEquals("practitioner-details?keycloak-uuid=12345", uuidSlot.captured)
    }
  }

  @Test
  fun testHasActivePinShouldReturnTrue() {
    every { secureSharedPreference.retrieveSessionPin() } returns "12345"
    Assert.assertTrue(accountAuthenticator.hasActivePin())
  }

  @Test
  fun testLoadActiveAccountShouldVerifyActiveAccount() {
    val errorHandler = mockk<Handler>()
    val callback = mockk<AccountManagerCallback<Bundle>>()

    val account = mockk<Account>()
    every { tokenManagerService.getActiveAccount() } returns account
    every { accountManager.getAuthToken(any(), any(), any(), any<Boolean>(), any(), any()) } returns
      mockk()

    accountAuthenticator.loadActiveAccount(callback, errorHandler)

    verify(exactly = 1) {
      accountManager.getAuthToken(account, AUTH_TOKEN_TYPE, any(), false, callback, errorHandler)
    }
  }

  @Test
  fun testLogoutShouldVerifyAlreadyLoggedOutUser() {

    every { secureSharedPreference.retrieveCredentials() } returns null
    every { tokenManagerService.isTokenActive(any()) } returns false

    val onLogout = mockk<() -> Unit>()
    every { onLogout.invoke() } returns Unit

    accountAuthenticator.logout(onLogout)

    verify(exactly = 1) { onLogout.invoke() }
  }

  @Test
  fun testLogoutShouldSuccessfullyLogout() {
    every { tokenManagerService.isTokenActive(any()) } returns true
    every { secureSharedPreference.retrieveCredentials() } returns
      AuthCredentials("abc", "111", "mystoken", "myrtoken")

    val callResponse = mockk<Call<ResponseBody>>(relaxed = true)
    val callbackSlot = slot<Callback<ResponseBody>>()
    every { callResponse.enqueue(capture(callbackSlot)) } returns Unit
    every { oAuthService.logout(any(), any(), any()) } returns callResponse

    val onLogout = mockk<() -> Unit>()
    every { onLogout.invoke() } returns Unit

    val accountAuthenticatorSpy = spyk(accountAuthenticator)
    every { accountAuthenticatorSpy.localLogout() } returns Unit
    accountAuthenticatorSpy.logout(onLogout)

    callbackSlot.captured.onResponse(mockk(), mockk { every { isSuccessful } returns true })

    verify(exactly = 1) { onLogout.invoke() }
    verify(exactly = 1) { accountAuthenticatorSpy.localLogout() }
    verify(exactly = 1) { accountAuthenticatorSpy.launchScreen(any<Class<LoginActivity>>()) }
  }

  @Test
  fun testLogoutShouldNotSuccessfullyLogout() {
    every { tokenManagerService.isTokenActive(any()) } returns true
    every { secureSharedPreference.retrieveCredentials() } returns
      AuthCredentials("abc", "111", "mystoken", "myrtoken")

    val callResponse = mockk<Call<ResponseBody>>(relaxed = true)
    val callbackSlot = slot<Callback<ResponseBody>>()
    every { callResponse.enqueue(capture(callbackSlot)) } returns Unit
    every { oAuthService.logout(any(), any(), any()) } returns callResponse

    val onLogout = mockk<() -> Unit>()
    every { onLogout.invoke() } returns Unit

    val accountAuthenticatorSpy = spyk(accountAuthenticator)
    every { accountAuthenticatorSpy.localLogout() } returns Unit
    accountAuthenticatorSpy.logout(onLogout)

    callbackSlot.captured.onResponse(
      mockk(),
      mockk {
        every { isSuccessful } returns false
        every { body() } returns null
      }
    )

    verify(exactly = 1) { onLogout.invoke() }
    verify(exactly = 0) { accountAuthenticatorSpy.localLogout() }
    verify(exactly = 0) { accountAuthenticatorSpy.launchScreen(any<Class<LoginActivity>>()) }
  }

  @Test
  fun testLogoutShouldFailureLogout() {
    every { tokenManagerService.isTokenActive(any()) } returns true
    every { secureSharedPreference.retrieveCredentials() } returns
      AuthCredentials("abc", "111", "mystoken", "myrtoken")

    val callResponse = mockk<Call<ResponseBody>>(relaxed = true)
    val callbackSlot = slot<Callback<ResponseBody>>()
    every { callResponse.enqueue(capture(callbackSlot)) } returns Unit
    every { oAuthService.logout(any(), any(), any()) } returns callResponse

    val onLogout = mockk<() -> Unit>()
    every { onLogout.invoke() } returns Unit

    val accountAuthenticatorSpy = spyk(accountAuthenticator)
    every { accountAuthenticatorSpy.localLogout() } returns Unit
    accountAuthenticatorSpy.logout(onLogout)

    callbackSlot.captured.onFailure(mockk(), RuntimeException())

    verify(exactly = 1) { onLogout.invoke() }
    verify(exactly = 0) { accountAuthenticatorSpy.localLogout() }
    verify(exactly = 0) { accountAuthenticatorSpy.launchScreen(any<Class<LoginActivity>>()) }
  }

  @Test
  fun testGetRefreshToken() {
    every { tokenManagerService.isTokenActive(any()) } returns false

    every { secureSharedPreference.retrieveCredentials() } returns null
    Assert.assertNull(accountAuthenticator.getRefreshToken())

    every { secureSharedPreference.retrieveCredentials() } returns
      AuthCredentials("abc", "123", null, null)
    Assert.assertNull(accountAuthenticator.getRefreshToken())
  }

  @Test
  fun testHasActiveSession() {
    every { tokenManagerService.getLocalSessionToken() } returns ""
    Assert.assertFalse(accountAuthenticator.hasActiveSession())
  }

  @Test
  fun testValidLocalCredentials() {
    every { accountManager.accounts } returns
      arrayOf(Account("demo", configService.provideAuthConfiguration().accountType))
    every { secureSharedPreference.retrieveCredentials() } returns
      AuthCredentials("demo", "51r1K4l1".toSha1())

    Assert.assertTrue(accountAuthenticator.validLocalCredentials("demo", "51r1K4l1".toCharArray()))
    Assert.assertFalse(
      accountAuthenticator.validLocalCredentials("WrongUsername", "51r1K4l1".toCharArray())
    )
    Assert.assertFalse(
      accountAuthenticator.validLocalCredentials("demo", "WrongPassword".toCharArray())
    )
  }

  @Test
  fun testUpdateSession() {
    every { secureSharedPreference.retrieveCredentials() } returns
      AuthCredentials("abc", "123", null, null)
    every { secureSharedPreference.saveCredentials(any()) } just runs

    val successResponse: OAuthResponse = mockk()
    every { successResponse.accessToken } returns "newAccessToken"
    every { successResponse.refreshToken } returns "newRefreshToken"

    accountAuthenticator.updateSession(successResponse)

    val slot = slot<AuthCredentials>()

    verify { secureSharedPreference.retrieveCredentials() }
    verify { secureSharedPreference.saveCredentials(capture(slot)) }

    val retrieveCredentials = slot.captured
    Assert.assertNotNull(retrieveCredentials)
    Assert.assertEquals(successResponse.accessToken, retrieveCredentials.sessionToken)
    Assert.assertEquals(successResponse.refreshToken, retrieveCredentials.refreshToken)
  }

  @Test
  fun testLaunchLoginScreenShouldStartLoginActivity() {
    accountAuthenticator.launchScreen(LoginActivity::class.java)
    val startedIntent: Intent =
      shadowOf(ApplicationProvider.getApplicationContext<HiltTestApplication>()).nextStartedActivity
    val shadowIntent: ShadowIntent = shadowOf(startedIntent)
    Assert.assertEquals(LoginActivity::class.java, shadowIntent.intentClass)
  }

  @Test
  fun testLogoutShouldCleanSessionAndStartLoginActivity() = runBlockingTest {
    every { tokenManagerService.isTokenActive(any()) } returns true
    every { secureSharedPreference.retrieveCredentials() } returns
      AuthCredentials("abc", "111", "mystoken", "myrtoken")
    val callResponse = mockk<Call<ResponseBody>>(relaxed = true)
    every { oAuthService.logout(any(), any(), any()) } returns callResponse

    accountAuthenticator.logout()

    verify { oAuthService.logout(any(), any(), any()) }
  }

  @Test
  fun testLocalLogoutInvalidatesAuthenticationToken() = runBlockingTest {
    every { secureSharedPreference.deleteSessionTokens() } returns Unit
    every { accountManager.invalidateAuthToken(any(), any()) } returns Unit
    every { tokenManagerService.getLocalSessionToken() } returns "my-token"

    accountAuthenticator.localLogout()

    verify { tokenManagerService.getLocalSessionToken() }
    verify { accountManager.invalidateAuthToken(any(), any()) }
  }

  @Test
  fun testLocalLogoutDeletesSessionTokens() = runBlockingTest {
    every { secureSharedPreference.deleteSessionTokens() } returns Unit
    every { accountManager.invalidateAuthToken(any(), any()) } returns Unit
    every { tokenManagerService.getLocalSessionToken() } returns "my-token"

    accountAuthenticator.localLogout()

    verify { tokenManagerService.getLocalSessionToken() }
    verify { secureSharedPreference.deleteSessionTokens() }
  }

  @Test
  fun `refresh expired auth token returns Bundle with new token`() {
    every { tokenManagerService.getActiveAccount() } returns mockk()
    every { tokenManagerService.isTokenActive(any()) } returns false andThen true
    every { accountManager.getAuthToken(any(), any(), any(), any<Boolean>(), any(), any()) } returns
      mockk()
    every { accountManager.peekAuthToken(any(), any()) } returns "auth-token"
    every { accountManager.notifyAccountAuthenticated(any()) } returns true
    every { accountAuthenticator.getRefreshToken() } returns "refresh-token"
    every { accountAuthenticator.refreshToken(any()) } returns
      OAuthResponse(accessToken = "new-access-token", refreshToken = "new-refresh-token")
    every { secureSharedPreference.retrieveCredentials() } returns
      AuthCredentials(username = "test", password = "test123")

    every { secureSharedPreference.saveCredentials(any()) } just Runs
    every { accountManager.setAuthToken(any(), any(), any()) } just Runs

    val bundle = runBlocking { accountAuthenticator.refreshSessionAuthToken() }

    verify { accountAuthenticator.refreshToken(any()) }
    verify { accountAuthenticator.updateSession(any()) }
    verify { accountManager.setAuthToken(any(), any(), any()) }
    verify { accountManager.notifyAccountAuthenticated(any()) }

    Assert.assertNotNull(bundle)
    Assert.assertEquals("new-access-token", bundle.getString((KEY_AUTHTOKEN)))
  }

  @Test
  fun `refresh auth token returns Bundle without token if not active`() {
    every { tokenManagerService.getActiveAccount() } returns mockk()
    every { tokenManagerService.isTokenActive(any()) } returns false
    every { accountManager.getAuthToken(any(), any(), any(), any<Boolean>(), any(), any()) } returns
      mockk()
    every { accountManager.peekAuthToken(any(), any()) } returns "auth-token"
    every { accountManager.notifyAccountAuthenticated(any()) } returns true
    every { accountAuthenticator.getRefreshToken() } returns "refresh-token"
    every { accountAuthenticator.refreshToken(any()) } returns
      OAuthResponse(accessToken = "new-access-token", refreshToken = "new-refresh-token")
    every { secureSharedPreference.retrieveCredentials() } returns
      AuthCredentials(username = "test", password = "test123")

    every { secureSharedPreference.saveCredentials(any()) } just Runs
    every { accountManager.setAuthToken(any(), any(), any()) } just Runs

    val bundle = runBlocking { accountAuthenticator.refreshSessionAuthToken() }

    Assert.assertNotNull(bundle)
    Assert.assertFalse(bundle.containsKey(KEY_AUTHTOKEN))
  }

  @Test
  fun `refresh auth token returns bundle with network error if no connectivity`() {
    every { tokenManagerService.getActiveAccount() } returns mockk()
    every { tokenManagerService.isTokenActive(any()) } returns false
    every { accountManager.getAuthToken(any(), any(), any(), any<Boolean>(), any(), any()) } returns
      mockk()
    every { accountManager.peekAuthToken(any(), any()) } returns "auth-token"
    every { accountAuthenticator.getRefreshToken() } returns "refresh-token"
    every { accountAuthenticator.refreshToken(any()) } throws UnknownHostException("localhost")

    val bundle = runBlocking { accountAuthenticator.refreshSessionAuthToken() }

    Assert.assertNotNull(bundle)
    Assert.assertFalse(bundle.containsKey(KEY_AUTHTOKEN))
    Assert.assertTrue(bundle.containsKey(KEY_ERROR_MESSAGE))
    Assert.assertEquals(bundle.getInt(KEY_ERROR_CODE), ERROR_CODE_NETWORK_ERROR)
  }
}
