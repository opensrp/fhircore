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

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson

class SecureConfig(context: Context) {
  private var myContext = context

  fun getMasterKey(): MasterKey {
    return MasterKey.Builder(myContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
  }

  fun getSecurePreferences(): SharedPreferences {
    return EncryptedSharedPreferences.create(
      myContext,
      SECURE_STORAGE_FILE_NAME,
      getMasterKey(),
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
  }

  fun saveCredentials(credentials: Credentials) {
    getSecurePreferences().edit {
      putString(KEY_LATEST_CREDENTIALS_PREFERENCE, Gson().toJson(credentials))
      putString(KEY_LATEST_SESSION_TOKEN_PREFERENCE, credentials.sessionToken)
    }
  }

  fun deleteCredentials() {
    getSecurePreferences().edit {
      remove(KEY_LATEST_CREDENTIALS_PREFERENCE)
      remove(KEY_LATEST_SESSION_TOKEN_PREFERENCE)
    }
  }

  fun retrieveSessionToken(): String? {
    return getSecurePreferences().getString(KEY_LATEST_SESSION_TOKEN_PREFERENCE, null)
  }

  fun retrieveSessionUsername(): String? {
    return retrieveCredentials()?.username
  }

  fun retrieveCredentials(): Credentials? {
    val credStr = getSecurePreferences().getString(KEY_LATEST_CREDENTIALS_PREFERENCE, null)

    if (credStr.isNullOrEmpty()) return null

    return Gson().fromJson(credStr, Credentials::class.java)
  }

  companion object {
    const val SECURE_STORAGE_FILE_NAME = "fhircore_secure_preferences"
    const val KEY_LATEST_CREDENTIALS_PREFERENCE = "LATEST_SUCCESSFUL_SESSION_CREDENTIALS"
    const val KEY_LATEST_SESSION_TOKEN_PREFERENCE = "LATEST_SUCCESSFUL_SESSION_TOKEN"
  }
}
