/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.pin

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Base64
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.clearPasswordInMemory
import org.smartregister.fhircore.engine.util.toPasswordHash

@HiltViewModel
class PinViewModel
@Inject
constructor(
  val sharedPreferences: SharedPreferencesHelper,
  val secureSharedPreference: SecureSharedPreference,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DispatcherProvider,
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

  private val _showError = MutableLiveData(false)
  val showError
    get() = _showError

  private val _showProgressBar = MutableLiveData(false)
  val showProgressBar
    get() = _showProgressBar

  val pinUiState: MutableState<PinUiState> =
    mutableStateOf(
      PinUiState(message = "", appName = "", setupPin = false, pinLength = 0, showLogo = false),
    )

  private val applicationConfiguration: ApplicationConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(ConfigType.Application)
  }

  fun setPinUiState(setupPin: Boolean = false, context: Context) {
    val username = secureSharedPreference.retrieveSessionUsername()
    pinUiState.value =
      PinUiState(
        appName = applicationConfiguration.appTitle,
        setupPin = setupPin,
        message =
          if (setupPin) {
            applicationConfiguration.loginConfig.pinLoginMessage
              ?: context.getString(R.string.set_pin_message)
          } else {
            context.getString(R.string.enter_pin_for_user, username)
          },
        pinLength = applicationConfiguration.loginConfig.pinLength,
        showLogo = applicationConfiguration.showLogo,
      )
  }

  fun onPinVerified(validPin: Boolean) {
    if (validPin) {
      showProgressBar(false)
      _navigateToHome.postValue(true)
    }
  }

  fun onShowPinError(showError: Boolean) {
    showProgressBar(false)
    _showError.postValue(showError)
  }

  fun onSetPin(newPin: CharArray) {
    viewModelScope.launch {
      showProgressBar(true)
      secureSharedPreference.saveSessionPin(newPin) {
        showProgressBar(false)
        _navigateToHome.postValue(true)
      }
    }
  }

  fun showProgressBar(showProgressBar: Boolean) {
    _showProgressBar.postValue(showProgressBar)
  }

  fun onMenuItemClicked(launchAppSettingScreen: Boolean) {
    secureSharedPreference.deleteSessionPin()

    if (launchAppSettingScreen) {
      secureSharedPreference.deleteCredentials()
      sharedPreferences.remove(SharedPreferenceKey.APP_ID.name)
      _navigateToSettings.value = true
    } else {
      _navigateToLogin.value = true
    }
  }

  fun forgotPin() {
    // TODO use valid supervisor (Practitioner) telephone number
    _launchDialPad.value = "tel:####"
  }

  fun pinLogin(enteredPin: CharArray, callback: (Boolean) -> Unit) {
    viewModelScope.launch {
      showProgressBar(true)
      val storedPinHash = secureSharedPreference.retrieveSessionPin()
      val salt = secureSharedPreference.retrievePinSalt()
      val generatedHash =
        async(dispatcherProvider.io()) {
            enteredPin.toPasswordHash(Base64.getDecoder().decode(salt))
          }
          .await()
      val validPin = generatedHash == storedPinHash
      if (validPin) clearPasswordInMemory(enteredPin)
      callback.invoke(validPin)
      onPinVerified(validPin)
      showProgressBar(false)
    }
  }
}
