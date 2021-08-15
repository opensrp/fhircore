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

package org.smartregister.fhircore.eir.ui.login

import android.content.Intent
import android.os.Bundle
import org.smartregister.fhircore.eir.BuildConfig
import org.smartregister.fhircore.eir.EirAuthenticationService
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.ui.patient.register.PatientRegisterActivity
import org.smartregister.fhircore.engine.auth.AuthenticationService
import org.smartregister.fhircore.engine.configuration.view.loginViewConfigurationOf
import org.smartregister.fhircore.engine.ui.login.BaseLoginActivity

class EirLoginActivity : BaseLoginActivity() {

  override val authenticationService: AuthenticationService
    get() = EirAuthenticationService(this)

  override fun navigateToHome() {
    val intent = Intent(this, PatientRegisterActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
    finish()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureViews(
      loginViewConfigurationOf(
        applicationName = getString(R.string.covax_app),
        applicationVersion = BuildConfig.VERSION_NAME
      )
    )
  }
}
