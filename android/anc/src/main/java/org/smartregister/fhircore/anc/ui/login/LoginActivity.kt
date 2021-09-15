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
import android.os.Bundle
import org.smartregister.fhircore.anc.BuildConfig
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.engine.configuration.view.loginViewConfigurationOf
import org.smartregister.fhircore.engine.ui.login.BaseLoginActivity

class LoginActivity : BaseLoginActivity() {

  override fun navigateToHome() {
    val intent = Intent(this, SplashActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
    finish()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureViews(
      loginViewConfigurationOf(
        applicationName = getString(R.string.app_name),
        applicationVersion = BuildConfig.VERSION_NAME,
        darkMode = true
      )
    )
  }
}
