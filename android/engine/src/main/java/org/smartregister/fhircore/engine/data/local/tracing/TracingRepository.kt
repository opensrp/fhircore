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
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Operation
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.search
import java.util.Date
import javax.inject.Inject
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.domain.model.TracingAttempt
import org.smartregister.fhircore.engine.domain.model.TracingHistory
import org.smartregister.fhircore.engine.domain.model.TracingOutcome
import org.smartregister.fhircore.engine.domain.model.TracingOutcomeDetails
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.extension.referenceValue

class TracingRepository @Inject constructor(val fhirEngine: FhirEngine) {
  suspend fun getTracingHistory(
    currentPage: Int,
    loadAll: Boolean,
    patientId: String
  ): List<TracingHistory> {
    val list =
      fhirEngine.search<ListResource> {
        filter(ListResource.SUBJECT, { value = "Patient/$patientId" })
        sort(ListResource.DATE, Order.ASCENDING)
        count = PaginationConstant.DEFAULT_PAGE_SIZE
        from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
      }

    return list.map {
      val data = getTracingHistoryFromList(it)

      TracingHistory(
        historyId = data.historyId,
        startDate = data.startDate,
        endDate = data.endDate,
        numberOfAttempts = data.numberOfAttempts,
        isActive = data.isActive
      )
    }
  }

  suspend fun getTracingOutcomes(currentPage: Int, historyId: String): List<TracingOutcome> {
    val list = fhirEngine.get<ListResource>(historyId)
    var encounters = mutableListOf<Encounter>()

    var task: Task? = null

    list.entry.forEach { entry ->
      val ref = entry.item
      val el = ref.reference.split("/")
      val resource = fhirEngine.get(ResourceType.fromCode(el[0]), el[1])

      if (resource is Task && task == null) {
        task = resource
      } else if (resource is Encounter) {
        encounters.add(resource)
      }
    }

    encounters =
      encounters.run {
        val pageSize = PaginationConstant.DEFAULT_PAGE_SIZE
        sortBy { it.period.start }
        val fromIndex: Int = ((currentPage + 1) - 1) * pageSize
        if (size <= fromIndex) {
          mutableListOf()
        } else subList(fromIndex, (fromIndex + pageSize).coerceAtMost(this.size))
      }

    var phoneTracingCounter = 1
    var homeTracingCounter = 1
    return encounters.map { encounter ->
      val code = task?.meta?.tagFirstRep?.code
      val title: String
      if (code == "phone-tracing") {
        title = "Phone Tracing Outcome $phoneTracingCounter"
        phoneTracingCounter++
      } else {
        title = "Home Tracing Outcome $homeTracingCounter"
        homeTracingCounter++
      }
      TracingOutcome(
        historyId = historyId,
        encounterId = encounter.logicalId,
        title = title,
        date = encounter.period.start,
      )
    }
  }

