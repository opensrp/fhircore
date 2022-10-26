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

package org.smartregister.fhircore.engine.ui.userprofile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.fetchLanguages

@HiltViewModel
class UserProfileViewModel
@Inject
constructor(
  val syncBroadcaster: SyncBroadcaster,
  val accountAuthenticator: AccountAuthenticator,
  val secureSharedPreference: SecureSharedPreference,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val configurationRegistry: ConfigurationRegistry
) : ViewModel() {

  val languages by lazy { configurationRegistry.fetchLanguages() }

  val onLogout = MutableLiveData<Boolean?>(null)

  val language = MutableLiveData<Language?>(null)

  fun runSync() {
    syncBroadcaster.runSync()
  }

  fun logoutUser() {
    onLogout.postValue(true)
    accountAuthenticator.logout()
  }

  fun retrieveUsername(): String? = secureSharedPreference.retrieveSessionUsername()

  fun allowSwitchingLanguages() = languages.size > 1

  fun loadSelectedLanguage(): String =
    Locale.forLanguageTag(
        sharedPreferencesHelper.read(SharedPreferencesHelper.LANG, Locale.UK.toLanguageTag())!!
      )
      .displayName

  fun setLanguage(language: Language) {
    sharedPreferencesHelper.write(SharedPreferencesHelper.LANG, language.tag)
    this.language.postValue(language)
  }
}
