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
import com.google.android.fhir.search.search
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.MessageDefinition
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.NotificationData
import org.smartregister.fhircore.engine.domain.notification.FhirNotificationManager
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.toCoding
import java.util.Date

@HiltWorker
class FhirAncFollowUpNotifierWorker
@AssistedInject
constructor(
  @Assisted val context: Context,
  @Assisted workerParams: WorkerParameters,
  val defaultRepository: DefaultRepository,
  val dispatcherProvider: DispatcherProvider,
  val notificationManager: FhirNotificationManager,
  val gson: Gson,
) : CoroutineWorker(context, workerParams) {
  override suspend fun doWork(): Result {
    return withContext(dispatcherProvider.io()) {

      val dueAncPatientDetails = getDueAncPatientDetails()

      if(dueAncPatientDetails.isNotEmpty()) {
        val descriptionBuilder = StringBuilder()
          .appendLine("ANC Follow Up is Due for below Patients").appendLine()
          .append(dueAncPatientDetails.joinToString("\n"))

        val notificationData = NotificationData(
          title = "ANC Follow Up Remainder",
          description = descriptionBuilder.toString(),
          type = "ANC",
        )

        notificationManager.showNotification(notificationData)

        MessageDefinition().apply {
          status = Enumerations.PublicationStatus.ACTIVE
          category = MessageDefinition.MessageSignificanceCategory.NOTIFICATION
          name = gson.toJson(notificationData)
          title = notificationData.title
          description = notificationData.description
          purpose = notificationData.type
          date = Date()
        }.run {
          defaultRepository.addOrUpdate(resource = this)
        }
      }

      Result.success()
    }
  }

  private suspend fun getDueAncPatientDetails() =
    defaultRepository.fhirEngine
      .search<Task> {
        filter(
          Task.GROUP_IDENTIFIER,
          { value = of(Identifier().apply { use = Identifier.IdentifierUse.SECONDARY; value = "anc_follow_visit" }) },
        )

        filter(
          Task.STATUS,
          { value = of(Task.TaskStatus.READY.toCoding()) },
          { value = of(Task.TaskStatus.FAILED.toCoding()) },
        )
      }
      .map { it.resource }
      .filter {
        it.executionPeriod.start.before(Date()) && it.executionPeriod.end.after(Date())
      }
      .map { task ->
        val patient = defaultRepository.fhirEngine
          .get(ResourceType.Patient, task.`for`.extractId()) as Patient

        StringBuilder()
          .append(patient.name.first().text).append(" -> ")
          .appendLine(patient.telecomFirstRep.value).appendLine()
          .toString()
      }

  companion object {
    const val WORK_ID = "FhirAncFollowUpNotifierWorker"
  }
}
