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
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.jsonwebtoken.JwtException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verifyOrder
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import okhttp3.internal.http.RealResponseBody
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.auth.OAuthService
import org.smartregister.fhircore.engine.data.remote.model.response.OAuthResponse
import org.smartregister.fhircore.engine.data.remote.shared.TokenAuthenticator
import org.smartregister.fhircore.engine.data.remote.shared.TokenAuthenticator.Companion.AUTH_TOKEN_TYPE
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.toSha1
import retrofit2.Response

@HiltAndroidTest
class TokenAuthenticatorTest : RobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)
  @get:Rule val coroutineRule = CoroutineTestRule()
  @Inject lateinit var secureSharedPreference: SecureSharedPreference
  @Inject lateinit var configService: ConfigService
  private val oAuthService: OAuthService = mockk()
  private lateinit var tokenAuthenticator: TokenAuthenticator
  private val accountManager = mockk<AccountManager>()
  private val context = ApplicationProvider.getApplicationContext<HiltTestApplication>()

  @Before
  fun setUp() {
    hiltRule.inject()
    tokenAuthenticator =
      spyk(
        TokenAuthenticator(
          secureSharedPreference = secureSharedPreference,
          configService = configService,
          oAuthService = oAuthService,
          dispatcherProvider = coroutineRule.testDispatcherProvider,
          accountManager = accountManager,
          context = context
        )
      )
  }

  @After
  fun tearDown() {
    secureSharedPreference.deleteCredentials()
  }

  @Test
  fun testIsTokenActiveWithNullToken() {
    Assert.assertFalse(tokenAuthenticator.isTokenActive(null))
  }

  @Test
  @Throws(JwtException::class)
  fun testIsTokenActiveWithExpiredJwtToken() {
    Assert.assertFalse(tokenAuthenticator.isTokenActive("expired-token"))
  }

  @Test
  fun testGetAccessTokenShouldReturnValidAccessToken() {
    val account = Account("demo", "provider")
    every { tokenAuthenticator.findAccount() } returns account
    every { tokenAuthenticator.isTokenActive(any()) } returns true
    val accessToken = "gibberishaccesstoken"
    every { accountManager.peekAuthToken(account, AUTH_TOKEN_TYPE) } returns accessToken
    Assert.assertEquals(accessToken, tokenAuthenticator.getAccessToken())
  }

  @Test
  fun testGetAccessTokenShouldInvalidateExpiredToken() {
    val account = Account("demo", "provider")
    val accessToken = "gibberishaccesstoken"
    every { tokenAuthenticator.findAccount() } returns account
    every { tokenAuthenticator.isTokenActive(any()) } returns false
    every { accountManager.peekAuthToken(account, AUTH_TOKEN_TYPE) } returns accessToken
    every { accountManager.invalidateAuthToken(account.type, accessToken) } just runs
    every {
      accountManager.getAuthToken(account, AUTH_TOKEN_TYPE, any(), true, any(), any())
    } returns mockk()

    tokenAuthenticator.getAccessToken()

    verifyOrder {
      accountManager.invalidateAuthToken(account.type, accessToken)
      accountManager.getAuthToken(account, AUTH_TOKEN_TYPE, any(), true, any(), any())
    }
  }

  @Test
  fun testFetchTokenShouldRetrieveNewTokenAndCreateAccount() {
    val token = "goodToken"
    val refreshToken = "refreshToken"
    val username = "demo"
    val password = charArrayOf('P', '4', '5', '5', 'W', '4', '0')

    val oAuthResponse =
      OAuthResponse(
        accessToken = token,
        refreshToken = refreshToken,
        tokenType = "",
        expiresIn = 3600,
        scope = "openid"
      )
    coEvery { oAuthService.fetchToken(any()) } returns oAuthResponse

    every { accountManager.accounts } returns arrayOf()

    val accountSlot = slot<Account>()
    val tokenSlot = slot<String>()

    every { accountManager.addAccountExplicitly(capture(accountSlot), any(), null) } returns true
    every { accountManager.setAuthToken(any(), any(), capture(tokenSlot)) } just runs

    runTest {
      tokenAuthenticator.fetchAccessToken(username, password)
      Assert.assertEquals(username, accountSlot.captured.name)
      Assert.assertEquals(token, tokenSlot.captured)
    }

    // Credentials saved
    val credentials = secureSharedPreference.retrieveCredentials()
    Assert.assertNotNull(credentials)
    Assert.assertTrue(username.contentEquals(credentials?.username))
    Assert.assertTrue(password.concatToString().toSha1().contentEquals(credentials?.password))
  }

  @Test
  fun testLogout() {
    val account = Account("demo", "provider")
    val refreshToken = "gibberishaccesstoken"
    every { tokenAuthenticator.findAccount() } returns account
    every { accountManager.getPassword(account) } returns refreshToken

    val refreshTokenSlot = slot<String>()
    coEvery { oAuthService.logout(any(), any(), capture(refreshTokenSlot)) } returns
      Response.success(200, mockk<RealResponseBody>())

    every { accountManager.invalidateAuthToken(account.type, any()) } just runs
    every { accountManager.peekAuthToken(account, AUTH_TOKEN_TYPE) } returns "oldToken"
    val result = tokenAuthenticator.logout()
    Assert.assertTrue(result.isSuccess)
  }

  @Test
  fun testRefreshTokenShouldReturnToken() {
    val accessToken = "soRefreshingNewToken"
    val oAuthResponse =
      OAuthResponse(
        accessToken = accessToken,
        refreshToken = "soRefreshingRefreshToken",
        tokenType = "",
        expiresIn = 3600,
        scope = "openid"
      )
    coEvery { oAuthService.fetchToken(any()) } returns oAuthResponse

    val newAccessToken = tokenAuthenticator.refreshToken("oldRefreshToken")
    Assert.assertNotNull(newAccessToken)
    Assert.assertEquals(accessToken, newAccessToken)
  }

  @Test
  fun testFindAccountShouldReturnAnAccount() {
    secureSharedPreference.saveCredentials(AuthCredentials("demo", "sirikali"))
    val account = Account("demo", "provider")
    every { accountManager.getAccountsByType(any()) } returns arrayOf(account)
    val resultAccount = tokenAuthenticator.findAccount()
    Assert.assertNotNull(resultAccount)
    Assert.assertEquals(account.name, resultAccount?.name)
    Assert.assertEquals(account.type, resultAccount?.type)
  }

  @Test
  fun testSessionActiveWithActiveToken() {
    val account = Account("demo", "provider")
    val token = "anotherToken"
    every { tokenAuthenticator.findAccount() } returns account
    every { accountManager.peekAuthToken(account, AUTH_TOKEN_TYPE) } returns token
    every { tokenAuthenticator.isTokenActive(any()) } returns true

    Assert.assertTrue(tokenAuthenticator.sessionActive())
  }

  @Test
  fun testSessionActiveWithInActiveToken() {
    val account = Account("demo", "provider")
    val token = "anotherToken"
    every { tokenAuthenticator.findAccount() } returns account
    every { accountManager.peekAuthToken(account, AUTH_TOKEN_TYPE) } returns token
    every { tokenAuthenticator.isTokenActive(any()) } returns false

    Assert.assertFalse(tokenAuthenticator.sessionActive())
  }

  @Test
  fun testInvalidateSessionShouldInvalidateToken() {
    val account = Account("demo", "provider")
    every { tokenAuthenticator.findAccount() } returns account
    every { accountManager.invalidateAuthToken(account.type, AUTH_TOKEN_TYPE) } just runs
    every { accountManager.removeAccountExplicitly(account) } returns true

    val onSessionInvalidated = {}
    tokenAuthenticator.invalidateSession(onSessionInvalidated)

    verifyOrder {
      accountManager.invalidateAuthToken(account.type, AUTH_TOKEN_TYPE)
      accountManager.removeAccountExplicitly(account)
      onSessionInvalidated()
    }
  }
}
