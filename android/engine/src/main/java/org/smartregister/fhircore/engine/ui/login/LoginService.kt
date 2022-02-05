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

  var runningActivity: AppCompatActivity

  fun navigateToHome(canSetPin: Boolean = false)

  fun navigateToPinLogin() {
    val intent =
      Intent(runningActivity, PinLoginActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    runningActivity.run {
      startActivity(intent)
      finish()
    }
  }

  fun navigateToPinSetup() {
    val intent =
      Intent(runningActivity, PinSetupActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    runningActivity.run {
      startActivity(intent)
      finish()
    }
  }
}
