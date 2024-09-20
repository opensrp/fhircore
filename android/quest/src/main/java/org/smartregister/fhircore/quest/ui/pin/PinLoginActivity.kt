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

package org.smartregister.fhircore.quest.ui.pin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.ExperimentalMaterialApi
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.smartregister.fhircore.engine.p2p.dao.P2PReceiverTransferDao
import org.smartregister.fhircore.engine.p2p.dao.P2PSenderTransferDao
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.extension.applyWindowInsetListener
import org.smartregister.fhircore.engine.util.extension.launchActivityWithNoBackStackHistory
import org.smartregister.fhircore.quest.ui.appsetting.AppSettingActivity
import org.smartregister.fhircore.quest.ui.login.LoginActivity
import org.smartregister.fhircore.quest.ui.main.AppMainActivity
import org.smartregister.p2p.P2PLibrary

@AndroidEntryPoint
class PinLoginActivity : BaseMultiLanguageActivity() {

  @Inject lateinit var secureSharedPreference: SecureSharedPreference

  @Inject lateinit var p2pSenderTransferDao: P2PSenderTransferDao

  @Inject lateinit var p2pReceiverTransferDao: P2PReceiverTransferDao

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  val pinViewModel by viewModels<PinViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.applyWindowInsetListener()
    val pinSetup = intent.extras?.getBoolean(PIN_SETUP) ?: false
    pinViewModel.apply {
      val pinLoginActivity = this@PinLoginActivity
      setPinUiState(setupPin = pinSetup, context = pinLoginActivity)
      navigateToSettings.observe(pinLoginActivity) {
        if (it) pinLoginActivity.launchActivityWithNoBackStackHistory<AppSettingActivity>()
        finish()
      }
      navigateToHome.observe(pinLoginActivity) {
        if (it) pinLoginActivity.navigateToHome()
        finish()
      }
      launchDialPad.observe(pinLoginActivity) { if (!it.isNullOrEmpty()) launchDialPad(it) }
      navigateToLogin.observe(pinLoginActivity) {
        if (it) pinLoginActivity.launchActivityWithNoBackStackHistory<LoginActivity>()
        finish()
      }
    }
    setContent { AppTheme { PinLoginScreen(pinViewModel) } }
  }

  @OptIn(ExperimentalMaterialApi::class)
  private fun navigateToHome() {
    startActivity(Intent(this, AppMainActivity::class.java))

    lifecycleScope.launch {
      // Initialize P2P only when username is provided then launch main activity
      val username = secureSharedPreference.retrieveSessionUsername()
      if (!username.isNullOrEmpty()) {
        withContext(dispatcherProvider.main()) {
          P2PLibrary.init(
            P2PLibrary.Options(
              context = applicationContext,
              dbPassphrase = username,
              username = username,
              senderTransferDao = p2pSenderTransferDao,
              receiverTransferDao = p2pReceiverTransferDao,
            ),
          )
        }
      }
    }
  }

  private fun launchDialPad(phone: String) {
    startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse(phone) })
  }

  companion object {
    const val PIN_SETUP = "pinSetup"
  }
}
