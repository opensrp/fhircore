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

package org.dtree.fhircore.dataclerk.ui.main

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.sync.SyncJobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Flag
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.fetchLanguages

@HiltViewModel
class AppMainViewModel
@Inject
constructor(
  val configurationRegistry: ConfigurationRegistry,
  private val sharedPreferencesHelper: SharedPreferencesHelper,
  private val secureSharedPreference: SecureSharedPreference,
) : ViewModel() {

  val patientRegisterConfiguration: RegisterViewConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(AppConfigClassification.PATIENT_REGISTER)
  }
  private val simpleDateFormat = SimpleDateFormat(SYNC_TIMESTAMP_OUTPUT_FORMAT, Locale.getDefault())
  val appMainUiState: MutableState<AppMainUiState> = mutableStateOf(appMainUiStateOf())

  val syncSharedFlow = MutableSharedFlow<SyncJobStatus>()

  private val applicationConfiguration: ApplicationConfiguration =
    configurationRegistry.retrieveConfiguration(AppConfigClassification.APPLICATION)

  suspend fun retrieveAppMainUiState() {
    appMainUiState.value =
      appMainUiStateOf(
        appTitle = applicationConfiguration.applicationName,
        currentLanguage = loadCurrentLanguage(),
        username = secureSharedPreference.retrieveSessionUsername() ?: "",
        lastSyncTime = retrieveLastSyncTimestamp() ?: "",
        languages = configurationRegistry.fetchLanguages(),
      )
  }

  fun retrieveLastSyncTimestamp(): String? =
    sharedPreferencesHelper.read(SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name, null)

  private fun loadCurrentLanguage() =
    Locale.forLanguageTag(
        sharedPreferencesHelper.read(SharedPreferenceKey.LANG.name, Locale.UK.toLanguageTag())!!
      )
      .displayName

  fun openForm(context: Context): Intent {
    val isArtClient = patientRegisterConfiguration.appId.contains("art-client")
    val artCode =
      Coding().apply {
        code = if (isArtClient) "client-already-on-art" else "exposed-infant"
        display = if (isArtClient) "Person Already on ART" else "Exposed Infant"
      }
    return Intent(context, QuestionnaireActivity::class.java)
      .putExtras(
        QuestionnaireActivity.intentArgs(
          formName = patientRegisterConfiguration.registrationForm,
          questionnaireType = QuestionnaireType.DEFAULT,
          populationResources =
            arrayListOf(
              Flag().apply {
                code =
                  CodeableConcept().apply {
                    text = if (isArtClient) "client-already-on-art" else "exposed-infant"
                    addCoding(artCode)
                  }
              }
            ),
        )
      )
  }

  fun onEvent(event: AppMainEvent) {
    viewModelScope.launch {
      syncSharedFlow.emit(event.state)

      when (event) {
        is AppMainEvent.UpdateSyncState -> {
          when (event.state) {
            is SyncJobStatus.Finished, is SyncJobStatus.Failed -> {
              appMainUiState.value =
                appMainUiState.value.copy(
                  lastSyncTime = event.lastSyncTime ?: (retrieveLastSyncTimestamp() ?: ""),
                )
            }
            else ->
              appMainUiState.value =
                appMainUiState.value.copy(
                  lastSyncTime = event.lastSyncTime ?: appMainUiState.value.lastSyncTime
                )
          }
        }
        else -> {}
      }
    }
  }

  fun sync(syncBroadcaster: SyncBroadcaster) {
    syncBroadcaster.runSync()
  }

  fun updateLastSyncTimestamp(timestamp: OffsetDateTime) {
    sharedPreferencesHelper.write(
      SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name,
      formatLastSyncTimestamp(timestamp)
    )
  }

  fun formatLastSyncTimestamp(timestamp: OffsetDateTime): String {

    val syncTimestampFormatter =
      SimpleDateFormat(SYNC_TIMESTAMP_INPUT_FORMAT, Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
      }
    val parse: Date? = syncTimestampFormatter.parse(timestamp.toString())
    return if (parse == null) "" else simpleDateFormat.format(parse)
  }

  companion object {
    const val SYNC_TIMESTAMP_INPUT_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    const val SYNC_TIMESTAMP_OUTPUT_FORMAT = "hh:mm aa, MMM d"
  }
}
