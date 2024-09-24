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

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.annotations.VisibleForTesting
import org.smartregister.fhircore.engine.auth.AuthCredentials
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.encodeJson

@Singleton
class SecureSharedPreference @Inject constructor(@ApplicationContext val context: Context) {
  private val secureSharedPreferences: SharedPreferences by lazy {
    initEncryptedSharedPreferences()
  }

  @VisibleForTesting
  fun initEncryptedSharedPreferences() =
    runCatching { createEncryptedSharedPreferences() }
      .getOrElse {
        resetSharedPrefs()
        createEncryptedSharedPreferences()
      }

  @VisibleForTesting
  fun createEncryptedSharedPreferences() =
    EncryptedSharedPreferences.create(
      context,
      SECURE_STORAGE_FILE_NAME,
      getMasterKey(),
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

  private fun getMasterKey() =
    MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

  fun saveCredentials(username: String, password: CharArray) {
    val randomSaltBytes = get256RandomBytes()

    secureSharedPreferences.edit {
      putString(
        SharedPreferenceKey.LOGIN_CREDENTIAL_KEY.name,
        AuthCredentials(
            username = username,
            salt = Base64.getEncoder().encodeToString(randomSaltBytes),
            passwordHash = password.toPasswordHash(randomSaltBytes),
          )
          .encodeJson(),
      )
    }
    clearPasswordInMemory(password)
  }

  fun deleteCredentials() =
    secureSharedPreferences.edit { remove(SharedPreferenceKey.LOGIN_CREDENTIAL_KEY.name) }

  fun retrieveSessionUsername() = retrieveCredentials()?.username

  fun retrieveCredentials(): AuthCredentials? =
    secureSharedPreferences
      .getString(SharedPreferenceKey.LOGIN_CREDENTIAL_KEY.name, null)
      ?.decodeJson<AuthCredentials>()

  suspend fun saveSessionPin(pin: CharArray, onSavedPin: () -> Unit) {
    val randomSaltBytes = get256RandomBytes()
    secureSharedPreferences.edit {
      putString(
        SharedPreferenceKey.LOGIN_PIN_SALT.name,
        Base64.getEncoder().encodeToString(randomSaltBytes),
      )
      putString(
        SharedPreferenceKey.LOGIN_PIN_KEY.name,
        coroutineScope {
          async(Dispatchers.Default) { pin.toPasswordHash(randomSaltBytes) }.await()
        },
      )
    }
    onSavedPin()
  }

  @VisibleForTesting fun get256RandomBytes() = 256.getRandomBytesOfSize()

  fun retrievePinSalt() =
    secureSharedPreferences.getString(SharedPreferenceKey.LOGIN_PIN_SALT.name, null)

  fun retrieveSessionPin() =
    secureSharedPreferences.getString(SharedPreferenceKey.LOGIN_PIN_KEY.name, null)

  fun deleteSessionPin() =
    secureSharedPreferences.edit { remove(SharedPreferenceKey.LOGIN_PIN_KEY.name) }

  /** This method resets/clears all existing values in the shared preferences synchronously */
  fun resetSharedPrefs() = secureSharedPreferences.edit { clear() }

  companion object {
    const val SECURE_STORAGE_FILE_NAME = "fhircore_secure_preferences"
  }
}
