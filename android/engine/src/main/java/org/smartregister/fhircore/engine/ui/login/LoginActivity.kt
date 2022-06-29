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
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.view.ConfigurableComposableView
import org.smartregister.fhircore.engine.configuration.view.LoginViewConfiguration
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.FORCE_LOGIN_VIA_USERNAME
import org.smartregister.fhircore.engine.util.FORCE_LOGIN_VIA_USERNAME_FROM_PIN_SETUP

@AndroidEntryPoint
class LoginActivity :
  BaseMultiLanguageActivity(), ConfigurableComposableView<LoginViewConfiguration> {

  @Inject lateinit var loginService: LoginService

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  @Inject lateinit var syncBroadcaster: Lazy<SyncBroadcaster>

  private val loginViewModel by viewModels<LoginViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    loginService.loginActivity = this
    loginViewModel.apply {
      navigateToHome.observe(this@LoginActivity) {
        if (loginViewModel.loginViewConfiguration.value?.enablePin == true) {
          val lastPinExist = loginViewModel.accountAuthenticator.hasActivePin()
          val forceLoginViaUsernamePinSetup =
            loginViewModel.sharedPreferences.read(FORCE_LOGIN_VIA_USERNAME_FROM_PIN_SETUP, false)
          when {
            lastPinExist -> {
              goToHomeScreen(FORCE_LOGIN_VIA_USERNAME, false)
            }
            forceLoginViaUsernamePinSetup -> {
              goToHomeScreen(FORCE_LOGIN_VIA_USERNAME_FROM_PIN_SETUP, false)
            }
            else -> {
              loginService.navigateToPinLogin(goForSetup = true)
            }
          }
        } else {
          configurationRegistry.fetchNonWorkflowConfigResources()
          syncBroadcaster.get().runSync()
          loginService.navigateToHome()
        }
      }
      launchDialPad.observe(this@LoginActivity) { if (!it.isNullOrEmpty()) launchDialPad(it) }
    }

    // TODO login configurations now in app config update this
    configureViews(configurationRegistry.retrieveConfiguration(ConfigType.Application))

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

  private fun goToHomeScreen(sharedPreferencesKey: String, sharedPreferencesValue: Boolean) {
    loginViewModel.sharedPreferences.write(sharedPreferencesKey, sharedPreferencesValue)
    configurationRegistry.fetchNonWorkflowConfigResources()
    syncBroadcaster.get().runSync()
    loginService.navigateToHome()
  }

  fun getApplicationConfiguration(): ApplicationConfiguration {
    return configurationRegistry.retrieveConfiguration(ConfigType.Application)
  }

  override fun configureViews(viewConfiguration: LoginViewConfiguration) {
    loginViewModel.updateViewConfigurations(viewConfiguration)
  }

  private fun launchDialPad(phone: String) {
    startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse(phone) })
  }
}
