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
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.MessageDefinition
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.NotificationData
import org.smartregister.fhircore.engine.domain.notification.FhirNotificationManager
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.firstDayOfMonth
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.lastDayOfMonth
import java.util.Date

@HiltWorker
class FhirMonthlyEddNotifierWorker
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

      val eddPatientDetails = getThisMonthEddPatientDetails()

      if(eddPatientDetails.isNotEmpty()) {
        val descriptionBuilder = StringBuilder()
          .appendLine("Patients with possible EDD in this month").appendLine()
          .append(eddPatientDetails.joinToString("\n"))

        val notificationData = NotificationData(
          title = "EDD Remainder",
          description = descriptionBuilder.toString(),
          type = "PNC",
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

  private suspend fun getThisMonthEddPatientDetails(): List<String> {
    val firstDayOfMonth = Date().firstDayOfMonth()
    val lastDayOfMonth = Date().lastDayOfMonth()

    return defaultRepository.fhirEngine
      .search<Observation> {
        filter(
          Observation.CODE,
          { value = of(Coding().apply { system = "http://www.snomed.org/"; code = "129019007" }) },
        )
      }
      .map { it.resource }
      .filter {
        val edd = it.effectiveDateTimeType.value
        edd >= firstDayOfMonth && edd <= lastDayOfMonth
      }
      .map { obs ->
        val patient = defaultRepository.fhirEngine
          .get(ResourceType.Patient, obs.subject.extractId()) as Patient

        StringBuilder()
          .append(patient.name.first().text)
          .append(" (").append(patient.telecomFirstRep.value).append(") ")
          .append(" -> ")
          .appendLine(obs.effectiveDateTimeType.value.formatDate(SDF_YYYY_MM_DD))
          .toString()
      }
  }

  companion object {
    const val WORK_ID = "FhirMonthlyEddNotifierWorker"
  }
}
