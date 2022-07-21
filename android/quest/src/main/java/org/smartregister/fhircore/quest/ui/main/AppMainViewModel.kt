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
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.android.fhir.sync.State
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.navigation.NavigationConfiguration
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.configuration.workflow.WorkflowTrigger
import org.smartregister.fhircore.engine.navigation.NavigationBottomSheet
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.APP_ID_KEY
import org.smartregister.fhircore.engine.util.LAST_SYNC_TIMESTAMP
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.fetchLanguages
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaire
import org.smartregister.fhircore.engine.util.extension.refresh
import org.smartregister.fhircore.engine.util.extension.setAppLocale
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.navigation.NavigationArg.bindArgumentsOf
import org.smartregister.p2p.utils.startP2PScreen

@HiltViewModel
class AppMainViewModel
@Inject
constructor(
  val accountAuthenticator: AccountAuthenticator,
  val syncBroadcaster: SyncBroadcaster,
  val secureSharedPreference: SecureSharedPreference,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val configurationRegistry: ConfigurationRegistry,
  val configService: ConfigService,
  val patientRegisterRepository: PatientRegisterRepository
) : ViewModel() {

  val appMainUiState: MutableState<AppMainUiState> =
    mutableStateOf(
      appMainUiStateOf(
        navigationConfiguration =
          NavigationConfiguration(sharedPreferencesHelper.read(APP_ID_KEY) ?: "")
      )
    )

  //TODO create State

  val refreshDataState: MutableState<Boolean> = mutableStateOf(false)

  private val simpleDateFormat = SimpleDateFormat(SYNC_TIMESTAMP_OUTPUT_FORMAT, Locale.getDefault())

  private val applicationConfiguration: ApplicationConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(ConfigType.Application)
  }

  fun retrieveAppMainUiState() {
    appMainUiState.value =
      appMainUiStateOf(
        appTitle = applicationConfiguration.appTitle,
        currentLanguage = loadCurrentLanguage(),
        username = secureSharedPreference.retrieveSessionUsername() ?: "",
        lastSyncTime = retrieveLastSyncTimestamp() ?: "",
        languages = configurationRegistry.fetchLanguages(),
        navigationConfiguration = configurationRegistry.retrieveConfiguration(ConfigType.Navigation),
        //TODO set value for registerCountMap = map from countfunction()
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
      AppMainEvent.SyncData -> {
        syncBroadcaster.runSync()
        retrieveAppMainUiState()
      }
      is AppMainEvent.RegisterNewClient -> {
        event.context.launchQuestionnaire<QuestionnaireActivity>(
          questionnaireId = event.questionnaireId
        )
      }
      is AppMainEvent.OpenRegistersBottomSheet -> {
        event.context.run {
          (this as AppCompatActivity).let { activity ->
            val navigationBottomSheet =
              NavigationBottomSheet(registersList = event.registersList) {}
            navigationBottomSheet.show(activity.supportFragmentManager, NavigationBottomSheet.TAG)
          }
        }
      }
      is AppMainEvent.DeviceToDeviceSync -> startP2PScreen(context = event.context)
      is AppMainEvent.UpdateSyncState -> {
        when (event.state) {
          is State.Finished, is State.Failed -> {
            // Notify subscribers to refresh views after sync and refresh UI
            refreshDataState.value = true
            retrieveAppMainUiState()
            calculateRegisterCounts()

          }
          else ->
            appMainUiState.value =
              appMainUiState.value.copy(lastSyncTime = event.lastSyncTime ?: "")
        }
      }
      is AppMainEvent.NavigateToScreen -> {
        val navigationAction =
          event.actions?.find {
            it.trigger == WorkflowTrigger.ON_CLICK &&
              it.workflow == ApplicationWorkflow.LAUNCH_REGISTER
          }

        val urlParams = bindArgumentsOf(Pair(NavigationArg.REGISTER_ID, event.registerId))
        event.navController.navigate(route = MainNavigationScreen.Home.route + urlParams)
      }
      is AppMainEvent.NavigateToMenu -> {
        /** Collecting the navigation action where the trigger is of type 'ON_CLICK' */
        val navigationAction = event.actions?.find { it.trigger == WorkflowTrigger.ON_CLICK }
        /** Navigating to appropriate screen based on the navigationAction's Workflow */
        when (navigationAction?.workflow) {
          ApplicationWorkflow.DEVICE_TO_DEVICE_SYNC -> startP2PScreen(context = event.context)
          ApplicationWorkflow.LAUNCH_SETTINGS ->
            event.navController.navigate(route = MainNavigationScreen.Settings.route)
          ApplicationWorkflow.LAUNCH_REPORT ->
            event.navController.navigate(route = MainNavigationScreen.Reports.route)
          else -> event.navController.navigate(route = MainNavigationScreen.Home.route)
        }
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

  fun calculateRegisterCounts() {
    //TODO iterate through the menus in the NavigationConfiguration to get the ids for all type of regs
    // for each register calculate the counts by calling the patientRegisterRepository.countRegisterData(registerId)
    // populate the regiserCountMap with registerId as key and register count as the value

    // patientRegisterRepository.countRegisterData(registerId) }
  }

  companion object {
    const val SYNC_TIMESTAMP_INPUT_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    const val SYNC_TIMESTAMP_OUTPUT_FORMAT = "hh:mm aa, MMM d"
    const val UTC = "UTC"
  }
}