  suspend fun getHistoryDetails(historyId: String, encounterId: String): TracingOutcomeDetails {
    val list = fhirEngine.get<ListResource>(historyId)
    val encounter = fhirEngine.get<Encounter>(encounterId)
    val reasons: MutableList<String> = mutableListOf()
    var outcome = ""
    var appointmentDate: Date? = null
    var conducted = true

    list.entry.forEach { entry ->
      try {
        val ref = entry.item
        val el = ref.reference.split("/")
        if (el[0] == ResourceType.Task.toString()) {
          val resource = fhirEngine.get(ResourceType.fromCode(el[0]), el[1])

          if (resource is Task) {
            resource.reasonCode?.codingFirstRep?.display?.let { reasons.add(it) }
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

    val outcomeObs =
      fhirEngine.search<Observation> {
        filter(Observation.ENCOUNTER, { value = encounter.referenceValue() })
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
    outcomeObs.firstOrNull()?.let {
      conducted = it.code.codingFirstRep.code == "tracing-outcome-conducted"
      if (it.hasValueCodeableConcept()) {
        outcome = it.valueCodeableConcept.text
      }
    }

    val dateObs =
      fhirEngine.search<Observation> {
        filter(Observation.ENCOUNTER, { value = encounter.referenceValue() })
        filter(
          Observation.CODE,
          {
            value =
              of(
                CodeableConcept(
                  Coding(
                    "https://d-tree.org",
                    "phone-tracing-outcome-date-of-agreed-appointment",
                    ""
                  )
                )
              )
          },
          {
            value =
              of(
                CodeableConcept(
                  Coding(
                    "https://d-tree.org",
                    "home-tracing-outcome-date-of-agreed-appointment",
                    ""
                  )
                )
              )
          },
          operation = Operation.OR
        )
      }
    dateObs.firstOrNull()?.let {
      if (it.hasValueDateTimeType()) {
        appointmentDate = it.valueDateTimeType.value
      }
    }
    return TracingOutcomeDetails(
      title = "",
      date = encounter.period.start,
      dateOfAppointment = appointmentDate,
      reasons = reasons,
      outcome = outcome,
      conducted = conducted,
    )
  }

  suspend fun getPatientListResource(patient: Patient): ListResource? {
    return fhirEngine
      .search<ListResource> {
        filter(ListResource.SUBJECT, { value = patient.referenceValue() })
        filter(ListResource.STATUS, { value = of(ListResource.ListStatus.CURRENT.toCode()) })
        sort(ListResource.TITLE, Order.DESCENDING)
        count = 1
        from = 0
      }
      .firstOrNull()
  }

  suspend fun getTracingAttempt(patient: Patient): TracingAttempt {
    val list = getPatientListResource(patient)
    return list?.let { toTracingAttempt(it) }
      ?: TracingAttempt(
        historyId = null,
        lastAttempt = null,
        numberOfAttempts = 0,
        outcome = "",
        reasons = listOf()
      )
  }

  suspend fun getTracingAttempt(list: ListResource?): TracingAttempt {
    return list?.let { toTracingAttempt(it) }
      ?: TracingAttempt(
        historyId = null,
        lastAttempt = null,
        numberOfAttempts = 0,
        outcome = "",
        reasons = listOf()
      )
  }

  private suspend fun toTracingAttempt(list: ListResource): TracingAttempt {
    var lastAttempt: Encounter? = null
    list
      .entry
      .map { it.item }
      .filter { it.referenceElement.resourceType == ResourceType.Encounter.name }
      .forEach { ref ->
        val resourceId = IdType(ref.reference).idPart
        kotlin
          .runCatching { fhirEngine.get<Encounter>(resourceId) }
          .onSuccess { enc ->
            lastAttempt = maxOf(lastAttempt, enc, nullsFirst(compareBy { it.period?.start?.time }))
          }
          .onFailure { it.printStackTrace() }
      }

    val obs =
      lastAttempt?.let {
        fhirEngine.search<Observation> {
          filter(Observation.ENCOUNTER, { value = it.referenceValue() })
          filter(
            Observation.CODE,
            {
              value =
                of(
                  CodeableConcept(
                    Coding().apply {
                      system = "https://d-tree.org"
                      code = "tracing-outcome-conducted"
                    }
                  )
                )
            },
            {
              value =
                of(
                  CodeableConcept(
                    Coding().apply {
                      system = "https://d-tree.org"
                      code = "tracing-outcome-unconducted"
                    }
                  )
                )
            },
            operation = Operation.OR
          )
          count = 1
        }
      }

    val outcome =
      obs?.firstOrNull { it.hasValueCodeableConcept() }?.valueCodeableConcept?.text ?: ""

    val attempts =
      list.orderedBy.coding.filter { coding -> coding.code.all { it.isDigit() } }.maxOfOrNull {
        it.code.toInt()
      }
        ?: list.title.substringAfterLast("_").toIntOrNull() ?: 0

    return TracingAttempt(
      numberOfAttempts = attempts,
      lastAttempt = lastAttempt?.period?.start,
      outcome = outcome,
      reasons = listOf(),
      historyId = list.logicalId
    )
  }

  private suspend fun getTracingHistoryFromList(list: ListResource): TracingHistory {
    val tasks = mutableListOf<Task>()
    val encounters = mutableListOf<Encounter>()
    var lastAttempt: Encounter? = null
    val reasons = mutableListOf<String>()

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

    return TracingHistory(
      historyId = list.logicalId,
      startDate = list.date,
      endDate =
        if (list.status != ListResource.ListStatus.CURRENT) lastAttempt?.period?.start else null,
      numberOfAttempts = encounters.size,
      isActive = list.status == ListResource.ListStatus.CURRENT,
    )
  }
}
