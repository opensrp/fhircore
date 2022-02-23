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

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.util.extension.extractId
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AppSettingViewModel @Inject constructor(
  val fhirResourceDataSource: FhirResourceDataSource,
  val accountAuthenticator: AccountAuthenticator,
val defaultRepository: DefaultRepository) : ViewModel() {

  val loadConfigs: MutableLiveData<Boolean?> = MutableLiveData(null)

  private val _appId = MutableLiveData("")
  val appId
    get() = _appId

  private val _username = MutableLiveData("")
  val username
    get() = _username

  private val _password = MutableLiveData("")
  val password
    get() = _password

  private val _rememberApp = MutableLiveData(false)
  val rememberApp
    get() = _rememberApp

  fun onApplicationIdChanged(appId: String) {
    _appId.value = appId
  }

  fun onUsernameChanged(username: String) {
    _username.value = username
  }

  fun onPasswordChanged(password: String) {
    _password.value = password
  }

  fun onRememberAppChecked(rememberApp: Boolean) {
    _rememberApp.value = rememberApp
  }

  private val _error = MutableLiveData("")
  val error
    get() = _error

  fun loadConfigurations(loadConfigs: Boolean, appId: String, username: String?, password: String?) {
    if (username == null && password == null)
      this@AppSettingViewModel.loadConfigs.postValue(loadConfigs)


    viewModelScope.launch(Dispatchers.Default) {
      kotlin.runCatching {
        val cPath = "${ResourceType.Composition.name}?${Composition.SP_IDENTIFIER}=$appId"
        val composition = fhirResourceDataSource.loadData(cPath).entryFirstRep.resource as Composition
        defaultRepository.save(composition)

        accountAuthenticator.fetchToken(username!!, password!!.toCharArray()).execute().body()!!.let {
          accountAuthenticator.updateTempSession(it)
        }

        composition.section.groupBy { it.focus.reference.split("/")[0] }.entries.forEach {
          val ids = it.value.map { it.focus.extractId() }.joinToString(",")
          val rPath = it.key+"?${Composition.SP_RES_ID}=$ids"
          fhirResourceDataSource.loadData(rPath).entry.forEach {
            defaultRepository.save(it.resource)
          }
        }
        this@AppSettingViewModel.loadConfigs.postValue(loadConfigs)
      }.onFailure {
        Timber.e(it)
        error.postValue("${it.message}")
      }
    }
  }
}
