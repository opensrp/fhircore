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

package org.smartregister.fhircore.engine.ui.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import org.smartregister.fhircore.engine.ui.pin.PinLoginActivity
import org.smartregister.fhircore.engine.ui.pin.PinSetupActivity

interface LoginService {

  var loginActivity: AppCompatActivity

  fun navigateToHome()

  fun navigateToPinLogin(launchSetup: Boolean = false) {
    loginActivity.run {
      startActivity(
        if (launchSetup) Intent(loginActivity, PinSetupActivity::class.java)
        else
          Intent(loginActivity, PinLoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
      )
      finish()
    }
  }
}
