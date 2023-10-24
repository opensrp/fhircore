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
import javax.inject.Provider
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.ui.components.register.LoaderDialog
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.showToast

@AndroidEntryPoint
class AppSettingActivity : AppCompatActivity() {

  @Inject lateinit var accountAuthenticator: AccountAuthenticator
  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  @Inject lateinit var dispatcherProvider: DispatcherProvider
  @Inject lateinit var libraryEvaluatorProvider: Provider<LibraryEvaluator>
  private val appSettingViewModel: AppSettingViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    super.onCreate(savedInstanceState)
    val appSettingActivity = this@AppSettingActivity
    setContent { AppTheme { LoaderDialog(dialogMessage = stringResource(R.string.initializing)) } }
    lifecycleScope.launch(dispatcherProvider.io()) { libraryEvaluatorProvider.get().initialize() }
    val existingAppId =
      sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null)?.trimEnd()

    appSettingViewModel.error.observe(appSettingActivity) { error ->
      if (!error.isNullOrEmpty()) showToast(error)
    }

    // If app exists load the configs otherwise fetch from the server
    if (!existingAppId.isNullOrEmpty()) {
      appSettingViewModel.run {
        onApplicationIdChanged(existingAppId)
        loadConfigurations(appSettingActivity)
      }
    } else {
      setContent {
        AppTheme {
          val appId by appSettingViewModel.appId.observeAsState("")
          val showProgressBar by appSettingViewModel.showProgressBar.observeAsState(false)
          val error by appSettingViewModel.error.observeAsState("")
          AppSettingScreen(
            appId = appId,
            onAppIdChanged = appSettingViewModel::onApplicationIdChanged,
            fetchConfiguration = appSettingViewModel::fetchConfigurations,
            showProgressBar = showProgressBar,
            error = error
          )
        }
      }
    }
  }
}
