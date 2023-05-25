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
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.lastOffset
import org.smartregister.fhircore.engine.util.getLastOffset

@HiltWorker
class FhirCompleteCarePlanWorker
@AssistedInject
constructor(
  @Assisted val context: Context,
  @Assisted workerParams: WorkerParameters,
  val fhirEngine: FhirEngine,
  val fhirCarePlanGenerator: FhirCarePlanGenerator,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DispatcherProvider
) : CoroutineWorker(context, workerParams) {
  override suspend fun doWork(): Result {
    return withContext(dispatcherProvider.io()) {
      val applicationConfiguration =
        configurationRegistry.retrieveConfiguration<ApplicationConfiguration>(
          ConfigType.Application
        )
      val batchSize = applicationConfiguration.taskBackgroundWorkerBatchSize.div(BATCH_SIZE_FACTOR)

      val lastOffset =
        sharedPreferencesHelper.read(key = WORK_ID.lastOffset(), defaultValue = "0")!!.toInt()

      val carePlans = getCarePlans(batchSize = batchSize, lastOffset = lastOffset)

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

      val updatedLastOffset =
        getLastOffset(items = carePlans, lastOffset = lastOffset, batchSize = batchSize)

      sharedPreferencesHelper.write(
        key = WORK_ID.lastOffset(),
        value = updatedLastOffset.toString()
      )
      Result.success()
    }
  }

  suspend fun getCarePlans(batchSize: Int, lastOffset: Int) =
    fhirEngine.search<CarePlan> {
      filter(
        CarePlan.STATUS,
        { value = of(CarePlan.CarePlanStatus.DRAFT.toCode()) },
        { value = of(CarePlan.CarePlanStatus.ACTIVE.toCode()) },
        { value = of(CarePlan.CarePlanStatus.ONHOLD.toCode()) },
        { value = of(CarePlan.CarePlanStatus.ENTEREDINERROR.toCode()) },
        { value = of(CarePlan.CarePlanStatus.UNKNOWN.toCode()) }
      )
      count = batchSize
      from = if (lastOffset > 0) lastOffset + 1 else 0
    }

  companion object {
    const val WORK_ID = "FhirCompleteCarePlanWorker"
    const val BATCH_SIZE_FACTOR = 10
  }
}
