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

package org.smartregister.fhircore.quest.ui.usersetting

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Locale
import java.util.zip.ZipException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.EncryptionMethod
import net.sqlcipher.database.SQLiteDatabase
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.fetchLanguages
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.engine.util.extension.isDeviceOnline
import org.smartregister.fhircore.engine.util.extension.launchActivityWithNoBackStackHistory
import org.smartregister.fhircore.engine.util.extension.refresh
import org.smartregister.fhircore.engine.util.extension.setAppLocale
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.engine.util.extension.spaceByUppercase
import org.smartregister.fhircore.engine.util.extension.today
import org.smartregister.fhircore.quest.ui.appsetting.AppSettingActivity
import org.smartregister.fhircore.quest.ui.login.AccountAuthenticator
import org.smartregister.fhircore.quest.ui.login.LoginActivity
import org.smartregister.fhircore.quest.util.DBEncryptionProvider
import org.smartregister.p2p.utils.startP2PScreen
import timber.log.Timber

@HiltViewModel
class UserSettingViewModel
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val syncBroadcaster: SyncBroadcaster,
  val accountAuthenticator: AccountAuthenticator,
  val secureSharedPreference: SecureSharedPreference,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val configurationRegistry: ConfigurationRegistry,
  val workManager: WorkManager,
  val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

  val languages by lazy { configurationRegistry.fetchLanguages() }
  val showDBResetConfirmationDialog = MutableLiveData(false)
  val progressBarState = MutableLiveData(Pair(false, 0))
  val showProgressIndicatorFlow = MutableStateFlow(false)
  val unsyncedResourcesMutableSharedFlow = MutableSharedFlow<List<Pair<String, Int>>>()
  private val applicationConfiguration: ApplicationConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(ConfigType.Application)
  }

  fun retrieveUsername(): String? = secureSharedPreference.retrieveSessionUsername()

  fun retrieveLastSyncTimestamp(): String? =
    sharedPreferencesHelper.read(SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name, null)

  fun allowSwitchingLanguages() = languages.size > 1

  fun loadSelectedLanguage(): String =
    Locale.forLanguageTag(
        sharedPreferencesHelper.read(SharedPreferenceKey.LANG.name, Locale.ENGLISH.toLanguageTag())
          ?: Locale.ENGLISH.toLanguageTag(),
      )
      .displayName

  fun onEvent(event: UserSettingsEvent) {
    when (event) {
      is UserSettingsEvent.Logout -> {
        updateProgressBarState(true, R.string.logging_out)
        event.context.getActivity()?.let { activity ->
          // Attempt to logout remotely if user is online
          if (activity.isDeviceOnline()) {
            accountAuthenticator.logout {
              updateProgressBarState(false, R.string.logging_out)
              activity.launchActivityWithNoBackStackHistory<LoginActivity>()
            }
          } else {
            activity.launchActivityWithNoBackStackHistory<LoginActivity>()
          }
        }
      }
      is UserSettingsEvent.SyncData -> {
        if (event.context.isDeviceOnline()) {
          viewModelScope.launch(dispatcherProvider.main()) { syncBroadcaster.runOneTimeSync() }
        } else {
          event.context.showToast(event.context.getString(R.string.sync_failed), Toast.LENGTH_LONG)
        }
      }
      is UserSettingsEvent.SwitchLanguage -> {
        sharedPreferencesHelper.write(SharedPreferenceKey.LANG.name, event.language.tag)
        event.context.run {
          configurationRegistry.clearConfigsCache()
          setAppLocale(event.language.tag)
          getActivity()?.refresh()
        }
      }
      is UserSettingsEvent.ShowResetDatabaseConfirmationDialog ->
        showDBResetConfirmationDialog.postValue(event.isShow)
      is UserSettingsEvent.ResetDatabaseFlag -> if (event.isReset) this.resetAppData(event.context)
      is UserSettingsEvent.ShowLoaderView ->
        updateProgressBarState(event.show, event.messageResourceId)
      is UserSettingsEvent.SwitchToP2PScreen -> startP2PScreen(context = event.context)
      is UserSettingsEvent.ShowInsightsView -> renderInsightsView(event.context)
      is UserSettingsEvent.ExportDB -> {
        updateProgressBarState(true, R.string.exporting_db)
        copyDatabase(event.context)
        updateProgressBarState(false, R.string.exporting_db)
      }
    }
  }

  private fun updateProgressBarState(isShown: Boolean, messageResourceId: Int) {
    progressBarState.postValue(Pair(isShown, messageResourceId))
  }

  /**
   * This function clears all the data for the app from the device, cancels all background tasks,
   * deletes the logged in user account from device accounts and finally clear all data stored in
   * shared preferences.
   */
  fun resetAppData(context: Context) {
    viewModelScope.launch {
      workManager.cancelAllWork()

      withContext(dispatcherProvider.io()) { fhirEngine.clearDatabase() }

      accountAuthenticator.invalidateSession {
        sharedPreferencesHelper.resetSharedPrefs()
        secureSharedPreference.resetSharedPrefs()
        context.getActivity()?.launchActivityWithNoBackStackHistory<AppSettingActivity>()
      }
    }
  }

  fun enabledDeviceToDeviceSync(): Boolean = applicationConfiguration.deviceToDeviceSync != null

  fun renderInsightsView(context: Context) {
    viewModelScope.launch {
      showProgressIndicatorFlow.emit(true)

      withContext(dispatcherProvider.io()) {
        val unsyncedResources =
          fhirEngine
            .getUnsyncedLocalChanges()
            .distinctBy { it.resourceId }
            .groupingBy { it.resourceType.spaceByUppercase() }
            .eachCount()
            .map { it.key to it.value }

        showProgressIndicatorFlow.emit(false)

        if (unsyncedResources.isNullOrEmpty()) {
          withContext(dispatcherProvider.main()) {
            context.showToast(context.getString(R.string.all_data_synced))
          }
        } else {
          unsyncedResourcesMutableSharedFlow.emit(unsyncedResources)
        }
      }
    }
  }

  fun dismissInsightsView() {
    viewModelScope.launch { unsyncedResourcesMutableSharedFlow.emit(listOf()) }
  }

  private fun copyDatabase(context: Context) {
    viewModelScope.launch {
      try {
        val passphrase = DBEncryptionProvider.getOrCreatePassphrase("fhirEngineDbPassphrase")

        var dbFileName = if (BuildConfig.DEBUG) "resources" else "resources_encrypted"
        val appDbPath = File("/data/data/${context.packageName}/databases/$dbFileName.db")

        val downloadsDir =
          Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        val timestamp = today().formatDate("yyyyMMdd-HHmmss")
        var backupPath =
          File(downloadsDir, "${applicationConfiguration.appTitle.replace(" ", "_")}_$timestamp.db")

        decryptDb(appDbPath, backupPath, passphrase)
        zipPlaintextDb(backupPath, "password")
      } catch (e: Exception) {
        Timber.e(e, "Failed to copy application's database")
      }
    }
  }

  private fun decryptDb(databaseFile: File, backupFile: File, passphrase: ByteArray?) {
    if (databaseFile.exists()) {
      val encryptedDb =
        SQLiteDatabase.openDatabase(databaseFile.absolutePath, passphrase, null, 0, null, null)

      android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(
          backupFile.absolutePath,
          null,
          null
        )
        .close() // create an empty database

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
    } else {
      throw FileNotFoundException(databaseFile.absolutePath + " not found")
    }
  }

  private fun zipPlaintextDb(plaintextDbFile: File, password: String) {
    val zipParameters = ZipParameters()
    zipParameters.isEncryptFiles = true
    zipParameters.compressionLevel = CompressionLevel.HIGHER
    zipParameters.encryptionMethod = EncryptionMethod.AES

    val zipFile = ZipFile("${plaintextDbFile.absolutePath}.zip", password.toCharArray())
    try {
      zipFile.addFile(plaintextDbFile, zipParameters)
    } catch (e: ZipException) {
      Timber.e(e, "Failed to add file to zip")
    }

    try {
      if (!plaintextDbFile.delete()) {
        Timber.e("Failed to delete plaintext database file")
      }
      if (File("${plaintextDbFile.absolutePath}-journal").delete()) {
        Timber.e("Failed to delete plaintext database journal file")
      }
    } catch (e: IOException) {
      Timber.e(e)
    }
  }
}
