/*
 * Copyright 2021-2024 Ona Systems, Inc
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
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
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
    val username = "userName"
    secureSharedPreference.saveMultiCredentials(
      username = username,
      password = "!@#$".toCharArray(),
    )
    secureSharedPreference.saveSessionUsername(username)
    Assert.assertEquals(username, secureSharedPreference.retrieveSessionUsername()!!)
  }

  @Test
  fun testRetrieveCredentials() {
    every { secureSharedPreference.get256RandomBytes() } returns byteArrayOf(-100, 0, 100, 101)

    secureSharedPreference.saveMultiCredentials(
      username = "userName",
      password = "!@#$".toCharArray(),
    )

    Assert.assertEquals(
      "userName",
      secureSharedPreference.retrieveCredentials("userName")!!.username,
    )
    Assert.assertEquals(
      "!@#$".toCharArray().toPasswordHash(byteArrayOf(-100, 0, 100, 101)),
      secureSharedPreference.retrieveCredentials("userName")!!.passwordHash,
    )
  }

  @Test
  fun testDeleteCredentialReturnsNull() {
    secureSharedPreference.saveMultiCredentials(
      username = "userName",
      password = "!@#$".toCharArray(),
    )
    Assert.assertNotNull(secureSharedPreference.retrieveCredentials("userName"))
    secureSharedPreference.deleteCredentials("userName")
    Assert.assertNull(secureSharedPreference.retrieveCredentials("userName"))
  }

  @Test
  fun testSaveAndRetrievePin() = runBlocking {
    every { secureSharedPreference.get256RandomBytes() } returns byteArrayOf(-100, 0, 100, 101)
    secureSharedPreference.saveSessionUsername("userName")
    val username = secureSharedPreference.retrieveSessionUsername()!!
    val onSavedPinMock = mockk<() -> Unit>(relaxed = true)
    secureSharedPreference.saveSessionPin(
      username,
      pin = "1234".toCharArray(),
      onSavedPin = onSavedPinMock,
    )

    verify { onSavedPinMock.invoke() }
    Assert.assertEquals(
      "1234".toCharArray().toPasswordHash(byteArrayOf(-100, 0, 100, 101)),
      secureSharedPreference.retrieveSessionUserPin(username),
    )
    secureSharedPreference.deleteSessionPin(username)
    Assert.assertNull(secureSharedPreference.retrieveSessionUserPin(username))
  }

  @Test
  fun testResetSharedPrefsClearsData() = runBlocking {
    every { secureSharedPreference.get256RandomBytes() } returns byteArrayOf(-128, 100, 112, 127)
    secureSharedPreference.saveSessionUsername("userName")
    val username = secureSharedPreference.retrieveSessionUsername()!!
    val onSavedPinMock = mockk<() -> Unit>(relaxed = true)
    secureSharedPreference.saveSessionPin(
      username,
      pin = "6699".toCharArray(),
      onSavedPin = onSavedPinMock,
    )

    verify { onSavedPinMock.invoke() }
    val retrievedSessionPin = secureSharedPreference.retrieveSessionUserPin(username)

    Assert.assertEquals(
      "6699".toCharArray().toPasswordHash(byteArrayOf(-128, 100, 112, 127)),
      retrievedSessionPin,
    )

    secureSharedPreference.resetSharedPrefs()

    Assert.assertNull(secureSharedPreference.retrieveSessionUserPin(username))
  }
}
