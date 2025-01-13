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

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.google.android.fhir.db.DatabaseEncryptionException
import com.google.android.fhir.db.databaseEncryptionException
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import net.sqlcipher.database.SQLiteDatabase
import timber.log.Timber

object DBUtils {

    private const val ANDROID_KEYSTORE_NAME = "AndroidKeyStore"

    private const val MESSAGE_TO_BE_SIGNED = "Android FHIR SDK rocks!"

    @Synchronized
    fun getEncryptionPassphrase(keyName: String): ByteArray {
        val keyStore =
            try {
                KeyStore.getInstance(ANDROID_KEYSTORE_NAME)
            } catch (exception: KeyStoreException) {
                throw exception.databaseEncryptionException
            }

        val hmac =
            try {
                Mac.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256)
            } catch (exception: NoSuchAlgorithmException) {
                throw DatabaseEncryptionException(
                    exception,
                    DatabaseEncryptionException.DatabaseEncryptionErrorCode.UNSUPPORTED
                )
            }

        try {
            keyStore.load(/* param = */ null)
            val signingKey: SecretKey =
                keyStore.getKey(keyName, /* password= */ null) as SecretKey?
                    ?: run {
                        val keyGenerator =
                            KeyGenerator.getInstance(
                                KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
                                ANDROID_KEYSTORE_NAME
                            )
                        keyGenerator.init(
                            KeyGenParameterSpec.Builder(keyName, KeyProperties.PURPOSE_SIGN).build()
                        )
                        keyGenerator.generateKey()
                    }
            hmac.init(signingKey)
            return hmac.doFinal(MESSAGE_TO_BE_SIGNED.toByteArray(StandardCharsets.UTF_8))
        } catch (exception: KeyStoreException) {
            throw exception.databaseEncryptionException
        }
    }

    fun copyUnencryptedDb(databaseFile: File, backupFile: File): Boolean {
        return try {
            val src = FileInputStream(databaseFile).channel
            val dst = FileOutputStream(backupFile).channel
            dst.transferFrom(src, 0, src.size())
            src.close()
            dst.close()

            true
        } catch (e: FileNotFoundException) {
            Timber.e(e, "File does not exist")
            false
        } catch (e: SecurityException) {
            Timber.e(e, "No permissions to delete file")
            false
        }
    }

    fun decryptDb(databaseFile: File, backupFile: File, passphrase: ByteArray?): Boolean {
        if (backupFile.parentFile.canWrite() && databaseFile.exists()) {
            val encryptedDb =
                SQLiteDatabase.openDatabase(databaseFile.absolutePath, passphrase, null, 0, null, null)

            // create an empty database
            android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(
                backupFile.absolutePath,
                null,
                null
            )
                .close()

            val statement = encryptedDb.compileStatement("ATTACH DATABASE ? AS plaintext KEY ''")

            statement.bindString(1, backupFile.absolutePath)
            statement.execute()
            encryptedDb.rawExecSQL("SELECT sqlcipher_export('plaintext')")
            encryptedDb.rawExecSQL("DETACH DATABASE plaintext")

            val version = encryptedDb.version

            statement.close()
            encryptedDb.close()

            val plaintTextDb =
                android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(
                    backupFile.absolutePath,
                    null,
                    null
                )

            plaintTextDb.version = version
            plaintTextDb.close()

            return true
        } else {
            Timber.e(databaseFile.absolutePath + " not found")
            return false
        }
    }
}