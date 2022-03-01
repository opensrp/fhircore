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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.APP_ID_CONFIG
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.showToast

@AndroidEntryPoint
class AppSettingActivity : AppCompatActivity() {

  @Inject lateinit var accountAuthenticator: AccountAuthenticator
  @Inject lateinit var configurationRegistry: ConfigurationRegistry
  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  val appSettingViewModel: AppSettingViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    super.onCreate(savedInstanceState)
    appSettingViewModel.loadConfigs.observe(
      this,
      { loadConfigs ->
        if (loadConfigs != null && loadConfigs) {
          val applicationId = appSettingViewModel.appId.value!!
          configurationRegistry.loadAppConfigurations(
            appId = applicationId,
            accountAuthenticator = accountAuthenticator
          ) { loadSuccessful: Boolean ->
            if (loadSuccessful) {
              sharedPreferencesHelper.write(APP_ID_CONFIG, applicationId)
              finish()
            } else {
              showToast(
                getString(R.string.application_not_supported, appSettingViewModel.appId.value)
              )
            }
          }
        } else if (loadConfigs != null && !loadConfigs)
          showToast(getString(R.string.application_not_supported, appSettingViewModel.appId.value))
      }
    )

    /* will require in future enhancement
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
      appSettingViewModel.onApplicationIdChanged(it)
      appSettingViewModel.loadConfigurations(true)
    }
      ?: run {
        setContent {
          AppTheme {
            val appId by appSettingViewModel.appId.observeAsState("")
            val rememberApp by appSettingViewModel.rememberApp.observeAsState(false)
            AppSettingScreen(
              appId = appId,
              rememberApp = rememberApp ?: false,
              onAppIdChanged = appSettingViewModel::onApplicationIdChanged,
              onRememberAppChecked = appSettingViewModel::onRememberAppChecked,
              onLoadConfigurations = appSettingViewModel::loadConfigurations
            )
          }
        }
      }
  }
}
