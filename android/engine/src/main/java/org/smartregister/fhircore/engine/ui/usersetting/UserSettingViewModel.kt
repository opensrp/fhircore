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

package org.smartregister.fhircore.engine.ui.usersetting

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.ui.appsetting.AppSettingActivity
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.fetchLanguages
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.engine.util.extension.refresh
import org.smartregister.fhircore.engine.util.extension.setAppLocale

@HiltViewModel
class UserSettingViewModel
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val syncBroadcaster: SyncBroadcaster,
  val accountAuthenticator: AccountAuthenticator,
  val secureSharedPreference: SecureSharedPreference,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val configurationRegistry: ConfigurationRegistry
) : ViewModel() {

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  val languages by lazy { configurationRegistry.fetchLanguages() }

  val onLogout = MutableLiveData<Boolean?>(null)

  val showDBResetConfirmationDialog = MutableLiveData(false)

  val progressBarState = MutableLiveData(Pair(false, 0))

  fun retrieveUsername(): String? = secureSharedPreference.retrieveSessionUsername()

  fun allowSwitchingLanguages() = languages.size > 1

  fun loadSelectedLanguage(): String =
    Locale.forLanguageTag(
        sharedPreferencesHelper.read(SharedPreferenceKey.LANG.name, Locale.ENGLISH.toLanguageTag())
          ?: Locale.ENGLISH.toLanguageTag()
      )
      .displayName

  fun onEvent(event: UserSettingsEvent) {
    when (event) {
      is UserSettingsEvent.Logout -> {
        updateProgressBarState(true, R.string.logging_out)
        accountAuthenticator.logout { updateProgressBarState(false, R.string.logging_out) }
      }
      is UserSettingsEvent.SyncData -> syncBroadcaster.runSync()
      is UserSettingsEvent.SwitchLanguage -> {
        sharedPreferencesHelper.write(SharedPreferenceKey.LANG.name, event.language.tag)
        event.context.run {
          setAppLocale(event.language.tag)
          getActivity()?.refresh()
        }
      }
      is UserSettingsEvent.ShowResetDatabaseConfirmationDialog ->
        showResetDatabaseConfirmationDialogFlag(event.isShow)
      is UserSettingsEvent.ResetDatabaseFlag ->
        if (event.isReset) this.resetDatabase(dispatcherProvider.io())
      is UserSettingsEvent.ShowLoaderView ->
        updateProgressBarState(event.show, event.messageResourceId)
    }
  }

  fun showResetDatabaseConfirmationDialogFlag(isClearDatabase: Boolean) {
    showDBResetConfirmationDialog.postValue(isClearDatabase)
  }

  fun updateProgressBarState(isShown: Boolean, messageResourceId: Int) {
    progressBarState.postValue(Pair(isShown, messageResourceId))
  }

  fun resetDatabase(ioDispatcherProviderContext: CoroutineContext) {
    viewModelScope.launch(ioDispatcherProviderContext) {
      fhirEngine.clearDatabase()
      sharedPreferencesHelper.resetSharedPrefs()
      secureSharedPreference.resetSharedPrefs()
      accountAuthenticator.localLogout()
      accountAuthenticator.launchScreen(AppSettingActivity::class.java)
    }
  }
}
