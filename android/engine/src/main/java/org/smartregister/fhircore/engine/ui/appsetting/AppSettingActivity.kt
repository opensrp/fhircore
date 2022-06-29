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
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.login.LoginService
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.APP_ID_KEY
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

    val isLoggedIn =
      sharedPreferencesHelper.read(IS_LOGGED_IN, false) && accountAuthenticator.hasActiveSession()

    with(appSettingViewModel) {
      loadConfigs.observe(this@AppSettingActivity) { loadConfigs ->
        if (loadConfigs == false) {
          showToast(getString(R.string.application_not_supported, appId.value))
          return@observe
        }

        if (appId.value.isNullOrBlank()) return@observe

        val appId = appId.value!!.trimEnd()

        if (hasDebugSuffix() == true && BuildConfig.DEBUG) {
          lifecycleScope.launch(dispatcherProvider.io()) {
            configurationRegistry.loadConfigurations(appId = appId) { loadSuccessful: Boolean ->
              if (loadSuccessful) {
                sharedPreferencesHelper.write(APP_ID_KEY, appId)
                if (!isLoggedIn) {
                  accountAuthenticator.launchLoginScreen()
                } else {
                  loginService.loginActivity = this@AppSettingActivity
                  loginService.navigateToHome()
                }
                finish()
              } else {
                launch(dispatcherProvider.main()) {
                  showToast(getString(R.string.application_not_supported, appId))
                }
              }
            }
          }
          return@observe
        }

        lifecycleScope.launch(dispatcherProvider.io()) {
          configurationRegistry.loadConfigurations(appId) { loadSuccessful: Boolean ->
            if (loadSuccessful) {
              sharedPreferencesHelper.write(APP_ID_KEY, appId)
              accountAuthenticator.launchLoginScreen()
              finish()
            } else {
              launch(dispatcherProvider.main()) {
                showToast(getString(R.string.application_not_supported, appId))
              }
            }
          }
        }
      }

      fetchConfigs.observe(this@AppSettingActivity) { fetchConfigs ->
        if (fetchConfigs == false) {
          loadConfigurations(true)
          return@observe
        }

        if (hasDebugSuffix() == true && BuildConfig.DEBUG) {
          loadConfigurations(true)
          return@observe
        }

        if (appId.value.isNullOrBlank()) return@observe

        lifecycleScope.launch(dispatcherProvider.io()) {
          fetchConfigurations(appId.value!!, this@AppSettingActivity)
        }
      }

      error.observe(this@AppSettingActivity) { error ->
        if (error.isNotBlank()) showToast(getString(R.string.error_loading_config, error))
      }
    }

    val lastAppId = sharedPreferencesHelper.read(APP_ID_KEY, null)?.trimEnd()
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
            val showProgressBar by appSettingViewModel.showProgressBar.observeAsState(false)
            AppSettingScreen(
              appId = appId,
              onAppIdChanged = appSettingViewModel::onApplicationIdChanged,
              onLoadConfigurations = appSettingViewModel::fetchConfigurations,
              showProgressBar = showProgressBar
            )
          }
        }
      }
  }
}
