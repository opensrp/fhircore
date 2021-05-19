package org.smartregister.fhircore.auth.account

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson


class SecureConfig (context: Context) {
    private var myContext = context;

    private val SECURE_STORAGE_FILE_NAME = "fhircore_secure_preferences"
    private val KEY_CREDENTIALS_PREFERENCE = "REMEMBERED_CREDENTIALS"

    private fun getMasterKey(): MasterKey {
        return MasterKey.Builder(myContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private fun getSecurePreferences(): SharedPreferences {
        return EncryptedSharedPreferences
            .create(myContext,
                SECURE_STORAGE_FILE_NAME,
                getMasterKey(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    }

    fun saveCredentials(credentials: Credentials){
        getSecurePreferences().edit {
            putString(KEY_CREDENTIALS_PREFERENCE, Gson().toJson(credentials))
        }
    }

    fun deleteCredentials(){
        getSecurePreferences().edit {
            remove(KEY_CREDENTIALS_PREFERENCE)
        }
    }

    fun retrieveCredentials(): Credentials? {
        val credStr = getSecurePreferences().getString(KEY_CREDENTIALS_PREFERENCE, null)

        if(credStr.isNullOrEmpty()) return null

        return Gson().fromJson(credStr, Credentials::class.java)
    }

}