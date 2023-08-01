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
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry.Companion.DEBUG_SUFFIX
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.ui.login.LoginActivity
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.engine.util.extension.launchActivityWithNoBackStackHistory
import retrofit2.HttpException
import timber.log.Timber

@HiltViewModel
class AppSettingViewModel
@Inject
constructor(
  val fhirResourceDataSource: FhirResourceDataSource,
  val defaultRepository: DefaultRepository,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DispatcherProvider
) : ViewModel() {

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

  fun loadConfigurations(context: Context) {
    viewModelScope.launch(dispatcherProvider.io()) {
      appId.value?.let { thisAppId ->
        configurationRegistry.loadConfigurations(thisAppId) {
          showProgressBar.postValue(false)
          sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, thisAppId)
          launchLoginScreen(context)
        }
      }
    }
  }

  fun fetchConfigurations(context: Context) {
    showProgressBar.postValue(true)
    val appId = appId.value
    if (!appId.isNullOrEmpty()) {
      when {
        hasDebugSuffix() -> loadConfigurations(context)
        else -> fetchRemoteConfigurations(appId, context)
      }
    }
  }

  fun fetchRemoteConfigurations(appId: String, context: Context) {
    viewModelScope.launch {
      try {
        Timber.i("Fetching configs for app $appId")

        _showProgressBar.postValue(true)
        val cPath = "${ResourceType.Composition.name}?${Composition.SP_IDENTIFIER}=$appId"
        val data =
          fhirResourceDataSource.loadData(cPath).entryFirstRep.also {
            if (!it.hasResource()) {
              Timber.w("No response for composition resource on path $cPath")
              _showProgressBar.postValue(false)
              _error.postValue(context.getString(R.string.application_not_supported, appId))
              return@launch
            }
          }

        val composition = data.resource as Composition
        defaultRepository.save(composition)

        composition
          .section
          .groupBy { it.focus.reference.split("/")[0] }
          .entries
          .filter { it.key == ResourceType.Binary.name || it.key == ResourceType.Parameters.name }
          .forEach { entry: Map.Entry<String, List<Composition.SectionComponent>> ->
            val ids = entry.value.joinToString(",") { it.focus.extractId() }
            val rPath = entry.key + "?${Composition.SP_RES_ID}=$ids"
            fhirResourceDataSource.loadData(rPath).entry.forEach {
              defaultRepository.save(it.resource)
            }
          }

        loadConfigurations(context)
        _showProgressBar.postValue(false)
      } catch (unknownHostException: UnknownHostException) {
        _error.postValue(context.getString(R.string.error_loading_config_no_internet))
        showProgressBar.postValue(false)
      } catch (httpException: HttpException) {
        if ((400..503).contains(httpException.response()!!.code()))
          _error.postValue(context.getString(R.string.error_loading_config_general))
        else _error.postValue(context.getString(R.string.error_loading_config_http_error))
        showProgressBar.postValue(false)
      }
    }
  }

  fun hasDebugSuffix(): Boolean =
    appId.value?.endsWith(DEBUG_SUFFIX, ignoreCase = true) == true && BuildConfig.DEBUG

  fun launchLoginScreen(context: Context) {
    context.getActivity()?.launchActivityWithNoBackStackHistory<LoginActivity>()
  }
}
