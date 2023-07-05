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

package org.dtree.fhircore.dataclerk.ui.main

import android.content.Context
import android.content.Intent
import com.google.android.fhir.sync.SyncJobStatus
import org.smartregister.fhircore.engine.domain.model.Language

sealed class AppMainEvent(val state: SyncJobStatus) {
  data class SwitchLanguage(
    val syncState: SyncJobStatus,
    val language: Language,
    val context: Context
  ) : AppMainEvent(syncState)
  data class DeviceToDeviceSync(val syncState: SyncJobStatus, val context: Context) :
    AppMainEvent(syncState)
  data class Logout(val syncState: SyncJobStatus, val context: Context) : AppMainEvent(syncState)
  data class SyncData(val syncState: SyncJobStatus, val launchManualAuth: (Intent) -> Unit) :
    AppMainEvent(syncState)
  data class ResumeSync(val syncState: SyncJobStatus) : AppMainEvent(syncState)
  data class UpdateSyncState(val syncState: SyncJobStatus, val lastSyncTime: String?) :
    AppMainEvent(syncState)
  data class RefreshAuthToken(
    val syncState: SyncJobStatus,
    val launchManualAuth: (Intent) -> Unit
  ) : AppMainEvent(syncState)
}
