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
import android.os.Handler
import androidx.test.core.app.ApplicationProvider
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.data.remote.auth.OAuthService
import org.smartregister.fhircore.engine.data.remote.model.response.OAuthResponse
import org.smartregister.fhircore.engine.robolectric.FhircoreTestRunner
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.toSha1
import retrofit2.Call
import retrofit2.Response

@RunWith(FhircoreTestRunner::class)
class AuthenticationServiceTest : RobolectricTest() {
  private var mockOauthService: OAuthService? = null
  private lateinit var authenticationService: AuthenticationService
  private lateinit var accountManager: AccountManager
  private lateinit var secureSharedPreference: SecureSharedPreference

  lateinit var captor: CapturingSlot<Map<String, String>>

  @Before
  fun setUp() {
    accountManager = mockk()
    secureSharedPreference = mockk()

    mockOauthService = mockk()

    mockkObject(OAuthService.Companion)

    every { OAuthService.create(any(), any()) } returns mockOauthService!!

    authenticationService =
      spyk(AuthenticationServiceShadow(ApplicationProvider.getApplicationContext()))

    ReflectionHelpers.setField(
      authenticationService,
      "secureSharedPreference\$delegate",
      lazy { secureSharedPreference }
    )
    ReflectionHelpers.setField(
      authenticationService,
      "accountManager\$delegate",
      lazy { accountManager }
    )

    captor = slot()
  }

  @Test
  fun testFetchTokenShouldSendCorrectData() {
    every { mockOauthService?.fetchToken(capture(captor)) } returns mockk()
    every { authenticationService.clientId() } returns "test-client-id"
    every { authenticationService.clientSecret() } returns "test-client-id"

    authenticationService.fetchToken("testuser", "testpass".toCharArray())

    assertTrue(captor.captured.get("client_id").equals("test-client-id"))
    assertTrue(captor.captured.get("client_secret").equals("test-client-id"))
    assertTrue(captor.captured.get("grant_type").equals("password"))
    assertTrue(captor.captured.get("username").equals("testuser"))
    assertTrue(captor.captured.get("password").equals("testpass"))
  }

  @Test
  fun testRefreshTokenShouldSendCorrectData() {
    val mockCall = mockk<Call<OAuthResponse>>()

    every { mockOauthService?.fetchToken(capture(captor)) } returns mockCall
    every { authenticationService.clientId() } returns "test-client-id"
    every { authenticationService.clientSecret() } returns "test-client-id"

    val mockResponse = mockk<Response<OAuthResponse>>()

    every { mockCall.execute() } returns mockResponse

    every {
      hint(OAuthResponse::class)
      mockResponse.body()
    } returns mockk()

    authenticationService.refreshToken("my test refresh token")

    assertTrue(captor.captured.get("client_id").equals("test-client-id"))
    assertTrue(captor.captured.get("client_secret").equals("test-client-id"))
    assertTrue(captor.captured.get("grant_type").equals("refresh_token"))
    assertTrue(captor.captured.get("refresh_token").equals("my test refresh token"))
  }

  @Test
  fun testIsTokenActiveShouldReturnFalseWithNullToken() {
    val result = authenticationService.isTokenActive(null)

    assertFalse(result)
  }

  @Test
  fun testIsTokenActiveShouldReturnFalseWithInvalidToken() {
    val result = authenticationService.isTokenActive("my invalid token")

    assertFalse(result)
  }

  @Test
  fun testIsTokenActiveShouldReturnFalseWithExpiredToken() {
    val result =
      authenticationService.isTokenActive(
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
          "eyJpc3MiOiJ0b3B0YWwuY29tIiwiZXhwIjoxNDI2NDIwODAwLCJodHRwOi8vdG9wdGFsLmNvbS9qd3RfY2xhaW1zL2lzX2FkbWluIjp0cnVlLCJjb21wYW55IjoiVG9wdGFsIiwiYXdlc29tZSI6dHJ1ZX0." +
          "yRQYnWzskCZUxPwaQupWkiUzKELZ49eM7oWxAQK_ZXw"
      )

    assertFalse(result)
  }

  @Test
  fun testGetLocalSessionTokenShouldReturnNullWithExpiredToken() {
    val token =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
        "eyJpc3MiOiJ0b3B0YWwuY29tIiwiZXhwIjoxNDI2NDIwODAwLCJodHRwOi8vdG9wdGFsLmNvbS9qd3RfY2xhaW1zL2lzX2FkbWluIjp0cnVlLCJjb21wYW55IjoiVG9wdGFsIiwiYXdlc29tZSI6dHJ1ZX0." +
        "yRQYnWzskCZUxPwaQupWkiUzKELZ49eM7oWxAQK_ZXw"

    every { secureSharedPreference.retrieveSessionToken() } returns token

    val result = authenticationService.getLocalSessionToken()

    assertNull(result)

    verify { secureSharedPreference.retrieveSessionToken() }
  }

  @Test
  fun testGetLocalSessionTokenShouldReturnNonEmptyStringWithActiveToken() {
    every { secureSharedPreference.retrieveSessionToken() } returns "some-valid-token"
    every { authenticationService.isTokenActive("some-valid-token") } returns true

    val result = authenticationService.getLocalSessionToken()

    assertEquals("some-valid-token", result)

    verify { secureSharedPreference.retrieveSessionToken() }
    every { authenticationService.isTokenActive("some-valid-token") }
  }

