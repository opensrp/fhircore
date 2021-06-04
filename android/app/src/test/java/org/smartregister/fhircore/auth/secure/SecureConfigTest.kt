package org.smartregister.fhircore.auth.secure

import FakeKeyStore
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
