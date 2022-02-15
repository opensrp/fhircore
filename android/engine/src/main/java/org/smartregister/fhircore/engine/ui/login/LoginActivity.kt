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
import org.smartregister.fhircore.engine.configuration.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.ConfigurableComposableView
import org.smartregister.fhircore.engine.configuration.view.LoginViewConfiguration
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.FORCE_LOGIN_VIA_USERNAME

@AndroidEntryPoint
class LoginActivity :
  BaseMultiLanguageActivity(), ConfigurableComposableView<LoginViewConfiguration> {

  @Inject lateinit var loginService: LoginService

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  private val loginViewModel by viewModels<LoginViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    loginService.loginActivity = this
    loginViewModel.apply {
      navigateToHome.observe(
        this@LoginActivity,
        {
          if (loginViewModel.loginViewConfiguration.value?.enablePin == true) {
            loginService.navigateToPinLogin(goForSetup = true)
          } else {
            loginService.navigateToHome()
          }
        }
      )
      launchDialPad.observe(this@LoginActivity, { if (!it.isNullOrEmpty()) launchDialPad(it) })
    }

    if (configurationRegistry.isAppIdInitialized()) {
      configureViews(configurationRegistry.retrieveConfiguration(AppConfigClassification.LOGIN))
    }

    // Check if Pin enabled and stored then move to Pin login
    val isPinEnabled = loginViewModel.loginViewConfiguration.value?.enablePin ?: false
    val forceLoginViaUsername =
      loginViewModel.sharedPreferences.read(FORCE_LOGIN_VIA_USERNAME, false)
    val lastPinExist = loginViewModel.accountAuthenticator.hasActivePin()
    if (isPinEnabled && lastPinExist && !forceLoginViaUsername) {
      loginViewModel.sharedPreferences.write(FORCE_LOGIN_VIA_USERNAME, false)
      loginService.navigateToPinLogin()
    }

    setContent { AppTheme { LoginScreen(loginViewModel = loginViewModel) } }
  }

  override fun configureViews(viewConfiguration: LoginViewConfiguration) {
    loginViewModel.updateViewConfigurations(viewConfiguration)
  }

  private fun launchDialPad(phone: String) {
    startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse(phone) })
  }
}
