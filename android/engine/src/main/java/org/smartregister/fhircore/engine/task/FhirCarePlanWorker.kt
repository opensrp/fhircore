/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.task

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.search
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.util.extension.extractId

@HiltWorker
class FhirCarePlanWorker
@AssistedInject
constructor(
  @Assisted val context: Context,
  @Assisted workerParams: WorkerParameters,
  val fhirEngine: FhirEngine,
  val fhirCarePlanGenerator: FhirCarePlanGenerator
) : CoroutineWorker(context, workerParams) {
  override suspend fun doWork(): Result {
    fhirEngine
      .search<CarePlan> {
        filter(
          CarePlan.STATUS,
          { value = of(CarePlan.CarePlanStatus.REVOKED.toCode()) },
          { value = of(CarePlan.CarePlanStatus.COMPLETED.toCode()) }
        )
      }
      .forEach { carePlan ->
        var shouldCompleteCarePlan: Boolean = false
        carePlan
          .activity
          .flatMap { it.outcomeReference }
          .filter { it.reference.startsWith(ResourceType.Task.name) }
          .mapNotNull { fhirCarePlanGenerator.getTask(it.extractId()) }
          .forEach { task ->
            shouldCompleteCarePlan =
              (task.status in listOf(Task.TaskStatus.CANCELLED, Task.TaskStatus.COMPLETED))
          }

        // complete CarePlan
        if (shouldCompleteCarePlan) {
          carePlan.status = CarePlan.CarePlanStatus.COMPLETED
          fhirEngine.update(carePlan)
        }
      }
    return Result.success()
  }
}
