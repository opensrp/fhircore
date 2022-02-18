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

package org.smartregister.fhircore.engine.util

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.smartregister.fhircore.engine.auth.AuthCredentials
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.encodeJson

@Singleton
class SecureSharedPreference @Inject constructor(@ApplicationContext val context: Context) {

  val secureSharedPreferences =
    EncryptedSharedPreferences.create(
      context,
      SECURE_STORAGE_FILE_NAME,
      getMasterKey(),
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

  private fun getMasterKey() =
    MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

  fun saveCredentials(authCredentials: AuthCredentials) {
    secureSharedPreferences.edit {
      putString(KEY_LATEST_CREDENTIALS_PREFERENCE, authCredentials.encodeJson())
      putString(KEY_LATEST_SESSION_TOKEN_PREFERENCE, authCredentials.sessionToken)
    }
  }

  fun deleteCredentials() {
    secureSharedPreferences.edit {
      remove(KEY_LATEST_CREDENTIALS_PREFERENCE)
      remove(KEY_LATEST_SESSION_TOKEN_PREFERENCE)
      remove(KEY_SESSION_PIN)
    }
  }

  fun retrieveSessionToken() =
    secureSharedPreferences.getString(KEY_LATEST_SESSION_TOKEN_PREFERENCE, null)

  fun retrieveSessionUsername() = retrieveCredentials()?.username

  fun retrieveCredentials(): AuthCredentials? {
    return secureSharedPreferences
      .getString(KEY_LATEST_CREDENTIALS_PREFERENCE, null)
      ?.decodeJson<AuthCredentials>()
  }

  fun saveSessionPin(pin: String) {
    secureSharedPreferences.edit { putString(KEY_SESSION_PIN, pin) }
  }

  fun retrieveSessionPin() = secureSharedPreferences.getString(KEY_SESSION_PIN, null)

  fun deleteSessionPin() {
    secureSharedPreferences.edit { remove(KEY_SESSION_PIN) }
  }

  companion object {
    const val SECURE_STORAGE_FILE_NAME = "fhircore_secure_preferences"
    const val KEY_LATEST_CREDENTIALS_PREFERENCE = "LATEST_SUCCESSFUL_SESSION_CREDENTIALS"
    const val KEY_LATEST_SESSION_TOKEN_PREFERENCE = "LATEST_SUCCESSFUL_SESSION_TOKEN"
    const val KEY_SESSION_PIN = "KEY_SESSION_PIN"
  }
}
