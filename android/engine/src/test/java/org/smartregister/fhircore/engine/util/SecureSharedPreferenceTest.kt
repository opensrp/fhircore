/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.util

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
internal class SecureSharedPreferenceTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private lateinit var secureSharedPreference: SecureSharedPreference

  @Before
  fun setUp() {
    secureSharedPreference = spyk(SecureSharedPreference(application))
  }

  @Test
  fun testSaveCredentialsAndRetrieveSessionToken() {
    secureSharedPreference.saveCredentials(username = "userName", password = "!@#$".toCharArray())
    Assert.assertEquals("userName", secureSharedPreference.retrieveSessionUsername()!!)
  }

  @Test
  fun testRetrieveCredentials() {
    every { secureSharedPreference.get256RandomBytes() } returns byteArrayOf(-100, 0, 100, 101)

    secureSharedPreference.saveCredentials(username = "userName", password = "!@#$".toCharArray())

    Assert.assertEquals("userName", secureSharedPreference.retrieveCredentials()!!.username)
    Assert.assertEquals(
      "!@#$".toCharArray().toPasswordHash(byteArrayOf(-100, 0, 100, 101)),
      secureSharedPreference.retrieveCredentials()!!.passwordHash
    )
  }

  @Test
  fun testDeleteCredentialReturnsNull() {
    secureSharedPreference.saveCredentials(username = "userName", password = "!@#$".toCharArray())
    Assert.assertNotNull(secureSharedPreference.retrieveCredentials())
    secureSharedPreference.deleteCredentials()
    Assert.assertNull(secureSharedPreference.retrieveCredentials())
  }

  @Test
  fun testSaveAndRetrievePin() {

    every { secureSharedPreference.get256RandomBytes() } returns byteArrayOf(-100, 0, 100, 101)
    secureSharedPreference.saveSessionPin(pin = "1234".toCharArray())
    Assert.assertEquals(
      "1234".toCharArray().toPasswordHash(byteArrayOf(-100, 0, 100, 101)),
      secureSharedPreference.retrieveSessionPin()
    )
    secureSharedPreference.deleteSessionPin()
    Assert.assertNull(secureSharedPreference.retrieveSessionPin())
  }

  @Test
  fun testResetSharedPrefsClearsData() {

    every { secureSharedPreference.get256RandomBytes() } returns byteArrayOf(-128, 100, 112, 127)

    secureSharedPreference.saveSessionPin(pin = "6699".toCharArray())

    val retrievedSessionPin = secureSharedPreference.retrieveSessionPin()

    Assert.assertEquals(
      "6699".toCharArray().toPasswordHash(byteArrayOf(-128, 100, 112, 127)),
      retrievedSessionPin
    )

    secureSharedPreference.resetSharedPrefs()

    Assert.assertNull(secureSharedPreference.retrieveSessionPin())
  }
}
