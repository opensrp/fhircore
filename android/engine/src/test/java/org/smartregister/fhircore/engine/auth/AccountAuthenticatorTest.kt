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
import android.accounts.AccountManager.KEY_ACCOUNT_NAME
import android.accounts.AccountManager.KEY_ACCOUNT_TYPE
import android.accounts.AccountManager.KEY_AUTHTOKEN
import android.accounts.AccountManager.KEY_INTENT
import android.accounts.AccountManagerFuture
import android.content.Intent
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowIntent
import org.smartregister.fhircore.engine.app.fakes.FakeModel
import org.smartregister.fhircore.engine.auth.AccountAuthenticator.Companion.AUTH_TOKEN_TYPE
import org.smartregister.fhircore.engine.auth.AccountAuthenticator.Companion.IS_NEW_ACCOUNT
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.auth.OAuthService
import org.smartregister.fhircore.engine.data.remote.model.response.OAuthResponse
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.ui.appsetting.AppSettingActivity
import org.smartregister.fhircore.engine.ui.login.LoginActivity
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.toSha1
import retrofit2.Call
import retrofit2.Response

@ExperimentalCoroutinesApi
@HiltAndroidTest
class AccountAuthenticatorTest : RobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) var instantTaskExecutorRule = InstantTaskExecutorRule()

  var accountManager: AccountManager = mockk()

  var oAuthService: OAuthService = mockk()

  @Inject lateinit var configService: ConfigService

  @BindValue var secureSharedPreference: SecureSharedPreference = mockk()

  @BindValue var tokenManagerService: TokenManagerService = mockk()

  @Inject lateinit var sharedPreference: SharedPreferencesHelper

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  private lateinit var accountAuthenticator: AccountAuthenticator

  private val context = ApplicationProvider.getApplicationContext<HiltTestApplication>()

  private val authTokenType = "authTokenType"

  @Before
  fun setUp() {
    hiltRule.inject()
    accountAuthenticator =
      spyk(
        AccountAuthenticator(
          context = context,
          accountManager = accountManager,
          oAuthService = oAuthService,
          configService = configService,
          secureSharedPreference = secureSharedPreference,
          tokenManagerService = tokenManagerService,
          sharedPreference = sharedPreference,
          dispatcherProvider = dispatcherProvider
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
          configService = configService,
          secureSharedPreference = secureSharedPreference,
          tokenManagerService = tokenManagerService,
          sharedPreference = sharedPreference,
          dispatcherProvider = dispatcherProvider
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
          configService = configService,
          secureSharedPreference = secureSharedPreference,
          tokenManagerService = tokenManagerService,
          sharedPreference = sharedPreference,
          dispatcherProvider = dispatcherProvider
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
        .fetchToken(
          FakeModel.authCredentials.username,
          FakeModel.authCredentials.password.toCharArray()
        )
        .execute()
    Assert.assertEquals("testToken", token.body()!!.accessToken)
  }

  @Test
  @Ignore("Fix assertion")
  fun testRefreshToken() {
    val callMock = mockk<Call<OAuthResponse>>()
    val mockk = mockk<OAuthResponse>()
    val mockResponse = spyk(Response.success<OAuthResponse?>(mockk))
    every { callMock.execute() } returns mockResponse

    every { accountAuthenticator.oAuthService.fetchToken(any()) } returns callMock
    val token = accountAuthenticator.refreshToken(FakeModel.authCredentials.refreshToken!!)
    Assert.assertNotNull(token)
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
    accountAuthenticator.launchLoginScreen()
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
    every { oAuthService.logout(any(), any(), any()) } returns mockk()

    accountAuthenticator.logout()

    val startedIntent: Intent =
      shadowOf(ApplicationProvider.getApplicationContext<HiltTestApplication>()).nextStartedActivity
    val shadowIntent: ShadowIntent = shadowOf(startedIntent)

    // User will be prompted with AppSettings screen to provide appId to redownload config
    Assert.assertEquals(AppSettingActivity::class.java, shadowIntent.intentClass)

    verify { oAuthService.logout(any(), any(), any()) }
  }

  @Test
  fun loadActiveAccountWhenTokenInactiveShouldInvalidateToken() {
    val accountType = "testAccountType"
    val account = Account("test", accountType)
    every { tokenManagerService.getActiveAccount() } returns account
    every { tokenManagerService.isTokenActive(any()) } returns false
    every { accountAuthenticator.getAccountType() } returns accountType
    val token = "mystesttoken"
    every { accountManager.peekAuthToken(any(), any()) } returns token
    every { accountManager.invalidateAuthToken(any(), any()) } just runs
    every {
      accountManager.getAuthToken(
        any<Account>(),
        any<String>(),
        any<Bundle>(),
        any<Boolean>(),
        any(),
        any()
      )
    } returns
      object : AccountManagerFuture<Bundle> {
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
          TODO("Not yet implemented")
        }

        override fun isCancelled(): Boolean {
          TODO("Not yet implemented")
        }

        override fun isDone(): Boolean {
          TODO("Not yet implemented")
        }

        override fun getResult(): Bundle {
          TODO("Not yet implemented")
        }

        override fun getResult(timeout: Long, unit: TimeUnit?): Bundle {
          TODO("Not yet implemented")
        }
      }

    accountAuthenticator.loadActiveAccount(onActiveAuthTokenFound = {}, onValidTokenMissing = {})

    verify { accountManager.peekAuthToken(account, accountType) }
    verify { accountManager.invalidateAuthToken(accountType, token) }
  }

  @Test
  fun testLastLoggedInUsernameShouldNotNull() {
    every { secureSharedPreference.retrieveSessionUsername() } returns "abc"
    Assert.assertEquals("abc", accountAuthenticator.retrieveLastLoggedInUsername())
  }
}
