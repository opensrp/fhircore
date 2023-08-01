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

package org.smartregister.fhircore.engine.configuration.app

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import org.hl7.fhir.r4.model.Coding
import org.smartregister.fhircore.engine.appointment.MissedFHIRAppointmentsWorker
import org.smartregister.fhircore.engine.appointment.ProposedWelcomeServiceAppointmentsWorker
import org.smartregister.fhircore.engine.sync.ResourceTag
import org.smartregister.fhircore.engine.task.FhirTaskPlanWorker
import org.smartregister.fhircore.engine.task.WelcomeServiceBackToCarePlanWorker
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid

/** An interface that provides the application configurations. */
interface ConfigService {

  /** Provide [AuthConfiguration] for the Application */
  fun provideAuthConfiguration(): AuthConfiguration

  fun schedulePlan(context: Context) {
    WorkManager.getInstance(context)
      .enqueueUniquePeriodicWork(
        FhirTaskPlanWorker.WORK_ID,
        ExistingPeriodicWorkPolicy.UPDATE,
        PeriodicWorkRequestBuilder<FhirTaskPlanWorker>(12, TimeUnit.HOURS).build()
      )
  }

  fun defineResourceTags(): List<ResourceTag>

  /**
   * Provide a list of [Coding] that represents [ResourceTag]. [Coding] can be directly appended to
   * a FHIR resource.
   */
  fun provideResourceTags(sharedPreferencesHelper: SharedPreferencesHelper): List<Coding> {
    val tags = mutableListOf<Coding>()
    defineResourceTags().forEach { strategy ->
      if (strategy.isResource.not()) {
        val id = sharedPreferencesHelper.read(strategy.type, null)
        if (id.isNullOrBlank()) {
          strategy.tag.let { tag -> tags.add(tag.copy().apply { code = "Not defined" }) }
        } else {
          strategy.tag.let { tag ->
            tags.add(tag.copy().apply { code = id.extractLogicalIdUuid() })
          }
        }
      } else {
        val ids = sharedPreferencesHelper.read<List<String>>(strategy.type)
        if (ids.isNullOrEmpty()) {
          strategy.tag.let { tag -> tags.add(tag.copy().apply { code = "Not defined" }) }
        } else {
          ids.forEach { id ->
            strategy.tag.let { tag ->
              tags.add(tag.copy().apply { code = id.extractLogicalIdUuid() })
            }
          }
        }
      }
    }

    return tags
  }

  fun unschedulePlan(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(FhirTaskPlanWorker.WORK_ID)
  }

  fun scheduleCheckForMissedAppointments(context: Context) {
    val workRequest =
      PeriodicWorkRequestBuilder<MissedFHIRAppointmentsWorker>(1, TimeUnit.DAYS).build()

    WorkManager.getInstance(context)
      .enqueueUniquePeriodicWork(
        MissedFHIRAppointmentsWorker.NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        workRequest
      )
  }

  fun scheduleWelcomeServiceAppointments(context: Context) {
    val workRequest =
      PeriodicWorkRequestBuilder<ProposedWelcomeServiceAppointmentsWorker>(1, TimeUnit.DAYS).build()

    WorkManager.getInstance(context)
      .enqueueUniquePeriodicWork(
        ProposedWelcomeServiceAppointmentsWorker.NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        workRequest
      )
  }

  fun scheduleWelcomeServiceToCarePlanForMissedAppointments(context: Context) {
    val workRequest =
      PeriodicWorkRequestBuilder<WelcomeServiceBackToCarePlanWorker>(1, TimeUnit.DAYS).build()

    WorkManager.getInstance(context)
      .enqueueUniquePeriodicWork(
        WelcomeServiceBackToCarePlanWorker.NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        workRequest
      )
  }
}
