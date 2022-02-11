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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.components.PIN_INPUT_MAX_THRESHOLD
import org.smartregister.fhircore.engine.util.APP_ID_CONFIG
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.FORCE_LOGIN_VIA_USERNAME
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltViewModel
class PinViewModel
@Inject
constructor(
  val dispatcher: DispatcherProvider,
  val sharedPreferences: SharedPreferencesHelper,
  val secureSharedPreference: SecureSharedPreference,
  val app: Application
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

  lateinit var savedPin: String
  lateinit var enterUserLoginMessage: String

  lateinit var appId: String
  lateinit var appName: String
  var appLogoResId: Int = R.drawable.ic_liberia

  var isSetupPage: Boolean = false

  val onBackClick = MutableLiveData(false)

  fun onAppBackClick() {
    onBackClick.value = true
  }

  fun loadData(isSetup: Boolean = false) {
    savedPin = secureSharedPreference.retrieveSessionPin() ?: ""
    isSetupPage = isSetup
    enterUserLoginMessage =
      retrieveUsername().let {
        if (it.isNullOrEmpty()) {
          app.getString(R.string.enter_login_pin)
        } else {
          app.getString(R.string.enter_pin_for_user, it)
        }
      }
    appId = retrieveAppId()
    appName = retrieveAppName()
    appLogoResId = retrieveAppLogoResId()
  }

  fun retrieveAppId(): String = sharedPreferences.read(APP_ID_CONFIG, "")!!

  fun retrieveAppName(): String = sharedPreferences.read(APP_ID_CONFIG, "")!!

  // Todo: this applicationLogoIconResource must be load from config
  fun retrieveAppLogoResId(): Int {
    return when (appId) {
      "g6pd" -> R.drawable.ic_logo_g6pd
      else -> R.drawable.ic_liberia
    }
  }

  fun retrieveUsername(): String? = secureSharedPreference.retrieveSessionUsername()

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
      val pinMatched = newPin.equals(savedPin, false)
      enableSetPin.value = true
      showError.value = !pinMatched
      _pin.postValue(newPin)
      if (pinMatched && !isSetupPage) {
        _navigateToHome.value = true
      }
    } else {
      showError.value = false
      enableSetPin.value = false
    }
  }

  fun onMenuLoginClicked() {
    sharedPreferences.write(FORCE_LOGIN_VIA_USERNAME, true)
    _navigateToLogin.value = true
  }

  fun forgotPin() {
    // Todo: disable dialer action for now
    //  plus load supervisor contact from config
    // _launchDialPad.value = "tel:####"
  }

  fun onMenuSettingClicked() {
    sharedPreferences.remove(APP_ID_CONFIG)
    _navigateToSettings.value = true
  }
}
