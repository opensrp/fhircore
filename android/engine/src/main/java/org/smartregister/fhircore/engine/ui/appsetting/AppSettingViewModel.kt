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

package org.smartregister.fhircore.engine.ui.appsetting

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry.Companion.DEBUG_SUFFIX
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.retrieveCompositionSections
import timber.log.Timber

@HiltViewModel
class AppSettingViewModel
@Inject
constructor(
  val fhirResourceDataSource: FhirResourceDataSource,
  val defaultRepository: DefaultRepository
) : ViewModel() {

  val loadConfigs: MutableLiveData<Boolean?> = MutableLiveData(null)

  val fetchConfigs: MutableLiveData<Boolean?> = MutableLiveData(null)

  private val _appId = MutableLiveData("")
  val appId
    get() = _appId

  private val _showProgressBar = MutableLiveData(false)
  val showProgressBar
    get() = _showProgressBar

  private val _error = MutableLiveData("")
  val error: LiveData<String>
    get() = _error

  fun onApplicationIdChanged(appId: String) {
    _appId.value = appId
  }

  fun loadConfigurations(loadConfigs: Boolean) {
    this.loadConfigs.postValue(loadConfigs)
  }

  fun fetchConfigurations(fetchConfigs: Boolean) {
    this.fetchConfigs.postValue(fetchConfigs)
  }

  /**
   * Fetch the [Composition] resource whose identifier matches the provided [appId]. Save the
   * composition resource and all the nested resources referenced in the
   * [Composition.SectionComponent].
   */
  suspend fun fetchConfigurations(appId: String, context: Context) {
    runCatching {
      Timber.i("Fetching configs for app $appId")
      this._showProgressBar.postValue(true)
      val urlPath = "${ResourceType.Composition.name}?${Composition.SP_IDENTIFIER}=$appId"
      val compositionResponse =
        fhirResourceDataSource.loadData(urlPath).entryFirstRep.also {
          if (!it.hasResource()) {
            Timber.w("No response for composition resource on path $urlPath")
            _showProgressBar.postValue(false)
            _error.postValue(context.getString(R.string.application_not_supported, appId))
            return
          }
        }

      val composition = (compositionResponse.resource as Composition)
      composition
        .retrieveCompositionSections()
        .filter { it.hasFocus() && it.focus.hasReferenceElement() && it.focus.hasIdentifier() }
        .groupBy { it.focus.reference.substringBeforeLast("/") }
        .filter { it.key == ResourceType.Binary.name || it.key == ResourceType.Parameters.name }
        .forEach { entry: Map.Entry<String, List<Composition.SectionComponent>> ->
          val ids = entry.value.joinToString(",") { it.focus.extractId() }
          val resourceUrlPath = entry.key + "?${Composition.SP_RES_ID}=$ids"
          fhirResourceDataSource.loadData(resourceUrlPath).entry.forEach {
            defaultRepository.save(it.resource)
          }
        }

      // Save composition after fetching all the referenced section resources
      defaultRepository.save(composition)

      loadConfigurations(true)
      _showProgressBar.postValue(false)
    }
      .onFailure {
        Timber.w(it)
        _showProgressBar.postValue(false)
        _error.postValue("${it.message}")
      }
  }

  fun hasDebugSuffix(): Boolean = appId.value?.endsWith(DEBUG_SUFFIX, ignoreCase = true) ?: false
}
