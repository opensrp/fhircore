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
import android.app.Application
import android.content.Intent
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.data.remote.model.response.OAuthResponse
import org.smartregister.fhircore.engine.robolectric.FhircoreTestRunner
import org.smartregister.fhircore.engine.ui.login.BaseLoginActivity

@RunWith(FhircoreTestRunner::class)
@Config(sdk = [29])
class AccountAuthenticatorTest {
  private lateinit var authService: AuthenticationService
  private lateinit var accountAuthenticator: AccountAuthenticator
  private val context = ApplicationProvider.getApplicationContext<Application>()

  @Before
  fun setUp() {
    authService = mockk()
    accountAuthenticator = AccountAuthenticator(context, authService)
    ReflectionHelpers.setField(accountAuthenticator, "accountManager", mockk<AccountManager>())
  }

  @Test
  fun testAddAccountShouldReturnIntentWithCorrectExtras() {
    every { authService.getAccountType() } returns "test-account-type"
    every { authService.getLoginActivityClass() } returns BaseLoginActivity::class.java

    val result =
      accountAuthenticator.addAccount(
        mockk(),
        "test-account-type",
        "test-token-type",
        null,
        bundleOf()
      )

    val intent = result.getParcelable<Intent>(AccountManager.KEY_INTENT)
    val extras = intent!!.extras!!

    assertEquals("test-account-type", extras.getString(AccountManager.KEY_ACCOUNT_TYPE))
    assertEquals("test-token-type", extras.getString(AuthenticationService.AUTH_TOKEN_TYPE))
  }

  @Test
  fun testGetAuthTokenShouldReturnBundleWithCorrectAccountData() {
    every { authService.getLocalSessionToken() } returns "test-access-token"

    val result =
      accountAuthenticator.getAuthToken(
        mockk(),
        Account("testuser", "test-account-type"),
        "test-token-type",
        bundleOf()
      )

    assertEquals("testuser", result.getString(AccountManager.KEY_ACCOUNT_NAME))
    assertEquals("test-account-type", result.getString(AccountManager.KEY_ACCOUNT_TYPE))
    assertEquals("test-access-token", result.getString(AccountManager.KEY_AUTHTOKEN))
  }

  @Test
  fun testGetAuthTokenShouldNotCallRefreshIfAccessTokenExists() {
    every { authService.getLocalSessionToken() } returns "test-access-token"

    val result =
      accountAuthenticator.getAuthToken(
        mockk(),
        Account("testuser", "test-account-type"),
        "test-token-type",
        bundleOf()
      )

    assertEquals("test-access-token", result.getString(AccountManager.KEY_AUTHTOKEN))

    verify(inverse = true) { authService.getRefreshToken() }
    verify(inverse = true) { authService.fetchToken(any(), any()) }
  }

  @Test
  fun testGetAuthTokenShouldCallRefreshIfAccessTokenNotExists() {
    val oauth = OAuthResponse()
    oauth.accessToken = "valid access token"
    oauth.refreshToken = "valid refresh token"
    oauth.expiresIn = 1444444444
    oauth.refreshExpiresIn = 1444444444

    every { authService.getLocalSessionToken() } returns null
    every { authService.getRefreshToken() } returns "some refresh token"
    every { authService.refreshToken("some refresh token") } returns oauth

    val result =
      accountAuthenticator.getAuthToken(
        mockk(),
        Account("testuser", "test-account-type"),
        "test-token-type",
        bundleOf()
      )

    assertEquals("testuser", result.getString(AccountManager.KEY_ACCOUNT_NAME))
    assertEquals("test-account-type", result.getString(AccountManager.KEY_ACCOUNT_TYPE))
    assertEquals(oauth.accessToken, result.getString(AccountManager.KEY_AUTHTOKEN))

    verify { authService.getRefreshToken() }
    verify { authService.refreshToken("some refresh token") }
    verify { authService.updateSession(oauth) }
  }
}
