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

package org.smartregister.fhircore.engine.appointment

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext
import org.smartregister.fhircore.engine.task.FhirResourceUtil
import org.smartregister.fhircore.engine.util.DispatcherProvider

@HiltWorker
class ProposedWelcomeServiceAppointmentsWorker
@AssistedInject
constructor(
  @Assisted val appContext: Context,
  @Assisted workerParameters: WorkerParameters,
  private val fhirResourceUtil: FhirResourceUtil,
  val dispatcherProvider: DispatcherProvider,
) : CoroutineWorker(appContext, workerParameters) {
  override suspend fun doWork(): Result {
    return withContext(dispatcherProvider.singleThread()) {
      fhirResourceUtil.handleWelcomeServiceAppointmentWorker()
      Result.success()
    }
  }

  companion object {
    const val NAME = "ProposedWelcomeServiceAppointmentsWorker"
  }
}
