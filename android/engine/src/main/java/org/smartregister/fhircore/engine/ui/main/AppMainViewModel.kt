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

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.domain.model.SideMenuOption
import org.smartregister.fhircore.engine.navigation.SideMenuOptionFactory
import org.smartregister.fhircore.engine.sync.SyncBroadcaster

@HiltViewModel
class AppMainViewModel
@Inject
constructor(
  val accountAuthenticator: AccountAuthenticator,
  val syncBroadcaster: SyncBroadcaster,
  val sideMenuOptionFactory: SideMenuOptionFactory
) : ViewModel() {

  fun onSideMenuEvent(event: SideMenuEvent) {
    when (event) {
      SideMenuEvent.Logout -> accountAuthenticator.logout()
      SideMenuEvent.SwitchLanguage -> TODO("Change application language")
      is SideMenuEvent.SwitchRegister -> TODO("Reload data on register fragment")
      SideMenuEvent.SyncData -> syncBroadcaster.runSync()
      SideMenuEvent.TransferData -> TODO("Transfer data via P2P")
    }
  }

  fun retrieveSideMenuOptions(): List<SideMenuOption> =
    sideMenuOptionFactory.retrieveSideMenuOptions()
}
