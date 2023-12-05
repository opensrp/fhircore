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

package org.smartregister.fhircore.quest.util

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties.KEY_ALGORITHM_HMAC_SHA256
import android.security.keystore.KeyProperties.PURPOSE_SIGN
import androidx.annotation.RequiresApi
import com.google.android.fhir.db.DatabaseEncryptionException
import com.google.android.fhir.db.DatabaseEncryptionException.DatabaseEncryptionErrorCode.UNSUPPORTED
import com.google.android.fhir.db.databaseEncryptionException
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * TODO: This was copied from the SDK since the SDK class is internal.
 * Need to have the functionality exposed from the SDK to avoid the duplication
 */
object DBEncryptionProvider {
  private const val ANDROID_KEYSTORE_NAME = "AndroidKeyStore"

  private const val MESSAGE_TO_BE_SIGNED = "Android FHIR SDK rocks!"

  @Synchronized
  @RequiresApi(Build.VERSION_CODES.M)
  fun getPassphrase(keyName: String): ByteArray {
    val keyStore =
      try {
        KeyStore.getInstance(ANDROID_KEYSTORE_NAME)
      } catch (exception: KeyStoreException) {
        throw exception.databaseEncryptionException
      }

    val hmac =
      try {
        Mac.getInstance(KEY_ALGORITHM_HMAC_SHA256)
      } catch (exception: NoSuchAlgorithmException) {
        throw DatabaseEncryptionException(exception, UNSUPPORTED)
      }

    try {
      keyStore.load(/* param = */ null)
      val signingKey: SecretKey =
        keyStore.getKey(keyName, /* password= */ null) as SecretKey?
          ?: run {
            val keyGenerator =
              KeyGenerator.getInstance(KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEYSTORE_NAME)
            keyGenerator.init(KeyGenParameterSpec.Builder(keyName, PURPOSE_SIGN).build())
            keyGenerator.generateKey()
          }
      hmac.init(signingKey)
      return hmac.doFinal(MESSAGE_TO_BE_SIGNED.toByteArray(StandardCharsets.UTF_8))
    } catch (exception: KeyStoreException) {
      throw exception.databaseEncryptionException
    }
  }
}
