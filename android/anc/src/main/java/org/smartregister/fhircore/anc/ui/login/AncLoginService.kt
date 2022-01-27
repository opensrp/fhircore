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

package org.smartregister.fhircore.anc.ui.login

import android.content.Intent
import javax.inject.Inject
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.anc.ui.otp.OtpLoginActivity
import org.smartregister.fhircore.anc.ui.otp.OtpSetupActivity
import org.smartregister.fhircore.engine.ui.login.LoginActivity
import org.smartregister.fhircore.engine.ui.login.LoginService
import org.smartregister.fhircore.engine.util.FORCE_LOGIN_VIA_USERNAME
import org.smartregister.fhircore.engine.util.OTP_PIN
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

class AncLoginService @Inject constructor(val sharedPreferencesHelper: SharedPreferencesHelper) :
  LoginService {

  override lateinit var loginActivity: LoginActivity

  override fun navigateToHome(canSetOtp: Boolean) {
    // Todo: check whether to setup PIN     or moveTo Register Home
    if (canSetOtp && sharedPreferencesHelper.read(OTP_PIN, "").isNullOrEmpty()) {
      sharedPreferencesHelper.write(FORCE_LOGIN_VIA_USERNAME, "false")
      navigateToOtpSetup()
    } else {
      val intent =
        Intent(loginActivity, FamilyRegisterActivity::class.java).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      loginActivity.run {
        startActivity(intent)
        finish()
      }
    }
  }

  override fun navigateToOtpLogin() {
    val intent =
      Intent(loginActivity, OtpLoginActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    loginActivity.run {
      startActivity(intent)
      finish()
    }
  }

  private fun navigateToOtpSetup() {
    val intent =
      Intent(loginActivity, OtpSetupActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    loginActivity.run {
      startActivity(intent)
      finish()
    }
  }
}
