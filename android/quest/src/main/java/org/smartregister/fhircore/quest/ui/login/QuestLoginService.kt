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

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.core.content.ContextCompat.startActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.hl7.fhir.utilities.SystemExitManager.finish
import org.smartregister.fhircore.engine.appfeature.AppFeatureManager
import org.smartregister.fhircore.engine.p2p.dao.P2PReceiverTransferDao
import org.smartregister.fhircore.engine.p2p.dao.P2PSenderTransferDao
import org.smartregister.fhircore.engine.ui.login.LoginService
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.quest.ui.main.AppMainActivity
import org.smartregister.p2p.P2PLibrary

class QuestLoginService
@Inject
constructor(
  val appFeatureManager: AppFeatureManager,
  val secureSharedPreference: SecureSharedPreference,
  val p2pSenderTransferDao: P2PSenderTransferDao,
  val p2pReceiverTransferDao: P2PReceiverTransferDao,
  @ApplicationContext val appContext: Context,
) : LoginService {

  @OptIn(ExperimentalMaterialApi::class)
  override fun navigateToHome(startingActivity: AppCompatActivity) {
    startingActivity.run {
      val homeIntent =
        Intent(this, AppMainActivity::class.java).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
      this.startActivity(homeIntent)
      this.finish()
    }
  }

  override fun activateAuthorisedFeatures() {
    appFeatureManager.loadAndActivateFeatures()

    // Initialize P2P after login only when username is provided
    val username = secureSharedPreference.retrieveSessionUsername()
    if (!username.isNullOrEmpty()) {
      P2PLibrary.init(
        P2PLibrary.Options(
          context = appContext,
          dbPassphrase = username,
          username = username,
          senderTransferDao = p2pSenderTransferDao,
          receiverTransferDao = p2pReceiverTransferDao,
        ),
      )
    }
  }
}
