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
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.util.extension.extractId
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

  val rememberApp: MutableLiveData<Boolean?> = MutableLiveData(null)

  private val _showProgressBar = MutableLiveData(false)
  val showProgressBar
    get() = _showProgressBar

  fun onApplicationIdChanged(appId: String) {
    _appId.value = appId
  }

  fun onRememberAppChecked(rememberMe: Boolean) {
    rememberApp.postValue(rememberMe)
  }

  private val _error = MutableLiveData("")
  val error: LiveData<String>
    get() = _error

  fun loadConfigurations(loadConfigs: Boolean) {
    this.loadConfigs.postValue(loadConfigs)
  }

  fun fetchConfigurations(fetchConfigs: Boolean) {
    this.fetchConfigs.postValue(fetchConfigs)
  }

  suspend fun fetchConfigurations(appId: String, context: Context) {
    kotlin
      .runCatching {
        Timber.i("Fetching configs for app $appId")

        this._showProgressBar.postValue(true)
        val cPath = "${ResourceType.Composition.name}?${Composition.SP_IDENTIFIER}=$appId"
        val data =
          fhirResourceDataSource.loadData(cPath).entryFirstRep.also {
            if (!it.hasResource()) {
              Timber.w("No response for composition resource on path $cPath")
              _showProgressBar.postValue(false)
              _error.postValue(context.getString(R.string.application_not_supported, appId))
              return
            }
          }

        val composition = data.resource as Composition
        defaultRepository.save(composition)

        composition.section.groupBy { it.focus.reference.split("/")[0] }.entries.forEach {
          entry: Map.Entry<String, List<Composition.SectionComponent>> ->
          val ids = entry.value.joinToString(",") { it.focus.extractId() }
          val rPath = entry.key + "?${Composition.SP_RES_ID}=$ids"
          fhirResourceDataSource.loadData(rPath).entry.forEach {
            defaultRepository.save(it.resource)
          }
        }

        loadConfigurations(true)
        _showProgressBar.postValue(false)
      }
      .onFailure {
        Timber.w(it)
        _showProgressBar.postValue(false)
        _error.postValue("${it.message}")
      }
  }
}
