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
import android.accounts.AccountManager.KEY_INTENT
import android.content.Intent
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.mockk
import io.mockk.spyk
import java.util.Locale
import javax.inject.Inject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.auth.AccountAuthenticator.Companion.AUTH_TOKEN_TYPE
import org.smartregister.fhircore.engine.auth.AccountAuthenticator.Companion.IS_NEW_ACCOUNT
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.remote.auth.OAuthService
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.SecureSharedPreference

@HiltAndroidTest
class AccountAuthenticatorTest : RobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var accountManager: AccountManager

  @Inject lateinit var oAuthService: OAuthService

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  @Inject lateinit var secureSharedPreference: SecureSharedPreference

  @Inject lateinit var tokenManagerService: TokenManagerService

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
          configurationRegistry = configurationRegistry,
          secureSharedPreference = secureSharedPreference,
          tokenManagerService = tokenManagerService
        )
      )
  }

  @Test
  fun testThatAccountIsAddedWithCorrectConfigs() {

    val bundle =
      accountAuthenticator.addAccount(
        response = mockk(relaxed = true),
        accountType = configurationRegistry.authConfiguration.accountType,
        authTokenType = authTokenType,
        requiredFeatures = emptyArray(),
        options = bundleOf()
      )
    Assert.assertNotNull(bundle)
    val parcelable = bundle.getParcelable<Intent>(KEY_INTENT)
    Assert.assertNotNull(parcelable)
    Assert.assertNotNull(parcelable!!.extras)
    Assert.assertEquals(
      configurationRegistry.authConfiguration.accountType,
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
        accountType = configurationRegistry.authConfiguration.accountType
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
}
