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

package org.smartregister.fhircore.engine.data.local.tracing

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Operation
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.search
import java.util.Date
import javax.inject.Inject
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.domain.model.TracingHistory
import org.smartregister.fhircore.engine.util.extension.referenceValue

class TracingRepository @Inject constructor(val fhirEngine: FhirEngine) {
  suspend fun getTracingHistory(patientId: String): List<TracingHistory> {
    val list =
      fhirEngine.search<ListResource> {
        filter(ListResource.SUBJECT, { value = "Patient/$patientId" })
        sort(ListResource.DATE, Order.ASCENDING)
        count = 1
        from = 0
      }

    return list.map {
      val data = getListData(it)

      TracingHistory(
        historyId = data.historyId,
        startDate = data.startDate,
        endDate = data.endDate,
        numberOfAttempts = data.numberOfAttempts,
        isActive = data.isActive
      )
    }
  }

  private suspend fun getListData(list: ListResource): TracingListData {
    val tasks = mutableListOf<Task>()
    val encounters = mutableListOf<Encounter>()
    var lastAttempt: Encounter? = null
    val reasons = mutableListOf<String>()
    var outcome = ""
    list.entry.forEach { entry ->
      try {
        val ref = entry.item
        val el = ref.reference.split("/")
        val resource = fhirEngine.get(ResourceType.fromCode(el[0]), el[1])

        if (resource is Task) {
          resource.reasonCode?.codingFirstRep?.display?.let { reasons.add(it) }
          tasks.add(resource)
        } else if (resource is Encounter) {

          if (lastAttempt == null) {
            lastAttempt = resource
          } else if (lastAttempt?.period?.start != null) {
            if (resource.period.start.after(lastAttempt?.period?.start)) {
              lastAttempt = resource
            }
          }
          encounters.add(resource)
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

    if (lastAttempt != null) {
      val obs =
        fhirEngine.search<Observation> {
          filter(Observation.ENCOUNTER, { value = lastAttempt?.referenceValue() })
          filter(
            Observation.CODE,
            {
              value =
                of(CodeableConcept(Coding("https://d-tree.org", "tracing-outcome-conducted", "")))
            },
            {
              value =
                of(CodeableConcept(Coding("https://d-tree.org", "tracing-outcome-unconducted", "")))
            },
            operation = Operation.OR
          )
        }
      obs.firstOrNull()?.let { outcome = it.code.text ?: "" }
    }
    return TracingListData(
      historyId = list.logicalId,
      startDate = list.date,
      endDate =
        if (list.status != ListResource.ListStatus.CURRENT) lastAttempt?.period?.start else null,
      numberOfAttempts = encounters.size,
      lastAttempt = lastAttempt?.period?.start,
      outcome = outcome,
      reasons = reasons,
      isActive = list.status == ListResource.ListStatus.CURRENT,
    )
  }
}

private data class TracingListData(
  val historyId: String,
  val startDate: Date,
  val endDate: Date?,
  val lastAttempt: Date?,
  val numberOfAttempts: Int,
  val outcome: String,
  val reasons: List<String>,
  val isActive: Boolean,
)
