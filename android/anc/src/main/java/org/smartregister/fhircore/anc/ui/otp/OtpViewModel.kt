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

package org.smartregister.fhircore.anc.ui.otp

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.FORCE_LOGIN_VIA_USERNAME
import org.smartregister.fhircore.engine.util.OTP_PIN
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltViewModel
class OtpViewModel
@Inject
constructor(
  val dispatcher: DispatcherProvider,
  private val sharedPreferences: SharedPreferencesHelper,
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

  private val _pin = MutableLiveData<String>()
  val pin: LiveData<String>
    get() = _pin

  lateinit var savedOtp: String

  private val _loginError = MutableLiveData<String>()
  val loginError: LiveData<String>
    get() = _loginError

  private val _showError = MutableLiveData(false)
  val showError
    get() = _showError

  private val _enableSetPin = MutableLiveData(false)
  val enableSetPin
    get() = _enableSetPin

  fun loadData() {
    sharedPreferences.write(FORCE_LOGIN_VIA_USERNAME, "false")
    savedOtp = sharedPreferences.read(OTP_PIN, "").toString()
  }

  fun onPinConfirmed() {
    val newPin = pin.value ?: ""
    Log.e("aw", "pin Confirmed " + newPin)

    if (newPin.length == 4) {
      _showError.postValue(false)
      sharedPreferences.write(OTP_PIN, newPin)
      _navigateToHome.postValue(true)
    } else {
      _showError.postValue(true)
    }
  }

  fun onPinChanged(newPin: String) {
    //    _pin.postValue(newPin)
    //    //    enableSetPin.value = newPin.length>3
    //    _enableSetPin.postValue(newPin.length > 3)
    //    Log.e("aw", "pin changed " + newPin)

    if (newPin.length == 4) {
      val pinMatched = newPin.equals(savedOtp, false)
      showError.value = !pinMatched
      _pin.postValue(newPin)
      if (pinMatched) {
        _navigateToHome.value = true
      }
      Log.e("aw", "pin changed " + newPin)
    } else {
      showError.value = false
    }
  }

  fun onMenuLoginClicked() {
    Log.e("aw", "onMenuLoginClicked")
    sharedPreferences.write(FORCE_LOGIN_VIA_USERNAME, "true")
    _navigateToLogin.value = true
  }

  fun forgotPin() {
    // load supervisor contact e.g.
    _launchDialPad.value = "tel:0123456789"
  }
}
