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

package org.smartregister.fhircore.engine.sync

import com.google.android.fhir.sync.SyncJobStatus

/**
 * An interface the exposes a callback method [onSync] which accepts an application level FHIR
 * [SyncJobStatus].
 */
interface OnSyncListener {
  /** Callback method invoked to handle sync [SyncJobStatus] */
  fun onSync(syncJobStatus: SyncJobStatus)
}