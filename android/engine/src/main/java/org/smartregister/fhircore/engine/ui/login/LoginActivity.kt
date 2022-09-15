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
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.theme.AppTheme

@AndroidEntryPoint
class LoginActivity : BaseMultiLanguageActivity() {

  @Inject lateinit var loginService: LoginService

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  val loginViewModel by viewModels<LoginViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    loginService.loginActivity = this
    loginViewModel.apply {
      val isPinEnabled = loginViewModel.applicationConfiguration.loginConfig.enablePin ?: false

      // Run sync and navigate directly to home screen if session is active and pin is not enabled
      if (accountAuthenticator.hasActiveSession() && isPinEnabled) {
        loginService.navigateToPinLogin(false)
      } else if (accountAuthenticator.hasActiveSession() && !isPinEnabled) {
        loginService.navigateToHome()
      }

      navigateToHome.observe(this@LoginActivity) { launchHomeScreen ->
        when {
          launchHomeScreen && isPinEnabled && accountAuthenticator.hasActivePin() -> {
            loginService.navigateToPinLogin(false)
          }
          launchHomeScreen && isPinEnabled && !accountAuthenticator.hasActivePin() -> {
            loginService.navigateToPinLogin(true)
          }
          launchHomeScreen && !isPinEnabled -> {
            loginService.navigateToHome()
          }
        }
      }
      launchDialPad.observe(this@LoginActivity) { if (!it.isNullOrEmpty()) launchDialPad(it) }
    }

    setContent { AppTheme { LoginScreen(loginViewModel = loginViewModel) } }
  }

  private fun launchDialPad(phone: String) {
    startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse(phone) })
  }
}
