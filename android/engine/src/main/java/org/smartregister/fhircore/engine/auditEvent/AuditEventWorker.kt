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

package org.smartregister.fhircore.engine.auditEvent

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class AuditEventWorker
@AssistedInject
constructor(
  @Assisted val appContext: Context,
  @Assisted workerParameters: WorkerParameters,
  private val auditEventRepository: AuditEventRepository,
) : CoroutineWorker(appContext, workerParameters) {

  override suspend fun doWork(): Result {
    Timber.e("AuditEventWorker is running")
    auditEventRepository.createAuditEvent()
    return Result.success()
  }

  companion object {
    const val NAME = "AuditEventWorker"
  }
}
