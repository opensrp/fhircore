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
import org.smartregister.fhircore.anc.ui.pin.PinLoginActivity
import org.smartregister.fhircore.anc.ui.pin.PinSetupActivity
import org.smartregister.fhircore.engine.ui.login.LoginActivity
import org.smartregister.fhircore.engine.ui.login.LoginService

class AncLoginService @Inject constructor() : LoginService {

  override lateinit var loginActivity: LoginActivity

  /**
   * Navigate to app home page as this fun is implemented if multiple modules,
   * @param canSetPin : boolean to check whether app can navigate Home or PinSetup page
   */
  override fun navigateToHome(canSetPin: Boolean) {
    if (canSetPin) {
      navigateToPinSetup()
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

  override fun navigateToPinLogin() {
    val intent =
      Intent(loginActivity, PinLoginActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    loginActivity.run {
      startActivity(intent)
      finish()
    }
  }

  private fun navigateToPinSetup() {
    val intent =
      Intent(loginActivity, PinSetupActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    loginActivity.run {
      startActivity(intent)
      finish()
    }
  }
}
