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

package org.smartregister.fhircore.quest.ui.pin

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.components.PIN_INPUT_MAX_THRESHOLD
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.extension.applyWindowInsetListener
import org.smartregister.fhircore.quest.ui.appsetting.AppSettingActivity
import org.smartregister.fhircore.quest.ui.login.LoginActivity
import org.smartregister.fhircore.quest.ui.login.LoginService

@AndroidEntryPoint
class PinSetupActivity : BaseMultiLanguageActivity() {

  @Inject lateinit var loginService: LoginService

  val pinViewModel by viewModels<PinViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    loginService.loginActivity = this
    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

    pinViewModel.apply {
      val pinSetupActivity = this@PinSetupActivity
      setPinUiState(isSetup = true, context = pinSetupActivity)
      navigateToHome.observe(pinSetupActivity) { loginService.navigateToHome() }
      navigateToSettings.observe(pinSetupActivity) { pinSetupActivity.moveToSettings() }
      navigateToLogin.observe(pinSetupActivity) { pinSetupActivity.moveToLoginViaUsername() }
      pin.observe(pinSetupActivity) {
        it.let { enableSetPin.postValue(it.length >= PIN_INPUT_MAX_THRESHOLD) }
      }
    }
    setContent { AppTheme { PinSetupScreen(pinViewModel) } }
    this.applyWindowInsetListener()
  }

  private fun moveToSettings() {
    startActivity(
      Intent(this, AppSettingActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addCategory(Intent.CATEGORY_LAUNCHER)
      }
    )
    finish()
  }

  private fun moveToLoginViaUsername() {
    startActivity(
      Intent(this, LoginActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addCategory(Intent.CATEGORY_LAUNCHER)
      }
    )
    finish()
  }
}
