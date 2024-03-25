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
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.system.exitProcess
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.extension.showToast

@AndroidEntryPoint
class LoginActivity : BaseMultiLanguageActivity() {

  @Inject lateinit var loginService: LoginService

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  @Inject lateinit var syncBroadcaster: Lazy<SyncBroadcaster>

  private val loginViewModel by viewModels<LoginViewModel>()
  private var backPressed = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    loginViewModel.fetchLoginConfigs()
    navigateToScreen()

    setContent { AppTheme { LoginScreen(loginViewModel = loginViewModel) } }

    if (
      !intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME).isNullOrBlank() &&
        loginViewModel.username.value.isNullOrBlank()
    ) {
      loginViewModel.onUsernameUpdated(intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)!!)
      this@LoginActivity.showToast(getString(R.string.auth_token_expired), Toast.LENGTH_SHORT)
    }
  }

  private fun navigateToScreen() {
    loginViewModel.apply {
      loadLastLoggedInUsername()
      navigateToHome.observe(this@LoginActivity) { isNavigate ->
        if (isNavigate) {
          val isUpdatingCurrentAccount =
            intent.hasExtra(AccountManager.KEY_ACCOUNT_NAME) &&
              intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)!!.trim() ==
                loginViewModel.username.value?.trim()

          if (isUpdatingCurrentAccount) {
            syncBroadcaster.get().runSync() // restart/resume sync
            setResult(Activity.RESULT_OK)
            finish() // Return to the previous activity
          } else {
            syncBroadcaster.get().runSync()
            loginService.activateAuthorisedFeatures()
            loginService.navigateToHome(this@LoginActivity)
          }
        }
      }
      launchDialPad.observe(this@LoginActivity) { if (!it.isNullOrEmpty()) launchDialPad(it) }
    }
  }

  fun getApplicationConfiguration(): ApplicationConfiguration {
    return configurationRegistry.getAppConfigs()
  }

  private fun launchDialPad(phone: String) {
    startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse(phone) })
  }

  override fun onBackPressed() {
    super.onBackPressed()
    if (backPressed) {
      finishAffinity()
      exitProcess(0)
    } else {
      backPressed = true
      Toast.makeText(this, getString(R.string.press_back_again), Toast.LENGTH_SHORT).show()
      Handler(Looper.getMainLooper()).postDelayed({ backPressed = false }, 3000)
    }
  }
}
