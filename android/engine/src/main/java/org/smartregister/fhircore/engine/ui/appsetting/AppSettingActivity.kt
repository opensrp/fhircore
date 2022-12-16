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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.ui.components.register.LoaderDialog
import org.smartregister.fhircore.engine.ui.login.LoginActivity
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.showToast

@AndroidEntryPoint
class AppSettingActivity : AppCompatActivity() {

  @Inject lateinit var accountAuthenticator: AccountAuthenticator
  @Inject lateinit var configurationRegistry: ConfigurationRegistry
  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  @Inject lateinit var dispatcherProvider: DispatcherProvider
  @Inject lateinit var libraryEvaluator: LibraryEvaluator

  val appSettingViewModel: AppSettingViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    super.onCreate(savedInstanceState)

    setContent { AppTheme { LoaderDialog(dialogMessage = stringResource(R.string.initializing)) } }

    lifecycleScope.launch(dispatcherProvider.io()) { libraryEvaluator.initialize() }

    with(appSettingViewModel) {
      val appSettingActivity = this@AppSettingActivity
      loadConfigs.observe(appSettingActivity) { loadConfigs ->
        if (loadConfigs == false) {
          showToast(getString(R.string.application_not_supported, appId.value))
          return@observe
        }

        if (appId.value.isNullOrBlank()) return@observe

        val appId = appId.value!!.trimEnd()

        if (hasDebugSuffix() && BuildConfig.DEBUG) {
          lifecycleScope.launch(dispatcherProvider.io()) {
            configurationRegistry.loadConfigurations(context = appSettingActivity, appId = appId) {
              loadSuccessful: Boolean ->
              if (loadSuccessful) {
                sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, appId)
                accountAuthenticator.launchScreen(LoginActivity::class.java)
                appSettingViewModel.showProgressBar.postValue(false)
                finish()
              } else {
                launch(dispatcherProvider.main()) {
                  showToast(getString(R.string.application_not_supported, appId))
                  appSettingViewModel.showProgressBar.postValue(false)
                }
              }
            }
          }
          return@observe
        }

        lifecycleScope.launch(dispatcherProvider.io()) {
          appSettingViewModel.showProgressBar.postValue(true)
          configurationRegistry.loadConfigurations(context = appSettingActivity, appId = appId) {
            loadSuccessful: Boolean ->
            if (loadSuccessful) {
              sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, appId)
              accountAuthenticator.launchScreen(LoginActivity::class.java)
              finish()
            } else {
              launch(dispatcherProvider.main()) {
                showToast(getString(R.string.application_not_supported, appId))
              }
            }
            appSettingViewModel.showProgressBar.postValue(false)
          }
        }
      }

      fetchConfigs.observe(appSettingActivity) { fetchConfigs ->
        if (fetchConfigs == false) {
          loadConfigurations(true)
          return@observe
        }

        if (hasDebugSuffix() && BuildConfig.DEBUG) {
          loadConfigurations(true)
          return@observe
        }

        if (appId.value.isNullOrBlank()) return@observe

        lifecycleScope.launch(dispatcherProvider.io()) {
          fetchConfigurations(appId.value!!, appSettingActivity)
        }
      }

      error.observe(appSettingActivity) { error ->
        if (!error.isNullOrEmpty()) showToast(getString(R.string.error_loading_config, error))
      }
    }

    val lastAppId = sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null)?.trimEnd()
    lastAppId?.let {
      with(appSettingViewModel) {
        onApplicationIdChanged(it)
        fetchConfigurations(!accountAuthenticator.hasActiveSession())
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
