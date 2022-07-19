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

package org.smartregister.fhircore.quest.ui.main

import android.content.Context
import androidx.navigation.NavHostController
import com.google.android.fhir.sync.State
import org.smartregister.fhircore.engine.configuration.navigation.NavigationActionConfig
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.domain.model.Language

sealed class AppMainEvent {
  data class SwitchLanguage(val language: Language, val context: Context) : AppMainEvent()
  data class DeviceToDeviceSync(val context: Context) : AppMainEvent()
  data class RegisterNewClient(val context: Context, val questionnaireId: String) : AppMainEvent()
  data class OpenRegistersBottomSheet(
    val context: Context,
    val registersList: List<NavigationMenuConfig>?
  ) : AppMainEvent()
  object Logout : AppMainEvent()
  object SyncData : AppMainEvent()
  data class UpdateSyncState(val state: State, val lastSyncTime: String?) : AppMainEvent()
  data class NavigateToScreen(
    val navController: NavHostController,
    val actions: List<NavigationActionConfig>?,
    val registerId: String
  ) : AppMainEvent()
  data class NavigateToMenu(
    val context: Context,
    val navController: NavHostController,
    val actions: List<NavigationActionConfig>?,
    val registerId: String
  ) : AppMainEvent()
}
