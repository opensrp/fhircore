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

package org.smartregister.fhircore.quest.ui.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.ExperimentalMaterialApi
import androidx.core.os.bundleOf
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.smartregister.fhircore.engine.data.remote.shared.TokenAuthenticator
import org.smartregister.fhircore.engine.p2p.dao.P2PReceiverTransferDao
import org.smartregister.fhircore.engine.p2p.dao.P2PSenderTransferDao
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.extension.applyWindowInsetListener
import org.smartregister.fhircore.engine.util.extension.launchActivityWithNoBackStackHistory
import org.smartregister.fhircore.quest.ui.main.AppMainActivity
import org.smartregister.fhircore.quest.ui.pin.PinLoginActivity
import org.smartregister.p2p.P2PLibrary

@AndroidEntryPoint
class LoginActivity : BaseMultiLanguageActivity() {

  @Inject lateinit var secureSharedPreference: SecureSharedPreference
  @Inject lateinit var p2pSenderTransferDao: P2PSenderTransferDao
  @Inject lateinit var p2pReceiverTransferDao: P2PReceiverTransferDao
  @Inject lateinit var workManager: WorkManager
  val loginViewModel by viewModels<LoginViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    navigateToScreen()
    setContent { AppTheme { LoginScreen(loginViewModel = loginViewModel) } }
    this.applyWindowInsetListener()
  }

  private fun navigateToScreen() {
    // Cancel all background tasks when login is required to refresh token
    val cancelAllWork =
      intent.extras?.getBoolean(TokenAuthenticator.CANCEL_ALL_WORK, false) ?: false
    if (cancelAllWork) workManager.cancelAllWork()

    loginViewModel.apply {
      val loginActivity = this@LoginActivity
      val isPinEnabled = isPinEnabled()
      val hasActivePin = secureSharedPreference.retrieveSessionPin() != null

      if (isPinEnabled && hasActivePin) {
        navigateToPinLogin(launchSetup = false)
      }

      navigateToHome.observe(loginActivity) { launchHomeScreen ->
        when {
          launchHomeScreen && isPinEnabled && hasActivePin ->
            navigateToPinLogin(launchSetup = false)
          launchHomeScreen && isPinEnabled && !hasActivePin ->
            navigateToPinLogin(launchSetup = true)
          launchHomeScreen && !isPinEnabled -> loginActivity.navigateToHome()
        }
      }
      launchDialPad.observe(loginActivity) { if (!it.isNullOrEmpty()) launchDialPad(it) }
    }
  }

  @OptIn(ExperimentalMaterialApi::class)
  fun navigateToHome() {
    startActivity(Intent(this, AppMainActivity::class.java))
    // Initialize P2P after login only when username is provided then finish activity
    val username = secureSharedPreference.retrieveSessionUsername()
    if (!username.isNullOrEmpty()) {
      P2PLibrary.init(
        P2PLibrary.Options(
          context = applicationContext,
          dbPassphrase = username,
          username = username,
          senderTransferDao = p2pSenderTransferDao,
          receiverTransferDao = p2pReceiverTransferDao
        )
      )
    }
    finish()
  }

  private fun navigateToPinLogin(launchSetup: Boolean = false) {
    this.launchActivityWithNoBackStackHistory<PinLoginActivity>(
      bundle = bundleOf(Pair(PinLoginActivity.PIN_SETUP, launchSetup))
    )
  }

  private fun launchDialPad(phone: String) {
    startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse(phone) })
  }
}
