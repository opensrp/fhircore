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
import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.CurrentSyncJobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlin.system.exitProcess
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.sync.SyncListenerManager
import org.smartregister.fhircore.engine.sync.SyncState
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.engine.util.extension.isDeviceOnline
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.quest.BuildConfig
import timber.log.Timber

@HiltViewModel
class ClearDataViewModel
@Inject
constructor(
  private val appContext: Application,
  val configurationRegistry: ConfigurationRegistry,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val fhirEngine: FhirEngine,
  val dispatcherProvider: DispatcherProvider,
  val syncBroadcaster: SyncBroadcaster,
  val syncListenerManager: SyncListenerManager,
) : ViewModel(), OnSyncListener {

  val isDebug: Boolean = false

  private val _unsyncedResourceCount = MutableStateFlow(0)
  val unsyncedResourceCount: StateFlow<Int> = _unsyncedResourceCount

  private val _appName = MutableStateFlow("")
  val appName: StateFlow<String> = _appName

  private val _isClearing = MutableStateFlow(false)
  val isClearing: StateFlow<Boolean> = _isClearing

  private val _navigateBack = MutableStateFlow(false)
  val navigateBack: StateFlow<Boolean> = _navigateBack

  private val _isSyncing = MutableStateFlow(false)
  val isSyncing: StateFlow<Boolean> = _isSyncing

  private var clearDataJob: Job? = null

  val applicationConfiguration: ApplicationConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(ConfigType.Application, paramsMap = emptyMap())
  }

  init {
    syncListenerManager.registerSyncListener(this)
    viewModelScope.launch {
      _unsyncedResourceCount.value =
        withContext(dispatcherProvider.io()) { fhirEngine.getUnsyncedLocalChanges().size }
      _appName.value =
        try {
          applicationConfiguration.appTitle
        } catch (e: Exception) {
          BuildConfig.FLAVOR
        }
    }
  }

  override fun onSync(syncState: SyncState) {
    when (syncState.currentSyncJobStatus) {
      is CurrentSyncJobStatus.Running -> _isSyncing.value = true
      else -> {
        _isSyncing.value = false
        viewModelScope.launch {
          _unsyncedResourceCount.value =
            withContext(dispatcherProvider.io()) { fhirEngine.getUnsyncedLocalChanges().size }
        }
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    syncListenerManager.deregisterSyncListener(this)
  }

  fun onEvent(event: ClearDataEvent) {
    when (event) {
      is ClearDataEvent.SyncData -> syncData(event.context)
      is ClearDataEvent.ClearAppData -> {
        event.context.getActivity()?.let { clearAppData(it as AppCompatActivity) }
      }
      is ClearDataEvent.AbortClearData -> abortClearData()
    }
  }

  fun syncData(context: Context) {
    if (context.isDeviceOnline()) {
      viewModelScope.launch(dispatcherProvider.main()) { syncBroadcaster.runOneTimeSync() }
    } else {
      context.showToast(context.getString(R.string.sync_failed), Toast.LENGTH_LONG)
    }
  }

  fun clearAppData(activity: AppCompatActivity) {
    _isClearing.value = true
    clearDataJob =
      viewModelScope.launch(dispatcherProvider.io()) {
        try {
          clearCache()
          clearCodeCache()
          clearDatabases()
          clearSharedPreferences()
          clearFiles()
          clearNoBackup()
          clearAgentLogs()

          withContext(NonCancellable + dispatcherProvider.main()) {
            Toast.makeText(
                appContext,
                appContext.getString(
                  org.smartregister.fhircore.quest.R.string.clear_data_cleared,
                  _appName.value,
                ),
                Toast.LENGTH_SHORT,
              )
              .show()
            delay(2000)
            finishAffinity(activity)
            exitProcess(0)
          }
        } catch (e: Exception) {
          Timber.e(e, "Failed to clear app data")
        } finally {
          withContext(NonCancellable + dispatcherProvider.main()) { _isClearing.value = false }
        }
      }
  }

  fun abortClearData() {
    clearDataJob?.cancel()
    _navigateBack.value = true
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
}
