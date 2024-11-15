/*
 * Copyright 2021-2024 Ona Systems, Inc
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

@file:OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)

package org.smartregister.fhircore.quest.ui.appsetting

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.components.register.LoaderDialog
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.applyWindowInsetListener
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.quest.BuildConfig
import org.smartregister.fhircore.quest.ui.login.AccountAuthenticator

@AndroidEntryPoint
class AppSettingActivity : AppCompatActivity() {

  @Inject lateinit var accountAuthenticator: AccountAuthenticator

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  private val appSettingViewModel: AppSettingViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val appSettingActivity = this@AppSettingActivity
    appSettingActivity.applyWindowInsetListener()
    setContent {
      AppTheme {
        val error by appSettingViewModel.error.observeAsState("")
        LoaderDialog(dialogMessage = stringResource(R.string.initializing))
        if (error.isNotEmpty()) {
          showToast(error)
          finish()
        }
      }
    }
    val existingAppId =
      sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null)?.trimEnd()

    // If app exists load the configs otherwise fetch from the server
    if (!existingAppId.isNullOrEmpty()) {
      appSettingViewModel.run {
        onApplicationIdChanged(existingAppId)
        loadConfigurations(appSettingActivity)
      }
    } else if (!BuildConfig.OPENSRP_APP_ID.isNullOrEmpty()) {
      appSettingViewModel.onApplicationIdChanged(BuildConfig.OPENSRP_APP_ID)
      appSettingViewModel.fetchConfigurations(appSettingActivity)
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
            error = error,
          )
        }
      }
    }
  }
}
