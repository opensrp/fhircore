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

package org.smartregister.fhircore.engine.ui.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.get
import com.google.android.fhir.sync.SyncJob
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.configuration.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.ui.register.model.Language
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.LAST_SYNC_TIMESTAMP
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

/**
 * Subclass of [ViewModel]. This view model is responsible for updating configuration views by
 * providing [registerViewConfiguration] variable which is a [MutableLiveData] that can be observed
 * on when UI configuration changes.
 */
@HiltViewModel
class RegisterViewModel
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val syncJob: SyncJob,
  val fhirResourceDataSource: FhirResourceDataSource,
  val configurationRegistry: ConfigurationRegistry,
  val configService: ConfigService,
  val dispatcher: DispatcherProvider,
  val sharedPreferencesHelper: SharedPreferencesHelper,
) : ViewModel() {

  private val applicationConfiguration =
    configurationRegistry.retrieveConfiguration<ApplicationConfiguration>(
      AppConfigClassification.APPLICATION
    )

  private val _lastSyncTimestamp =
    MutableLiveData(sharedPreferencesHelper.read(LAST_SYNC_TIMESTAMP, ""))
  val lastSyncTimestamp
    get() = _lastSyncTimestamp

  private val _refreshRegisterData: MutableLiveData<Boolean> = MutableLiveData(false)
  val refreshRegisterData
    get() = _refreshRegisterData

  private val _filterValue = MutableLiveData<Pair<RegisterFilterType, Any?>>()
  val filterValue
    get() = _filterValue

  val languages: List<Language> by lazy { loadLanguages() }

  var selectedLanguage =
    MutableLiveData(
      sharedPreferencesHelper.read(SharedPreferencesHelper.LANG, Locale.ENGLISH.toLanguageTag())
        ?: Locale.ENGLISH.toLanguageTag()
    )

  val registerViewConfiguration: MutableLiveData<RegisterViewConfiguration> = MutableLiveData()

  fun updateViewConfigurations(registerViewConfiguration: RegisterViewConfiguration) {
    this.registerViewConfiguration.value = registerViewConfiguration
  }

  fun loadLanguages() =
    applicationConfiguration.languages.map { Language(it, Locale.forLanguageTag(it).displayName) }

  fun allowLanguageSwitching() = languages.size > 1

  /**
   * Update [_filterValue]. Null means filtering has been reset therefore data for the current page
   * will be loaded instead
   */
  fun updateFilterValue(registerFilterType: RegisterFilterType, newValue: Any?) {
    _filterValue.value = Pair(registerFilterType, newValue)
  }

  /**
   * Set [_refreshRegisterData]. Reloads the data on the register fragment when true, ignored
   * otherwise
   */
  fun setRefreshRegisterData(refreshData: Boolean) {
    _refreshRegisterData.value = refreshData
  }

  fun setLastSyncTimestamp(lastSyncTimestamp: String) {
    if (lastSyncTimestamp.isNotEmpty()) {
      sharedPreferencesHelper.write(LAST_SYNC_TIMESTAMP, lastSyncTimestamp)
    }
    _lastSyncTimestamp.value = lastSyncTimestamp
  }

  fun patientExists(barcode: String): LiveData<Result<Boolean>> {
    val result = MutableLiveData<Result<Boolean>>()
    viewModelScope.launch(dispatcher.io()) {
      try {
        fhirEngine.get<Patient>(barcode)
        result.postValue(Result.success(true))
      } catch (resourceNotFoundException: ResourceNotFoundException) {
        result.postValue(Result.failure(resourceNotFoundException))
      }
    }
    return result
  }
}