  @Test
  fun testHasActiveShouldReturnTrueWithActiveToken() {
    every { secureSharedPreference.retrieveSessionToken() } returns "some-valid-token"
    every { authenticationService.isTokenActive("some-valid-token") } returns true

    val result = authenticationService.hasActiveSession()

    assertTrue(result)

    verify { secureSharedPreference.retrieveSessionToken() }
    every { authenticationService.isTokenActive("some-valid-token") }
  }

  @Test
  fun testHasActiveShouldReturnTrueWithNonActiveToken() {
    every { secureSharedPreference.retrieveSessionToken() } returns "some-invalid-token"
    every { authenticationService.isTokenActive("some-valid-token") } returns false

    val result = authenticationService.hasActiveSession()

    assertFalse(result)

    verify { secureSharedPreference.retrieveSessionToken() }
    every { authenticationService.isTokenActive("some-invalid-token") }
  }

  @Test
  fun testGetRefreshTokenShouldReturnNullWithExpiredToken() {
    val token =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
        "eyJpc3MiOiJ0b3B0YWwuY29tIiwiZXhwIjoxNDI2NDIwODAwLCJodHRwOi8vdG9wdGFsLmNvbS9qd3RfY2xhaW1zL2lzX2FkbWluIjp0cnVlLCJjb21wYW55IjoiVG9wdGFsIiwiYXdlc29tZSI6dHJ1ZX0." +
        "yRQYnWzskCZUxPwaQupWkiUzKELZ49eM7oWxAQK_ZXw"

    every { secureSharedPreference.retrieveCredentials() } returns
      AuthCredentials("", "", "", token)

    val result = authenticationService.getRefreshToken()

    assertNull(result)

    verify { secureSharedPreference.retrieveCredentials() }
  }

  @Test
  fun testGetRefreshShouldReturnNonEmptyStringWithActiveToken() {
    every { secureSharedPreference.retrieveCredentials() } returns
      AuthCredentials("", "", "", "some-valid-token")
    every { authenticationService.isTokenActive("some-valid-token") } returns true

    val result = authenticationService.getRefreshToken()

    assertEquals("some-valid-token", result)

    verify { secureSharedPreference.retrieveCredentials() }
    every { authenticationService.isTokenActive("some-valid-token") }
  }

  @Test
  fun testValidLocalCredentialsShouldReturnTrueWithCorrectUserPwd() {
    every { secureSharedPreference.retrieveCredentials() } returns
      AuthCredentials("testuser", "testpwd".toSha1(), "", "")

    val result = authenticationService.validLocalCredentials("testuser", "testpwd".toCharArray())

    assertTrue(result)

    verify { secureSharedPreference.retrieveCredentials() }
  }

  @Test
  fun testAddAuthenticatedAccountShouldAddAccountWithCorrectData() {
    val oauth = OAuthResponse()
    oauth.accessToken = "valid access token"
    oauth.refreshToken = "valid refresh token"
    oauth.expiresIn = 1444444444
    oauth.refreshExpiresIn = 1444444444

    every { accountManager.addAccountExplicitly(any(), any(), any()) } returns true
    every { secureSharedPreference.saveCredentials(any()) } just runs
    every { accountManager.notifyAccountAuthenticated(any()) } returns true

    authenticationService.addAuthenticatedAccount(
      Response.success(oauth),
      "testuser",
      "testpwd".toCharArray()
    )

    val account = Account("testuser", "test-account-type")

    verify { accountManager.addAccountExplicitly(account, null, null) }
    verify { accountManager.notifyAccountAuthenticated(any()) }
    verify { secureSharedPreference.saveCredentials(any()) }
  }

  @Test
  fun testLoadActiveAccountShouldDoNothingWithNoSessionUser() {
    every { secureSharedPreference.retrieveSessionUsername() } returns null

    authenticationService.loadActiveAccount({}, Handler())

    verify { authenticationService.getActiveAccount() }
    verify { secureSharedPreference.retrieveSessionUsername() }
    verify(inverse = true) { accountManager.getAccountsByType(any()) }
    verify(inverse = true) { accountManager.getAuthToken(any(), any(), any(), true, any(), any()) }
  }

  @Test
  fun testLoadActiveAccountShouldDoNothingWithNonExistentAccount() {
    every { accountManager.getAccountsByType(any()) } returns arrayOf()
    every { secureSharedPreference.retrieveSessionUsername() } returns "testuser"

    authenticationService.loadActiveAccount({}, Handler())

    verify { secureSharedPreference.retrieveSessionUsername() }
    verify { accountManager.getAccountsByType(any()) }
    verify(inverse = true) { accountManager.getAuthToken(any(), any(), any(), true, any(), any()) }
  }

  @Test
  fun testLoadActiveAccountShouldGetAuthTokenWithExistingAccount() {
    every { accountManager.getAccountsByType(any()) } returns
      arrayOf(Account("testuser", "test-account-type"))
    every { secureSharedPreference.retrieveSessionUsername() } returns "testuser"
    every { accountManager.getAuthToken(any(), any(), any(), true, any(), any()) } returns mockk()

    authenticationService.loadActiveAccount(
      { assertNotNull(it.result.getString(AccountManager.KEY_AUTHTOKEN)) },
      Handler()
    )

    verify { secureSharedPreference.retrieveSessionUsername() }
    verify { accountManager.getAccountsByType(any()) }
    verify(exactly = 1) { accountManager.getAuthToken(any(), any(), any(), true, any(), any()) }
  }
}
