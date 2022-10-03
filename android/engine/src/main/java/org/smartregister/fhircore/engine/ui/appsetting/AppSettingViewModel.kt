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
import java.nio.charset.Charset
import javax.inject.Inject
import okio.ByteString.Companion.decodeBase64
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry.Companion.DEBUG_SUFFIX
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.retrieveCompositionSections
import org.smartregister.fhircore.engine.util.extension.tryDecodeJson
import timber.log.Timber

@HiltViewModel
class AppSettingViewModel
@Inject
constructor(
  val fhirResourceDataSource: FhirResourceDataSource,
  val defaultRepository: DefaultRepository,
  val sharedPreferencesHelper: SharedPreferencesHelper
) : ViewModel() {

  val loadConfigs: MutableLiveData<Boolean?> = MutableLiveData(null)

  val showProgressBar = MutableLiveData(false)

  val fetchConfigs: MutableLiveData<Boolean?> = MutableLiveData(null)

  private val _appId = MutableLiveData("")
  val appId
    get() = _appId

  private val _error = MutableLiveData("")
  val error: LiveData<String>
    get() = _error

  fun onApplicationIdChanged(appId: String) {
    _appId.value = appId
  }

  fun loadConfigurations(loadConfigs: Boolean) {
    if (loadConfigs) showProgressBar.postValue(true)
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
      showProgressBar.postValue(true)
      val urlPath = "${ResourceType.Composition.name}?${Composition.SP_IDENTIFIER}=$appId"
      val compositionResource = fetchComposition(urlPath, context) ?: return

      val patientRelatedResourceTypes = mutableListOf<ResourceType>()
      compositionResource
        .retrieveCompositionSections()
        .filter { it.hasFocus() && it.focus.hasReferenceElement() && it.focus.hasIdentifier() }
        .groupBy { it.focus.reference.substringBeforeLast("/") }
        .filter { it.key == ResourceType.Binary.name || it.key == ResourceType.Parameters.name }
        .forEach { entry: Map.Entry<String, List<Composition.SectionComponent>> ->
          val ids = entry.value.joinToString(",") { it.focus.extractId() }
          val resourceUrlPath = entry.key + "?${Composition.SP_RES_ID}=$ids"
          fhirResourceDataSource.loadData(resourceUrlPath).entry.forEach {
            defaultRepository.create(it.resource)

            if (it.resource is Binary) {
              val binary = it.resource as Binary
              binary.data.decodeToString().decodeBase64()!!.string(Charset.defaultCharset()).let {
                val config =
                  it.tryDecodeJson<RegisterConfiguration>()
                    ?: it.tryDecodeJson<ProfileConfiguration>()

                when (config) {
                  is RegisterConfiguration ->
                    config.fhirResource.dependentResourceTypes(patientRelatedResourceTypes)
                  is ProfileConfiguration ->
                    config.fhirResource.dependentResourceTypes(patientRelatedResourceTypes)
                }
              }
            }
          }
        }

      saveSyncSharedPreferences(patientRelatedResourceTypes.toList())

      // Save composition after fetching all the referenced section resources
      defaultRepository.create(compositionResource)

      loadConfigurations(true)
      showProgressBar.postValue(false)
    }
      .onFailure {
        Timber.w(it)
        showProgressBar.postValue(false)
        _error.postValue("${it.message}")
      }
  }

  suspend fun fetchComposition(urlPath: String, context: Context): Composition? {
    return fhirResourceDataSource.loadData(urlPath).entryFirstRep.let {
      if (!it.hasResource()) {
        Timber.w("No response for composition resource on path $urlPath")
        showProgressBar.postValue(false)
        _error.postValue(context.getString(R.string.application_not_supported, appId))
        return null
      }

      it.resource as Composition
    }
  }

  fun saveSyncSharedPreferences(resourceTypes: List<ResourceType>) =
    sharedPreferencesHelper.write(
      SharedPreferenceKey.REMOTE_SYNC_RESOURCES.name,
      resourceTypes.distinctBy { it.name }
    )

  private fun FhirResourceConfig.dependentResourceTypes(target: MutableList<ResourceType>) {
    this.baseResource.dependentResourceTypes(target)
    this.relatedResources.forEach { it.dependentResourceTypes(target) }
  }

  private fun ResourceConfig.dependentResourceTypes(target: MutableList<ResourceType>) {
    target.add(ResourceType.fromCode(resource))
    relatedResources.forEach { it.dependentResourceTypes(target) }
  }

  fun hasDebugSuffix(): Boolean = appId.value?.endsWith(DEBUG_SUFFIX, ignoreCase = true) ?: false
}
