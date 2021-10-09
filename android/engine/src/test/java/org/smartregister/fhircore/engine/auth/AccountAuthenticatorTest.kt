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
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Parcel
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.data.remote.model.response.OAuthResponse
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.ui.login.BaseLoginActivity

class AccountAuthenticatorTest : RobolectricTest() {

  private lateinit var accountAuthenticator: AccountAuthenticator
  private lateinit var context: Application
  private lateinit var authenticationService: FakeAuthenticationService

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    authenticationService = spyk(FakeAuthenticationService(context))
    accountAuthenticator = AccountAuthenticator(context, authenticationService)
  }

  @Test
  fun addAccountShouldBuildBundleWithCustomData() {
    val response = AccountAuthenticatorResponse(Parcel.obtain())
    val bundle = accountAuthenticator.addAccount(response, "", "dummy-auth", null, bundleOf())
    val intent = bundle.getParcelable<Intent>(AccountManager.KEY_INTENT)

    assertNotNull(intent)
    assertEquals(BaseLoginActivity::class.java.canonicalName, intent?.component?.className)
    assertEquals(
      authenticationService.getAccountType(),
      intent?.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE)
    )
    assertEquals(
      response.javaClass.canonicalName,
      intent?.getParcelableExtra<AccountAuthenticatorResponse>(
          AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE
        )
        ?.javaClass
        ?.canonicalName
    )
    assertEquals("dummy-auth", intent?.getStringExtra(AuthenticationService.AUTH_TOKEN_TYPE))
    assertTrue(intent?.getBooleanExtra(AuthenticationService.IS_NEW_ACCOUNT, false) ?: false)
  }

  @Test
  fun getAuthTokenShouldVerifyDifferentScenarios() {

    val response = AccountAuthenticatorResponse(Parcel.obtain())
    val account = Account("demo", "local")
    val authTokenType = "dummy-auth"
    val options = bundleOf()

    val accountManager = mockk<AccountManager>()
    ReflectionHelpers.setField(accountAuthenticator, "accountManager", accountManager)

    every { authenticationService.getLocalSessionToken() } returns null
    every { authenticationService.getRefreshToken() } returns "12345"
    every { authenticationService.refreshToken("12345") } returns OAuthResponse("6789")
    every { authenticationService.updateSession(any()) } returns Unit
    every { accountManager.notifyAccountAuthenticated(account) } returns true

    var bundle = accountAuthenticator.getAuthToken(response, account, authTokenType, options)

    verify(exactly = 1) { authenticationService.getLocalSessionToken() }
    verify(exactly = 1) { authenticationService.getRefreshToken() }
    verify(exactly = 1) { authenticationService.refreshToken("12345") }
    verify(exactly = 1) { authenticationService.updateSession(any()) }
    verify(exactly = 1) { accountManager.notifyAccountAuthenticated(account) }

    assertEquals(account.name, bundle.getString(AccountManager.KEY_ACCOUNT_NAME))
    assertEquals(account.type, bundle.getString(AccountManager.KEY_ACCOUNT_TYPE))
    assertEquals("6789", bundle.getString(AccountManager.KEY_AUTHTOKEN))

    every { authenticationService.refreshToken(any()) } throws RuntimeException()
    bundle = accountAuthenticator.getAuthToken(response, account, authTokenType, options)

    val intent = bundle.getParcelable<Intent>(AccountManager.KEY_INTENT)

    assertNotNull(intent)
    assertEquals(BaseLoginActivity::class.java.canonicalName, intent?.component?.className)
    assertEquals(account.name, intent?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME))
    assertEquals(account.type, intent?.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE))
    assertEquals(
      response.javaClass.canonicalName,
      intent?.getParcelableExtra<AccountAuthenticatorResponse>(
          AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE
        )
        ?.javaClass
        ?.canonicalName
    )
    assertEquals(authTokenType, intent?.getStringExtra(AuthenticationService.AUTH_TOKEN_TYPE))
  }

  @Test
  fun editPropertiesShouldReturnEmptyBundle() {
    val bundle = accountAuthenticator.editProperties(null, null)
    assertNotNull(bundle)
    assertTrue(bundle.isEmpty)
  }

  @Test
  fun confirmCredentialsShouldReturnEmptyBundle() {
    val bundle = accountAuthenticator.confirmCredentials(mockk(), mockk(), null)
    assertNotNull(bundle)
    assertTrue(bundle.isEmpty)
  }

  @Test
  fun getAuthTokenLabelShouldReturnUppercase() {
    val authTokenType = accountAuthenticator.getAuthTokenLabel("auth_type")
    assertEquals("AUTH_TYPE", authTokenType)
  }

  @Test
  fun hasFeaturesShouldReturnBundledData() {
    val bundle = accountAuthenticator.hasFeatures(mockk(), mockk(), arrayOf())
    assertEquals(1, bundle.size())
    assertFalse(bundle.getBoolean(AccountManager.KEY_BOOLEAN_RESULT, true))
  }

  class FakeAuthenticationService(override val context: Context) : AuthenticationService(context) {

    private val applicationConfiguration =
      ApplicationConfiguration(
        "https://keycloak-stage.smartregister.org/auth/realms/FHIR_Android/",
        "https://fhir.labs.smartregister.org/fhir/",
        "fhir-core-client",
        "1528b638-9344-4409-9cbf-10680b4ca5f5",
        "openid"
      )

    override fun skipLogin() = true

    override fun getLoginActivityClass() = BaseLoginActivity::class.java

    override fun getAccountType() = "org.smartregister.fhircore.eir"

    override fun clientSecret() = applicationConfiguration.clientSecret

    override fun clientId() = applicationConfiguration.clientId

    override fun providerScope() = applicationConfiguration.scope

    override fun getApplicationConfigurations() = applicationConfiguration
  }
}
