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
import android.content.Intent
import com.google.android.fhir.sync.State
import org.smartregister.fhircore.engine.domain.model.Language

sealed class AppMainEvent {
  data class SwitchLanguage(val language: Language, val context: Context) : AppMainEvent()
  data class DeviceToDeviceSync(val context: Context) : AppMainEvent()
  object Logout : AppMainEvent()
  data class SyncData(val launchManualAuth: (Intent) -> Unit) : AppMainEvent()
  object ResumeSync : AppMainEvent()
  data class UpdateSyncState(val state: State, val lastSyncTime: String?) : AppMainEvent()
  data class RefreshAuthToken(val launchManualAuth: (Intent) -> Unit) : AppMainEvent()
}
