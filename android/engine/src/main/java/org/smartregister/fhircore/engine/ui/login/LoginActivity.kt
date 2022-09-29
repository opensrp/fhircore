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

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.view.ConfigurableComposableView
import org.smartregister.fhircore.engine.configuration.view.LoginViewConfiguration
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.FORCE_LOGIN_VIA_USERNAME
import org.smartregister.fhircore.engine.util.FORCE_LOGIN_VIA_USERNAME_FROM_PIN_SETUP
import org.smartregister.fhircore.engine.util.extension.showToast

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
        val isUpdatingCurrentAccount =
          intent.hasExtra(AccountManager.KEY_ACCOUNT_NAME) &&
            intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)!!.trim() ==
              loginViewModel.username.value?.trim()

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
        } else if (isUpdatingCurrentAccount) {
          configurationRegistry.fetchNonWorkflowConfigResources()
          syncBroadcaster.get().runSync() // restart/resume sync
          setResult(Activity.RESULT_OK)
          finish() // Return to the previous activity
        } else {
          configurationRegistry.fetchNonWorkflowConfigResources()
          syncBroadcaster.get().runSync()
          loginService.navigateToHome()
        }
      }
      launchDialPad.observe(this@LoginActivity) { if (!it.isNullOrEmpty()) launchDialPad(it) }
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

    if (!intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME).isNullOrBlank() &&
        loginViewModel.username.value.isNullOrBlank()
    ) {
      loginViewModel.onUsernameUpdated(intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)!!)
      this@LoginActivity.showToast(getString(R.string.auth_token_expired), Toast.LENGTH_SHORT)
    }
  }

  private fun goToHomeScreen(sharedPreferencesKey: String, sharedPreferencesValue: Boolean) {
    loginViewModel.sharedPreferences.write(sharedPreferencesKey, sharedPreferencesValue)
    configurationRegistry.fetchNonWorkflowConfigResources()
    syncBroadcaster.get().runSync()
    loginService.navigateToHome()
  }

  fun getApplicationConfiguration(): ApplicationConfiguration {
    return configurationRegistry.retrieveConfiguration(AppConfigClassification.APPLICATION)
  }

  override fun configureViews(viewConfiguration: LoginViewConfiguration) {
    loginViewModel.updateViewConfigurations(viewConfiguration)
  }

  private fun launchDialPad(phone: String) {
    startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse(phone) })
  }
}
