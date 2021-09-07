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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import com.google.android.fhir.search.count
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.ui.register.model.Language
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.ui.register.model.SideMenuOption
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.runSync
import timber.log.Timber

/**
 * Subclass of [ViewModel]. This view model is responsible for updating configuration views by
 * providing [registerViewConfiguration] variable which is a [MutableLiveData] that can be observed
 * on when UI configuration changes.
 */
class RegisterViewModel(
  application: Application,
  registerViewConfiguration: RegisterViewConfiguration,
  val dispatcher: DispatcherProvider = DefaultDispatcherProvider
) : AndroidViewModel(application) {

  private val _filterValue = MutableLiveData<Pair<RegisterFilterType, Any?>>()
  val filterValue
    get() = _filterValue

  private val applicationConfiguration =
    (getApplication<Application>() as ConfigurableApplication).applicationConfiguration

  private val fhirEngine = (application as ConfigurableApplication).fhirEngine

  lateinit var languages: List<Language>

  var selectedLanguage =
    MutableLiveData(
      SharedPreferencesHelper.read(SharedPreferencesHelper.LANG, Locale.ENGLISH.toLanguageTag())
        ?: Locale.ENGLISH.toLanguageTag()
    )

  val registerViewConfiguration = MutableLiveData(registerViewConfiguration)

  fun updateViewConfigurations(registerViewConfiguration: RegisterViewConfiguration) {
    this.registerViewConfiguration.value = registerViewConfiguration
  }

  fun loadLanguages() {
    languages =
      applicationConfiguration.languages.map { Language(it, Locale.forLanguageTag(it).displayName) }
  }

  fun runSync() =
    viewModelScope.launch(dispatcher.io()) {
      try {
        getApplication<Application>().runSync()
      } catch (exception: Exception) {
        Timber.e("Error syncing data", exception)
      }
    }

  suspend fun performCount(sideMenuOption: SideMenuOption): Long {
    if (sideMenuOption.countForResource &&
        sideMenuOption.entityTypePatient &&
        sideMenuOption.showCount
    ) {
      return try {
        withContext(dispatcher.io()) {
            val count = fhirEngine.count<Patient> { sideMenuOption.searchFilterLambda }.toInt()
            Timber.d("Loaded %s clients from db", count)
            count
          }
          .toLong()
      } catch (resourceNotFoundException: ResourceNotFoundException) {
        -1
      }
    }
    return -1
  }

  /**
   * Update [_filterValue]. Null means filtering has been reset therefore data for the current page
   * will be loaded instead
   */
  fun updateFilterValue(registerFilterType: RegisterFilterType, newValue: Any?) {
    _filterValue.value = Pair(registerFilterType, newValue)
  }
}
