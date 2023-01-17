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

package org.smartregister.fhircore.engine.ui.pin

import android.app.Application
import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.ui.components.PIN_INPUT_MAX_THRESHOLD
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltViewModel
class PinViewModel
@Inject
constructor(
  val sharedPreferences: SharedPreferencesHelper,
  val secureSharedPreference: SecureSharedPreference,
  val configurationRegistry: ConfigurationRegistry,
) : ViewModel() {

  private val _launchDialPad: MutableLiveData<String?> = MutableLiveData(null)
  val launchDialPad
    get() = _launchDialPad

  private val _navigateToHome = MutableLiveData<Boolean>()
  val navigateToHome: LiveData<Boolean>
    get() = _navigateToHome

  private val _navigateToLogin = MutableLiveData<Boolean>()
  val navigateToLogin: LiveData<Boolean>
    get() = _navigateToLogin

  private val _navigateToSettings = MutableLiveData<Boolean>()
  val navigateToSettings: LiveData<Boolean>
    get() = _navigateToSettings

  private val _pin = MutableLiveData<String>()
  val pin: LiveData<String>
    get() = _pin

  private val _showError = MutableLiveData(false)
  val showError
    get() = _showError

  private val _enableSetPin = MutableLiveData(false)
  val enableSetPin
    get() = _enableSetPin

  val pinUiState: MutableState<PinUiState> = mutableStateOf(PinUiState())

  val applicationConfiguration: ApplicationConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(ConfigType.Application)
  }

  fun setPinUiState(isSetup: Boolean = false, context: Context) {
    val username = secureSharedPreference.retrieveSessionUsername()
    pinUiState.value =
      PinUiState(
        appId = sharedPreferences.read(SharedPreferenceKey.APP_ID.name, "")!!,
        appName = applicationConfiguration.appTitle,
        savedPin = secureSharedPreference.retrieveSessionPin() ?: "",
        isSetupPage = isSetup,
        enterUserLoginMessage =
          if (username.isNullOrEmpty()) context.getString(R.string.enter_login_pin)
          else context.getString(R.string.enter_pin_for_user, username)
      )
  }

  fun onPinConfirmed() {
    val newPin = pin.value ?: ""

    if (newPin.length == PIN_INPUT_MAX_THRESHOLD) {
      _showError.postValue(false)
      secureSharedPreference.saveSessionPin(newPin)
      _navigateToHome.postValue(true)
    } else {
      _showError.postValue(true)
    }
  }

  fun onPinChanged(newPin: String) {
    if (newPin.length == PIN_INPUT_MAX_THRESHOLD) {
      val pinMatched = newPin == secureSharedPreference.retrieveSessionPin()
      enableSetPin.value = true
      showError.value = !pinMatched
      _pin.postValue(newPin)
      if (pinMatched && !pinUiState.value.isSetupPage) _navigateToHome.value = true
    } else {
      showError.value = false
      enableSetPin.value = false
    }
  }

  fun onMenuLoginClicked(setupPin: Boolean) {
    // reset user credentials...
    if (setupPin) {
      secureSharedPreference.run {
        deleteSessionTokens()
        deleteSessionPin()
        deleteCredentials()
      }
    } else secureSharedPreference.deleteSessionPin()
    _navigateToLogin.value = true
  }

  fun forgotPin() {
    // Todo: disable dialer action for now
    //  plus load supervisor contact from config
    _launchDialPad.value = "tel:####"
  }

  fun onMenuSettingClicked() {
    sharedPreferences.remove(SharedPreferenceKey.APP_ID.name)
    _navigateToSettings.value = true
  }
}
