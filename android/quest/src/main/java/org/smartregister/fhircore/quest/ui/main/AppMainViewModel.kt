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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.sync.State
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.navigation.NavigationConfiguration
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.ui.bottomsheet.RegisterBottomSheetFragment
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
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
  val registerRepository: RegisterRepository,
  val dispatcherProvider: DefaultDispatcherProvider
) : ViewModel() {

  val appMainUiState: MutableState<AppMainUiState> =
    mutableStateOf(
      appMainUiStateOf(
        navigationConfiguration =
          NavigationConfiguration(
            sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, "")!!
          )
      )
    )

  val refreshDataState: MutableState<Boolean> = mutableStateOf(false)

  private val simpleDateFormat = SimpleDateFormat(SYNC_TIMESTAMP_OUTPUT_FORMAT, Locale.getDefault())

  private val applicationConfiguration: ApplicationConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(ConfigType.Application)
  }

  private val navigationConfiguration: NavigationConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(ConfigType.Navigation)
  }

  fun retrieveAppMainUiState() {
    appMainUiState.value =
      appMainUiStateOf(
        appTitle = applicationConfiguration.appTitle,
        currentLanguage = loadCurrentLanguage(),
        username = secureSharedPreference.retrieveSessionUsername() ?: "",
        lastSyncTime = retrieveLastSyncTimestamp() ?: "",
        languages = configurationRegistry.fetchLanguages(),
        navigationConfiguration = navigationConfiguration,
        registerCountMap = retrieveRegisterCountMap()
      )
  }

  fun onEvent(event: AppMainEvent) {
    when (event) {
      AppMainEvent.Logout -> accountAuthenticator.logout()
      is AppMainEvent.SwitchLanguage -> {
        sharedPreferencesHelper.write(SharedPreferenceKey.LANG.name, event.language.tag)
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
        (event.context as AppCompatActivity).let { activity ->
          RegisterBottomSheetFragment(
              navigationMenuConfigs = event.registersList,
              registerCountMap = appMainUiState.value.registerCountMap,
              menuClickListener = {
                onEvent(
                  AppMainEvent.TriggerWorkflow(
                    context = event.context,
                    navController = event.navController,
                    actions = it.actions,
                    navMenu = it
                  )
                )
              }
            )
            .run { show(activity.supportFragmentManager, RegisterBottomSheetFragment.TAG) }
        }
      }
      is AppMainEvent.UpdateSyncState -> {
        when (event.state) {
          is State.Finished, is State.Failed -> {
            // Notify subscribers to refresh views after sync and refresh UI
            refreshDataState.value = true
            retrieveAppMainUiState()
          }
          else ->
            appMainUiState.value =
              appMainUiState.value.copy(lastSyncTime = event.lastSyncTime ?: "")
        }
      }
      is AppMainEvent.TriggerWorkflow -> {
        val navigationAction = event.actions?.find { it.trigger == ActionTrigger.ON_CLICK }

        when (navigationAction?.workflow) {
          ApplicationWorkflow.DEVICE_TO_DEVICE_SYNC -> startP2PScreen(context = event.context)
          ApplicationWorkflow.LAUNCH_SETTINGS ->
            event.navController.navigate(route = MainNavigationScreen.Settings.route)
          ApplicationWorkflow.LAUNCH_REPORT ->
            event.navController.navigate(route = MainNavigationScreen.Reports.route)
          ApplicationWorkflow.LAUNCH_REGISTER -> {
            val urlParams =
              bindArgumentsOf(
                Pair(NavigationArg.REGISTER_ID, event.navMenu.id),
                Pair(NavigationArg.SCREEN_TITLE, event.navMenu.display)
              )
            event.navController.navigate(route = MainNavigationScreen.Home.route + urlParams)
          }
          ApplicationWorkflow.LAUNCH_PROFILE ->
            // TODO bind the necessary patient profile url params
            event.navController.navigate(MainNavigationScreen.Profile.route)
          null -> return
        }
      }
    }
  }

  private fun retrieveRegisterCountMap(): Map<String, Long> {
    val countsMap = mutableStateMapOf<String, Long>()
    viewModelScope.launch(dispatcherProvider.io()) {
      with(navigationConfiguration) {
        clientRegisters.setRegisterCount(countsMap)
        bottomSheetRegisters?.registers?.setRegisterCount(countsMap)
      }
    }
    return countsMap
  }

  private suspend fun List<NavigationMenuConfig>.setRegisterCount(
    countsMap: SnapshotStateMap<String, Long>
  ) {
    // Set count for registerId against its value. Use action Id; otherwise default to menu id
    this.filter { it.showCount }.forEach { menuConfig ->
      val countAction =
        menuConfig.actions?.find { actionConfig -> actionConfig.trigger == ActionTrigger.ON_COUNT }
      if (countAction != null) {
        countsMap[countAction.id ?: menuConfig.id] =
          registerRepository.countRegisterData(menuConfig.id)
      }
    }
  }

  private fun loadCurrentLanguage() =
    Locale.forLanguageTag(
        sharedPreferencesHelper.read(SharedPreferenceKey.LANG.name, Locale.ENGLISH.toLanguageTag())
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

  fun retrieveLastSyncTimestamp(): String? =
    sharedPreferencesHelper.read(SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name, null)

  fun updateLastSyncTimestamp(timestamp: OffsetDateTime) {
    sharedPreferencesHelper.write(
      SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name,
      formatLastSyncTimestamp(timestamp)
    )
  }

  companion object {
    const val SYNC_TIMESTAMP_INPUT_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    const val SYNC_TIMESTAMP_OUTPUT_FORMAT = "hh:mm aa, MMM d"
    const val UTC = "UTC"
  }
}
