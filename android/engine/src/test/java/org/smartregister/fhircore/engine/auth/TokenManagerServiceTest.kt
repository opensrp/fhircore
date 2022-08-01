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
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.mockk.every
import io.mockk.spyk
import javax.inject.Inject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker.authCredentials
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.SecureSharedPreference

@HiltAndroidTest
class TokenManagerServiceTest : RobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var accountManager: AccountManager

  @Inject lateinit var secureSharedPreference: SecureSharedPreference

  @Inject lateinit var configService: ConfigService

  private lateinit var tokenManagerService: TokenManagerService

  private val context = ApplicationProvider.getApplicationContext<HiltTestApplication>()

  @Before
  fun setUp() {
    hiltRule.inject()
    tokenManagerService =
      spyk(
        TokenManagerService(
          context = context,
          accountManager = accountManager,
          configService = configService,
          secureSharedPreference = secureSharedPreference
        )
      )
  }

  @After
  fun tearDown() {
    secureSharedPreference.deleteCredentials()
  }

  @Test
  fun testLocalSessionTokenWithInactiveToken() {
    Assert.assertNull(tokenManagerService.getLocalSessionToken())
  }

  @Test
  fun testLocalSessionTokenWithActiveToken() {
    every { tokenManagerService.isTokenActive(authCredentials.sessionToken) } returns true
    secureSharedPreference.saveCredentials(authCredentials)
    Assert.assertEquals(authCredentials.sessionToken, tokenManagerService.getLocalSessionToken())
  }

  @Test
  fun testIsTokenActiveWithNullToken() {
    Assert.assertFalse(tokenManagerService.isTokenActive(null))
  }

  @Test
  @Throws(UnsupportedJwtException::class)
  fun testIsTokenActiveWithUnsupportedJwtToken() {
    Assert.assertFalse(tokenManagerService.isTokenActive("gibberish-token"))
  }

  @Test
  @Throws(ExpiredJwtException::class)
  fun testIsTokenActiveWithExpiredJwtToken() {
    Assert.assertFalse(tokenManagerService.isTokenActive("expired-token"))
  }

  @Test
  @Throws(MalformedJwtException::class)
  fun testIsTokenActiveWithMalformedJwtToken() {
    Assert.assertFalse(tokenManagerService.isTokenActive("malformed-token"))
  }

  @Test
  fun testGetActiveAccount() {
    secureSharedPreference.saveCredentials(authCredentials)
    accountManager.addAccountExplicitly(
      Account(authCredentials.username, configService.provideAuthConfiguration().accountType),
      authCredentials.password,
      bundleOf()
    )
    val activeAccount = tokenManagerService.getActiveAccount()
    Assert.assertNotNull(activeAccount)
    Assert.assertEquals(authCredentials.username, activeAccount!!.name)
    Assert.assertEquals(configService.provideAuthConfiguration().accountType, activeAccount.type)
  }
}
