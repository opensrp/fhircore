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

package org.smartregister.fhircore.quest.ui.main

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.util.LAST_SYNC_TIMESTAMP
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.fetchLanguages
import org.smartregister.fhircore.engine.util.extension.refresh
import org.smartregister.fhircore.engine.util.extension.setAppLocale
import org.smartregister.fhircore.quest.navigation.SideMenuOptionFactory

@HiltViewModel
class AppMainViewModel
@Inject
constructor(
  val accountAuthenticator: AccountAuthenticator,
  val syncBroadcaster: SyncBroadcaster,
  val sideMenuOptionFactory: SideMenuOptionFactory,
  val secureSharedPreference: SecureSharedPreference,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val configurationRegistry: ConfigurationRegistry,
  val configService: ConfigService
) : ViewModel() {

  private val simpleDateFormat = SimpleDateFormat(SYNC_TIMESTAMP_OUTPUT_FORMAT, Locale.getDefault())

  var appMainUiState by mutableStateOf(appMainUiStateOf())
    private set

  var applicationConfiguration: ApplicationConfiguration
    private set

  init {
    applicationConfiguration =
      configurationRegistry.retrieveConfiguration(AppConfigClassification.APPLICATION)
    appMainUiState =
      appMainUiStateOf(
        appTitle = applicationConfiguration.applicationName,
        currentLanguage = loadCurrentLanguage(),
        username = secureSharedPreference.retrieveSessionUsername() ?: "",
        sideMenuOptions = sideMenuOptionFactory.retrieveSideMenuOptions(),
        lastSyncTime = retrieveLastSyncTimestamp() ?: "",
        languages = configurationRegistry.fetchLanguages()
      )
  }

  fun onEvent(event: AppMainEvent) {
    when (event) {
      AppMainEvent.Logout -> accountAuthenticator.logout()
      is AppMainEvent.SwitchLanguage -> {
        sharedPreferencesHelper.write(SharedPreferencesHelper.LANG, event.language.tag)
        event.context.run {
          setAppLocale(event.language.tag)
          (this as Activity).refresh()
        }
      }
      is AppMainEvent.SwitchRegister -> event.navigateToRegister()
      AppMainEvent.SyncData -> {
        syncBroadcaster.runSync()
        appMainUiState =
          appMainUiState.copy(sideMenuOptions = sideMenuOptionFactory.retrieveSideMenuOptions())
      }
      AppMainEvent.TransferData -> {} // TODO Transfer data via P2P
      is AppMainEvent.UpdateSyncState -> {
        appMainUiState = appMainUiState.copy(lastSyncTime = event.lastSyncTime ?: "")
      }
    }
  }

  private fun loadCurrentLanguage() =
    Locale.forLanguageTag(
        sharedPreferencesHelper.read(SharedPreferencesHelper.LANG, Locale.ENGLISH.toLanguageTag())
          ?: Locale.ENGLISH.toLanguageTag()
      )
      .displayName

  fun formatLastSyncTimestamp(timestamp: OffsetDateTime): String {

    val syncTimestampFormatter =
      SimpleDateFormat(SYNC_TIMESTAMP_INPUT_FORMAT, Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone(UTC)
      }
    val parse: Date? = syncTimestampFormatter.parse(timestamp.toString())
    return if (parse == null) "" else simpleDateFormat.format(parse)
  }

  fun retrieveLastSyncTimestamp(): String? = sharedPreferencesHelper.read(LAST_SYNC_TIMESTAMP, null)

  fun updateLastSyncTimestamp(timestamp: OffsetDateTime) {
    sharedPreferencesHelper.write(LAST_SYNC_TIMESTAMP, formatLastSyncTimestamp(timestamp))
  }

  companion object {
    const val SYNC_TIMESTAMP_INPUT_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    const val SYNC_TIMESTAMP_OUTPUT_FORMAT = "hh:mm aa, MMM d"
    const val UTC = "UTC"
  }
}
