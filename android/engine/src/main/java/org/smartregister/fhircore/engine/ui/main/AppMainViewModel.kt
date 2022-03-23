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

package org.smartregister.fhircore.engine.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.android.fhir.sync.State
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.navigation.SideMenuOptionFactory
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.util.SecureSharedPreference

@HiltViewModel
class AppMainViewModel
@Inject
constructor(
  val accountAuthenticator: AccountAuthenticator,
  val syncBroadcaster: SyncBroadcaster,
  val sideMenuOptionFactory: SideMenuOptionFactory,
  val secureSharedPreference: SecureSharedPreference
) : ViewModel() {

  var appMainUiState by mutableStateOf(AppMainUiState())
    private set

  init {
    updateUiState()
  }

  fun onEvent(event: AppMainEvent) {
    when (event) {
      AppMainEvent.Logout -> accountAuthenticator.logout()
      AppMainEvent.SwitchLanguage -> TODO("Change application language")
      is AppMainEvent.SwitchRegister -> event.navigateToRegister()
      AppMainEvent.SyncData -> syncBroadcaster.runSync()
      AppMainEvent.TransferData -> TODO("Transfer data via P2P")
      is AppMainEvent.UpdateSyncState -> handleSyncState(event.state)
    }
  }

  private fun handleSyncState(state: State) {
    updateUiState(state)
  }

  fun updateUiState(state: State? = null) {
    appMainUiState =
      appMainUiState.copy(
        sideMenuOptions = sideMenuOptionFactory.retrieveSideMenuOptions(),
        username = secureSharedPreference.retrieveSessionUsername() ?: ""
      )
  }
}
