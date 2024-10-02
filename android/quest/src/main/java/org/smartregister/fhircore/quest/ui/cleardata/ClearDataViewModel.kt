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

package org.smartregister.fhircore.quest.ui.cleardata

import android.app.Application
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlin.system.exitProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.totalUnsyncedResources
import timber.log.Timber

@HiltViewModel
class ClearDataViewModel
@Inject
constructor(
  private val appContext: Application,
  val configurationRegistry: ConfigurationRegistry,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val fhirEngine: FhirEngine,
) : ViewModel() {

  private val _dataCleared = MutableStateFlow(false)
  val dataCleared: StateFlow<Boolean>
    get() = _dataCleared

  val applicationConfiguration: ApplicationConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(ConfigType.Application, paramsMap = emptyMap())
  }

  fun clearAppData(activity: ClearDataActivity) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        clearCache()
        clearCodeCache()
        clearDatabases()
        clearSharedPreferences()
        clearFiles()
        clearNoBackup()
        clearAgentLogs()

        _dataCleared.value = true
      } catch (e: Exception) {
        _dataCleared.value = false
      } finally {
        finishAffinity(activity)
        exitProcess(0)
      }
    }
  }

  private fun clearAgentLogs() {
    try {
      val agentLogsDir = File(appContext.filesDir, ".agent-logs")
      agentLogsDir.deleteRecursively()
    } catch (e: Exception) {
      Timber.e(e, "Failed to clear Agent Logs")
    }
  }

  private fun clearCache() {
    try {
      appContext.cacheDir.deleteRecursively()
      appContext.externalCacheDir?.deleteRecursively()
    } catch (e: Exception) {
      Timber.e(e, "Failed to clear cache")
    }
  }

  private fun clearCodeCache() {
    try {
      appContext.codeCacheDir.deleteRecursively()
    } catch (e: Exception) {
      Timber.e(e, "Failed to clear CodeCache")
    }
  }

  private fun clearDatabases() {
    try {
      val databasesDir = appContext.getDatabasePath("databases").parentFile
      databasesDir?.list()?.forEach { dbName -> appContext.deleteDatabase(dbName) }
    } catch (e: Exception) {
      Timber.e(e, "Failed to clear Databases")
    }
  }

  private fun clearFiles() {
    try {
      appContext.filesDir.deleteRecursively()
      appContext.externalCacheDir?.deleteRecursively()
    } catch (e: Exception) {
      Timber.e(e, "Failed to clear files")
    }
  }

  private fun clearNoBackup() {
    try {
      val noBackupDir = File(appContext.noBackupFilesDir.path)
      noBackupDir.deleteRecursively()
    } catch (e: Exception) {
      Timber.e(e, "Failed to clear no_backup directory")
    }
  }

  private fun clearSharedPreferences() {
    try {
      val sharedPrefsDir = File(appContext.applicationContext.filesDir.parent, "shared_prefs")
      sharedPrefsDir.listFiles()?.forEach { file -> file.delete() }
    } catch (e: Exception) {
      Timber.e(e, "Failed to clear SharedPreferences")
    }
  }

  suspend fun getUnsyncedResourceCount(): Int {
    return withContext(Dispatchers.IO) { fhirEngine.totalUnsyncedResources() }
  }

  fun getAppName(): String {
    return try {
      applicationConfiguration.appTitle
    } catch (e: Exception) {
      Timber.e(e, "Failed to get appTitle")
      ""
    }
  }
}
