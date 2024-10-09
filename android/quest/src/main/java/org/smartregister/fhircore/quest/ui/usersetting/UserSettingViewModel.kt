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

package org.smartregister.fhircore.quest.ui.usersetting

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.SettingsOptions
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.datastore.PreferenceDataStore
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MMM_DD_HH_MM_SS
import org.smartregister.fhircore.engine.util.extension.countUnSyncedResources
import org.smartregister.fhircore.engine.util.extension.fetchLanguages
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.engine.util.extension.isDeviceOnline
import org.smartregister.fhircore.engine.util.extension.launchActivityWithNoBackStackHistory
import org.smartregister.fhircore.engine.util.extension.reformatDate
import org.smartregister.fhircore.engine.util.extension.refresh
import org.smartregister.fhircore.engine.util.extension.setAppLocale
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.quest.BuildConfig
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.ui.appsetting.AppSettingActivity
import org.smartregister.fhircore.quest.ui.login.AccountAuthenticator
import org.smartregister.fhircore.quest.ui.login.LoginActivity
import org.smartregister.p2p.utils.startP2PScreen

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
  private val preferenceDataStore: PreferenceDataStore,
) : ViewModel() {

  val languages by lazy { configurationRegistry.fetchLanguages() }
  val showDBResetConfirmationDialog = MutableLiveData(false)
  val progressBarState = MutableLiveData(Pair(false, 0))
  val showProgressIndicatorFlow = MutableStateFlow(false)
  val unsyncedResourcesMutableSharedFlow = MutableSharedFlow<List<Pair<String, Int>>>()
  private val applicationConfiguration: ApplicationConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(ConfigType.Application)
  }
  private val _snackBarStateFlow = MutableSharedFlow<SnackBarMessageConfig>()
  val snackBarStateFlow = _snackBarStateFlow.asSharedFlow()

  val appVersionCode = BuildConfig.VERSION_CODE
  val appVersionName = BuildConfig.VERSION_NAME

  fun retrieveUsername(): String? = secureSharedPreference.retrieveSessionUsername()

  fun retrieveUserInfo() =
    sharedPreferencesHelper.read<UserInfo>(
      key = SharedPreferenceKey.USER_INFO.name,
    )

  fun practitionerLocation() =
    sharedPreferencesHelper.read(SharedPreferenceKey.PRACTITIONER_LOCATION.name, null)

  fun retrieveOrganization() =
    sharedPreferencesHelper.read(SharedPreferenceKey.ORGANIZATION.name, null)

  fun retrieveCareTeam() = sharedPreferencesHelper.read(SharedPreferenceKey.CARE_TEAM.name, null)

  fun retrieveDataMigrationVersion(): String = runBlocking {
    (preferenceDataStore.read(PreferenceDataStore.MIGRATION_VERSION).firstOrNull() ?: 0).toString()
  }

  fun retrieveLastSyncTimestamp(): String? =
    sharedPreferencesHelper.read(SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name, null)

  fun enableMenuOption(settingOption: SettingsOptions) =
    applicationConfiguration.settingsScreenMenuOptions.contains(settingOption)

  fun allowSwitchingLanguages() =
    enableMenuOption(SettingsOptions.SWITCH_LANGUAGES) && languages.size > 1

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
          event.context.showToast(
            event.context.getString(R.string.sync_failed),
            Toast.LENGTH_LONG,
          )
        }
      }
      is UserSettingsEvent.SwitchLanguage -> {
        sharedPreferencesHelper.write(SharedPreferenceKey.LANG.name, event.language.tag)
        event.context.run {
          configurationRegistry.configCacheMap.clear()
          setAppLocale(event.language.tag)
          getActivity()?.refresh()
        }
      }
      is UserSettingsEvent.ShowResetDatabaseConfirmationDialog ->
        showDBResetConfirmationDialog.postValue(
          event.isShow,
        )
      is UserSettingsEvent.ResetDatabaseFlag -> if (event.isReset) this.resetAppData(event.context)
      is UserSettingsEvent.ShowLoaderView ->
        updateProgressBarState(
          event.show,
          event.messageResourceId,
        )
      is UserSettingsEvent.SwitchToP2PScreen -> startP2PScreen(context = event.context)
      is UserSettingsEvent.ShowContactView -> {}
      is UserSettingsEvent.OnLaunchOfflineMap -> {}
      is UserSettingsEvent.ShowInsightsScreen -> {
        event.navController.navigate(MainNavigationScreen.Insight.route)
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

  fun getDateFormat() = applicationConfiguration.dateFormat

  fun getBuildDate() =
    reformatDate(
      inputDateString = BuildConfig.BUILD_DATE,
      currentFormat = SDF_YYYY_MMM_DD_HH_MM_SS,
      desiredFormat = applicationConfiguration.dateFormat,
    )

  fun fetchUnsyncedResources() {
    viewModelScope.launch {
      withContext(dispatcherProvider.io()) {
        showProgressIndicatorFlow.emit(true)
        val unsyncedResources = fhirEngine.countUnSyncedResources()
        showProgressIndicatorFlow.emit(false)
        unsyncedResourcesMutableSharedFlow.emit(unsyncedResources)
      }
    }
  }

  suspend fun emitSnackBarState(snackBarMessageConfig: SnackBarMessageConfig) {
    _snackBarStateFlow.emit(snackBarMessageConfig)
  }
}
