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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication

class AppSettingViewModel(application: Application) : AndroidViewModel(application) {

  val configsLoaded: MutableLiveData<Boolean?> = MutableLiveData(null)

  private val _appId = MutableLiveData("")
  val appId
    get() = _appId

  private val _rememberApp = MutableLiveData(false)
  val rememberApp
    get() = _rememberApp

  fun onApplicationIdChanged(appId: String) {
    _appId.value = appId
  }

  fun onRememberAppChecked(rememberApp: Boolean) {
    _rememberApp.value = rememberApp
  }

  fun loadConfigurations() {
    if (!this.appId.value.isNullOrEmpty()) {
      (getApplication<Application>() as ConfigurableApplication).configurationRegistry
        .loadAppConfigurations(this.appId.value!!, getApplication()) { successful ->
          configsLoaded.postValue(successful)
        }
    }
  }
}
