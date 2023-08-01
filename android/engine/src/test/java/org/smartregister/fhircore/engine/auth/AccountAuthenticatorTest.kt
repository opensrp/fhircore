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
import android.accounts.AccountManager.KEY_AUTHTOKEN
import android.accounts.AccountManager.KEY_INTENT
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
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
import io.mockk.spyk
import io.mockk.verify
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.shared.TokenAuthenticator
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import retrofit2.HttpException

@ExperimentalCoroutinesApi
@HiltAndroidTest
class AccountAuthenticatorTest : RobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) var instantTaskExecutorRule = InstantTaskExecutorRule()

  var accountManager: AccountManager = mockk()

  @Inject lateinit var configService: ConfigService

  @BindValue var secureSharedPreference: SecureSharedPreference = mockk()

  @BindValue var tokenAuthenticator: TokenAuthenticator = mockk()

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
          tokenAuthenticator = tokenAuthenticator,
          secureSharedPreference = secureSharedPreference,
        )
      )
  }

  @Test
  fun testThatAccountIsAddedWithCorrectConfigs() {
    val accountType = configService.provideAuthConfiguration().accountType
    val bundle =
      accountAuthenticator.addAccount(
        response = mockk(relaxed = true),
        accountType = accountType,
        authTokenType = authTokenType,
        requiredFeatures = emptyArray(),
        options = bundleOf()
      )
    Assert.assertNotNull(bundle)
    val parcelable = bundle.getParcelable<Intent>(KEY_INTENT)
    Assert.assertNotNull(parcelable)
    Assert.assertNotNull(parcelable!!.extras)

    Assert.assertTrue(parcelable.extras!!.containsKey(AccountAuthenticator.ACCOUNT_TYPE))
    Assert.assertEquals(accountType, parcelable.getStringExtra(AccountAuthenticator.ACCOUNT_TYPE))
    Assert.assertEquals(
      authTokenType,
      parcelable.getStringExtra(TokenAuthenticator.AUTH_TOKEN_TYPE)
    )
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
  fun testThatAuthTokenLabelIsCapitalized() {
    val capitalizedAuthToken = authTokenType.uppercase(Locale.ROOT)
    Assert.assertEquals(capitalizedAuthToken, accountAuthenticator.getAuthTokenLabel(authTokenType))
  }

  @Test
  fun testHasFeatures() {
    Assert.assertNotNull(accountAuthenticator.hasFeatures(mockk(), mockk(), arrayOf()))
  }

  @Test
  fun loadActiveAccountWhenTokenInactiveShouldInvalidateToken() {
    val accountType = TokenAuthenticator.AUTH_TOKEN_TYPE
    val account = Account("test", accountType)
    every { tokenAuthenticator.findAccount() } returns account
    every { tokenAuthenticator.isTokenActive(any()) } returns false
    every { tokenAuthenticator.getAccountType() } returns accountType
    val token = "mystesttoken"
    every { accountManager.peekAuthToken(account, accountType) } returns token
    every { accountManager.invalidateAuthToken(any(), any()) } just runs
    every { accountManager.getAuthToken(any(), any(), any(), any<Boolean>(), any(), any()) } returns
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

    accountAuthenticator.loadActiveAccount(onValidTokenMissing = {})

    verify { accountManager.peekAuthToken(account, accountType) }
    verify { accountManager.invalidateAuthToken(accountType, token) }
  }

  @Test
  fun testConfirmActiveAccountCallsOnResultCallback() {
    every { tokenAuthenticator.findAccount() } returns Account("testAccountName", "testAccountType")
    every {
      accountManager.confirmCredentials(
        any<Account>(),
        any<Bundle>(),
        any<Activity>(),
        any<AccountManagerCallback<Bundle>>(),
        any<Handler>()
      )
    } answers
      {
        val accountManagerBundleFuture =
          object : AccountManagerFuture<Bundle> {
            override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
            override fun isCancelled(): Boolean = false
            override fun isDone(): Boolean = true
            override fun getResult(): Bundle = bundleOf(KEY_INTENT to Intent())
            override fun getResult(timeout: Long, unit: TimeUnit?): Bundle =
              bundleOf(KEY_INTENT to Intent())
          }

        val callback = arg<AccountManagerCallback<Bundle>>(3)
        callback.run(accountManagerBundleFuture)
        accountManagerBundleFuture
      }

    var onResultCalled = false
    accountAuthenticator.confirmActiveAccount { onResultCalled = true }
    Assert.assertTrue(onResultCalled)
  }

  @Test
  fun testLastLoggedInUsernameShouldReturnSessionUsername() {
    every { secureSharedPreference.retrieveSessionUsername() } returns "abc"
    Assert.assertEquals("abc", accountAuthenticator.retrieveLastLoggedInUsername())
  }

  @Test
  fun testEditPropertiesShouldReturnEmptyBundle() {
    Assert.assertTrue(accountAuthenticator.editProperties(null, null).isEmpty)
  }

  @Test
  fun testAddAccountShouldReturnRelevantBundle() {
    val accountType = "accountType"
    val accountBundle =
      accountAuthenticator.addAccount(mockk(), accountType, authTokenType, arrayOf(), bundleOf())

    Assert.assertNotNull(accountBundle)
    Assert.assertTrue(accountBundle.containsKey(AccountManager.KEY_INTENT))

    val intent = accountBundle.get(AccountManager.KEY_INTENT) as Intent

    Assert.assertEquals(accountType, intent.getStringExtra(AccountAuthenticator.ACCOUNT_TYPE))
    Assert.assertEquals(authTokenType, intent.getStringExtra(TokenAuthenticator.AUTH_TOKEN_TYPE))
  }

  @Test
  fun testConfirmCredentialsShouldReturnEmptyBundle() {
    Assert.assertTrue(accountAuthenticator.confirmCredentials(mockk(), mockk(), bundleOf()).isEmpty)
  }

  @Test
  fun testGetAuthTokenWithoutRefreshToken() {
    every { tokenAuthenticator.isTokenActive(any()) } returns false
    val account = spyk(Account("newAccName", "newAccType"))
    every { accountManager.peekAuthToken(account, authTokenType) } returns ""
    val refreshToken = "refreshToken"
    every { accountManager.getPassword(account) } returns refreshToken

    every { tokenAuthenticator.refreshToken(refreshToken) } returns ""

    val authToken = accountAuthenticator.getAuthToken(mockk(), account, authTokenType, bundleOf())
    val parcelable = authToken.get(AccountManager.KEY_INTENT) as Intent

    Assert.assertNotNull(authToken)
    Assert.assertNotNull(parcelable)
    Assert.assertTrue(parcelable.hasExtra(AccountAuthenticator.ACCOUNT_TYPE))
    Assert.assertTrue(parcelable.hasExtra(TokenAuthenticator.AUTH_TOKEN_TYPE))
  }

  @Test
  fun testGetAuthTokenWithRefreshToken() {
    every { tokenAuthenticator.isTokenActive(any()) } returns false
    val account = spyk(Account("newAccName", "newAccType"))
    every { accountManager.peekAuthToken(account, authTokenType) } returns ""

    val refreshToken = "refreshToken"
    every { accountManager.getPassword(account) } returns refreshToken
    every { tokenAuthenticator.refreshToken(refreshToken) } returns "newAccessToken"

    val authTokenBundle: Bundle =
      accountAuthenticator.getAuthToken(null, account, authTokenType, bundleOf())

    Assert.assertNotNull(authTokenBundle)
    Assert.assertTrue(authTokenBundle.containsKey(KEY_AUTHTOKEN))
    Assert.assertEquals("newAccessToken", authTokenBundle.getString(KEY_AUTHTOKEN))
  }

  @Test
  fun testGetBundleWithoutAuthInfoWhenCaughtHttpException() {
    every { tokenAuthenticator.isTokenActive(any()) } returns false
    val account = spyk(Account("newAccName", "newAccType"))
    every { accountManager.peekAuthToken(account, authTokenType) } returns ""

    val refreshToken = "refreshToken"
    every { accountManager.getPassword(account) } returns refreshToken
    every { tokenAuthenticator.refreshToken(refreshToken) } throws
      HttpException(
        mockk {
          every { code() } returns 0
          every { message() } returns ""
        }
      )

    val authTokenBundle: Bundle =
      accountAuthenticator.getAuthToken(null, account, authTokenType, bundleOf())

    Assert.assertNotNull(authTokenBundle)
    Assert.assertFalse(authTokenBundle.containsKey(KEY_AUTHTOKEN))
  }

  @Test
  fun testGetBundleWithoutAuthInfoWhenCaughtUnknownHostException() {
    every { tokenAuthenticator.isTokenActive(any()) } returns false
    val account = spyk(Account("newAccName", "newAccType"))
    every { accountManager.peekAuthToken(account, authTokenType) } returns ""

    val refreshToken = "refreshToken"
    every { accountManager.getPassword(account) } returns refreshToken
    every { tokenAuthenticator.refreshToken(refreshToken) } throws UnknownHostException()

    val authTokenBundle: Bundle =
      accountAuthenticator.getAuthToken(null, account, authTokenType, bundleOf())

    Assert.assertNotNull(authTokenBundle)
    Assert.assertFalse(authTokenBundle.containsKey(KEY_AUTHTOKEN))
  }

  @Test(expected = RuntimeException::class)
  fun testGetBundleWithoutAuthInfoWhenCaughtUnknownHost() {
    every { tokenAuthenticator.isTokenActive(any()) } returns false
    val account = spyk(Account("newAccName", "newAccType"))
    every { accountManager.peekAuthToken(account, authTokenType) } returns ""

    val refreshToken = "refreshToken"
    every { accountManager.getPassword(account) } returns refreshToken
    every { tokenAuthenticator.refreshToken(refreshToken) } throws RuntimeException()

    accountAuthenticator.getAuthToken(null, account, authTokenType, bundleOf())
  }

  @Test
  fun testGetAuthTokenLabel() {
    val authTokenLabel = "auth_token_label"
    Assert.assertEquals(
      authTokenLabel.uppercase(),
      accountAuthenticator.getAuthTokenLabel(authTokenLabel)
    )
  }

  @Test
  fun testUpdateCredentialsShouldReturnEmptyBundle() {
    Assert.assertTrue(
      accountAuthenticator.updateCredentials(mockk(), mockk(), authTokenType, bundleOf()).isEmpty
    )
  }

  @Test
  fun testHasFeaturesShouldReturnEmptyBundle() {
    Assert.assertNotNull(accountAuthenticator.hasFeatures(mockk(), mockk(), arrayOf()))
  }

  @Test
  fun testThatLogoutCallsTokenAuthenticatorLogout() {
    every { tokenAuthenticator.logout() } returns Result.success(true)
    val onLogout = {}
    accountAuthenticator.logout(onLogout)
    verify { tokenAuthenticator.logout() }
  }

  @Test
  fun testValidateLoginCredentials() {
    every { tokenAuthenticator.validateSavedLoginCredentials(any(), any()) } returns true
    Assert.assertTrue(accountAuthenticator.validateLoginCredentials("doe", "pswd".toCharArray()))
  }

  @Test
  fun testThatInvalidateSessionCallsTokenAuthenticatorInvalidateSession() {
    every { tokenAuthenticator.invalidateSession(any()) } just runs
    val onSessionInvalidated = {}
    accountAuthenticator.invalidateSession(onSessionInvalidated)
    verify { tokenAuthenticator.invalidateSession(onSessionInvalidated) }
  }
}
