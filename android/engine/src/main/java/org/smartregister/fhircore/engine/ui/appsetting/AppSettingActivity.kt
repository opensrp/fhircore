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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.domain.util.DataLoadState
import org.smartregister.fhircore.engine.ui.login.LoginActivity
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.extension.launchActivityWithNoBackStackHistory

@AndroidEntryPoint
class AppSettingActivity : AppCompatActivity() {
  private val appSettingViewModel: AppSettingViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    super.onCreate(savedInstanceState)
    appSettingViewModel.loadConfigurations()
    lifecycleScope.launch {
      appSettingViewModel.goToHome.collect {
        if (it == true) {
          goHome()
        }
      }
    }

    installSplashScreen().setKeepOnScreenCondition { appSettingViewModel.loadState.value == null }

    setContent {
      AppTheme {
        val state by appSettingViewModel.loadState.observeAsState(DataLoadState.Loading)

        AppSettingScreen(
          state = state ?: DataLoadState.Loading,
          goToHome = this::goHome,
          retry = appSettingViewModel::fetchRemoteConfigurations,
        )
      }
    }
  }

  private fun goHome() {
    launchActivityWithNoBackStackHistory<LoginActivity>()
  }
}
