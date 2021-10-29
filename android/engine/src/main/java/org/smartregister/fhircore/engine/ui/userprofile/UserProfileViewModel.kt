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

package org.smartregister.fhircore.engine.ui.userprofile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication

class UserProfileViewModel(application: Application) : AndroidViewModel(application) {

  val onLogout = MutableLiveData<Boolean?>(null)

  val configurableApplication = getApplication<Application>() as ConfigurableApplication

  fun runSync() {
    configurableApplication.syncBroadcaster.syncInitiator?.runSync()
  }

  fun logoutUser() {
    configurableApplication.authenticationService.logout()
    onLogout.postValue(true)
  }

  fun retrieveUsername(): String? =
    configurableApplication.secureSharedPreference.retrieveSessionUsername()
}
