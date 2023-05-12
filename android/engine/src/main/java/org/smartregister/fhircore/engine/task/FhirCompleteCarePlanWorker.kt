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
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.extractId
import timber.log.Timber

@HiltWorker
class FhirCompleteCarePlanWorker
@AssistedInject
constructor(
  @Assisted val context: Context,
  @Assisted workerParams: WorkerParameters,
  val fhirEngine: FhirEngine,
  val fhirCarePlanGenerator: FhirCarePlanGenerator,
  val sharedPreferencesHelper: SharedPreferencesHelper
) : CoroutineWorker(context, workerParams) {
  override suspend fun doWork(): Result {
    val lastOffset = sharedPreferencesHelper.read(key = SharedPreferenceKey.FHIR_COMPLETE_CAREPLAN_WORKER_LAST_OFFSET.name, defaultValue = "0")!!.toInt()
    Timber.i("Starting FhirCompleteCarePlanWorker with from : $lastOffset and batch-size : $BATCH_SIZE ++++++")
    val carePlans = fhirEngine
      .search<CarePlan> {
        filter(
          CarePlan.STATUS,
          { value = of(CarePlan.CarePlanStatus.DRAFT.toCode()) },
          { value = of(CarePlan.CarePlanStatus.ACTIVE.toCode()) },
          { value = of(CarePlan.CarePlanStatus.ONHOLD.toCode()) },
          { value = of(CarePlan.CarePlanStatus.ENTEREDINERROR.toCode()) },
          { value = of(CarePlan.CarePlanStatus.UNKNOWN.toCode()) }
        )
        count = BATCH_SIZE
        from = if (lastOffset > 0 ) lastOffset + 1 else 0
      }

    carePlans.forEach carePlanLoop@{ carePlan ->
        carePlan
          .activity
          .flatMap { it.outcomeReference }
          .filter { it.reference.startsWith(ResourceType.Task.name) }
          .mapNotNull { fhirCarePlanGenerator.getTask(it.extractId()) }
          .forEach { task ->
            if (task.status !in listOf(Task.TaskStatus.CANCELLED, Task.TaskStatus.COMPLETED))
              return@carePlanLoop
          }

        // complete CarePlan
        carePlan.status = CarePlan.CarePlanStatus.COMPLETED
        fhirEngine.update(carePlan)
      }
    Timber.i("Finishing FhirCompleteCarePlanWorker with careplan count : ${carePlans.size} ++++++")
    val updatedLastOffset = if (carePlans.isNotEmpty())  lastOffset + BATCH_SIZE else 0
    sharedPreferencesHelper.write(key = SharedPreferenceKey.FHIR_COMPLETE_CAREPLAN_WORKER_LAST_OFFSET.name, updatedLastOffset.toString())
    return Result.success()
  }

  companion object {
    const val WORK_ID = "FhirCompleteCarePlanWorker"
    const val BATCH_SIZE = 100
  }
}
