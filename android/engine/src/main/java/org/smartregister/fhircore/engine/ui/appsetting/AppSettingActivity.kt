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

package org.smartregister.fhircore.engine.ui.appsetting

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.login.LoginService
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.APP_ID_CONFIG
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.IS_LOGGED_IN
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.showToast

@AndroidEntryPoint
class AppSettingActivity : AppCompatActivity() {

  @Inject lateinit var accountAuthenticator: AccountAuthenticator
  @Inject lateinit var configurationRegistry: ConfigurationRegistry
  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  @Inject lateinit var dispatcherProvider: DispatcherProvider
  @Inject lateinit var loginService: LoginService

  val appSettingViewModel: AppSettingViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    super.onCreate(savedInstanceState)

    val isLoggedIn = sharedPreferencesHelper.read(IS_LOGGED_IN, false)
    appSettingViewModel.loadConfigs.observe(this) { loadConfigs ->
      if (loadConfigs == true) {
        val applicationId = appSettingViewModel.appId.value!!
        lifecycleScope.launch {
          configurationRegistry.loadConfigurations(applicationId) { loadSuccessful: Boolean ->
            if (loadSuccessful) {
              sharedPreferencesHelper.write(APP_ID_CONFIG, applicationId)
              if (!isLoggedIn) {
                accountAuthenticator.launchLoginScreen()
              } else {
                loginService.loginActivity = this@AppSettingActivity
                loginService.navigateToHome()
              }
              finish()
            } else {
              showToast(
                getString(R.string.application_not_supported, appSettingViewModel.appId.value)
              )
            }
          }
        }
      } else if (loadConfigs != null && !loadConfigs)
        showToast(getString(R.string.application_not_supported, appSettingViewModel.appId.value))
    }

    with(appSettingViewModel) {
      this.fetchConfigs.observe(this@AppSettingActivity) {
        if (it == true && appId.value?.isNotBlank() == true)
          lifecycleScope.launch(dispatcherProvider.io()) {
            fetchConfigurations(appId.value!!, this@AppSettingActivity)
          }
        else {
          loadConfigurations(true)
        }
      }
    }

    appSettingViewModel.error.observe(this) {
      if (it.isNotBlank()) showToast(getString(R.string.error_loading_config, it))
    }

    /* Todo: Enhancement remember appId by explicitly opting to via a checkbox
    appSettingViewModel.rememberApp.observe(
      this,
      { doRememberApp ->
        doRememberApp?.let {
          if (doRememberApp) {
            if (!appSettingViewModel.appId.value.isNullOrEmpty()) {
              sharedPreferencesHelper.write(APP_ID_CONFIG, appSettingViewModel.appId.value ?: "")
            }
          } else {
            sharedPreferencesHelper.remove(APP_ID_CONFIG)
          }
        }
      }
    )
    */

    val lastAppId = sharedPreferencesHelper.read(APP_ID_CONFIG, null)
    lastAppId?.let {
      with(appSettingViewModel) {
        onApplicationIdChanged(it)
        fetchConfigurations(!isLoggedIn)
      }
    }
      ?: run {
        setContent {
          AppTheme {
            val appId by appSettingViewModel.appId.observeAsState("")
            val rememberApp by appSettingViewModel.rememberApp.observeAsState(false)
            val showProgressBar by appSettingViewModel.showProgressBar.observeAsState(false)
            AppSettingScreen(
              appId = appId,
              rememberApp = rememberApp ?: false,
              onAppIdChanged = appSettingViewModel::onApplicationIdChanged,
              onRememberAppChecked = appSettingViewModel::onRememberAppChecked,
              onLoadConfigurations = appSettingViewModel::fetchConfigurations,
              showProgressBar = showProgressBar
            )
          }
        }
      }
  }
}
