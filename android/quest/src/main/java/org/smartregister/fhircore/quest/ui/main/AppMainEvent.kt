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

package org.smartregister.fhircore.quest.ui.main

import android.content.Context
import androidx.navigation.NavController
import com.google.android.fhir.sync.CurrentSyncJobStatus
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.Language

sealed class AppMainEvent {

  data class SwitchLanguage(val language: Language, val context: Context) : AppMainEvent()

  data class OpenRegistersBottomSheet(
    val title: String? = "",
    val navController: NavController,
    val registersList: List<NavigationMenuConfig>?,
  ) : AppMainEvent()

  data class UpdateSyncState(val state: CurrentSyncJobStatus, val lastSyncTime: String?) :
    AppMainEvent()

  data class TriggerWorkflow(val navController: NavController, val navMenu: NavigationMenuConfig) :
    AppMainEvent()

  /**
   * Event triggered to open the profile with the given [profileId] for the resource identified by
   * [resourceId].
   */
  data class OpenProfile(
    val navController: NavController,
    val profileId: String,
    val resourceId: String,
    val resourceConfig: FhirResourceConfig? = null,
  ) : AppMainEvent()

  data class SyncData(val context: Context) : AppMainEvent()
}
