
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

package org.smartregister.fhircore.auth.secure

import SecureConfig
import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SecureConfigTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()

  private val testMasterKey =
    MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

  private val secureConfig = SecureConfig(context)

  private val testSharedPreferences =
    EncryptedSharedPreferences.create(
      context,
      SecureConfig.SECURE_STORAGE_FILE_NAME,
      testMasterKey,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

  @Test
  fun `verify secure preferences credentials save`() {
    val credentials = Credentials("testuser", "testpw".toCharArray(), "my-token")
    val credentialsExpectedStr = Gson().toJson(credentials)

    secureConfig.saveCredentials(credentials)

    val credentialsSavedStr =
      testSharedPreferences.getString(SecureConfig.KEY_LATEST_CREDENTIALS_PREFERENCE, null)

    assertEquals(credentialsExpectedStr, credentialsSavedStr)
  }

  @Test
  fun `verify secure preferences credentials retrieve`() {
    val credentials = Credentials("testuser", "testpw".toCharArray(), "my-token")
    val credentialsExpectedStr = Gson().toJson(credentials)

    testSharedPreferences.edit {
      putString(SecureConfig.KEY_LATEST_CREDENTIALS_PREFERENCE, credentialsExpectedStr)
    }

    val credentialsRetrieved = secureConfig.retrieveCredentials()!!

    assertEquals(credentials.username, credentialsRetrieved.username)
    assertEquals(
      credentials.password.concatToString(),
      credentialsRetrieved.password.concatToString()
    )
    assertEquals(credentials.sessionToken, credentialsRetrieved.sessionToken)
  }

  @Test
  fun `verify secure preferences session token retrieve`() {
    val credentials = Credentials("testuser", "testpw".toCharArray(), "my-token")

    secureConfig.saveCredentials(credentials)

    val sessionToken = secureConfig.retrieveSessionToken()

    assertEquals("my-token", sessionToken)
  }

  @Test
  fun `verify secure preferences session username retrieve`() {
    val credentials = Credentials("testuser", "testpw".toCharArray(), "my-token")
    val credentialsExpectedStr = Gson().toJson(credentials)

    testSharedPreferences.edit {
      putString(SecureConfig.KEY_LATEST_CREDENTIALS_PREFERENCE, credentialsExpectedStr)
    }

    val sessionUsername = secureConfig.retrieveSessionUsername()

    assertEquals("testuser", sessionUsername)
  }

  @Test
  fun `verify secure preferences credentials delete`() {
    val credentials = Credentials("testuser", "testpw".toCharArray(), "my-token")
    val credentialsStr = Gson().toJson(credentials)

    testSharedPreferences.edit {
      putString(SecureConfig.KEY_LATEST_CREDENTIALS_PREFERENCE, credentialsStr)
    }

    secureConfig.deleteCredentials()

    var currentVal =
      testSharedPreferences.getString(SecureConfig.KEY_LATEST_CREDENTIALS_PREFERENCE, null)

    assertNull(currentVal)
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}
