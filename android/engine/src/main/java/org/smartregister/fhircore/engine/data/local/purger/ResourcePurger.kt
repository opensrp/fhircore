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

package org.smartregister.fhircore.engine.data.local.purger

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.search
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.AuditEvent
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import timber.log.Timber

// TODO: Filter the status at the DB first than fetching all of the resources, we don't want to
// block the db
class ResourcePurger(private val fhirEngine: FhirEngine) {

  suspend operator fun invoke() {
    onPurge<Encounter>(ResourceType.Encounter)
    onPurge<AuditEvent>(ResourceType.AuditEvent)
    onPurge<QuestionnaireResponse>(ResourceType.QuestionnaireResponse)
    onPurgeObservation()
    onPurgeCarePlans()
    onPurgeAppointment()
  }

  private suspend fun <R : Resource> query(type: ResourceType): Flow<List<R>> = flow {
    var start = 0
    do {
      val resources =
        fhirEngine.search<R>(Search(type, count = 500, from = PAGE_COUNT * start++)).map {
          it.resource
        }
      emit(resources)
    } while (resources.isNotEmpty())
  }

  private suspend fun <R : Resource> onPurge(type: ResourceType) =
    query<R>(type).collect { it.purge() }

  private suspend fun Resource.purge() =
    try {
      fhirEngine.purge(this.resourceType, logicalId)
      Timber.tag("purge").d("Purged $resourceType with id: $logicalId")
    } catch (e: Exception) {
      Timber.tag("purge:Exception").e(e.message!!)
    }

  private suspend fun Iterable<Resource>.purge() {
    try {
      val resourceType = this.firstOrNull()?.resourceType ?: return
      val idsToPurge = this.map { it.logicalId }.toSet()
      fhirEngine.purge(resourceType, this.map { it.logicalId }.toSet())
      Timber.tag("purge").d("Purged ${idsToPurge.size} resources of type '$resourceType'")
    } catch (e: Exception) {
      Timber.tag("purge:Exception").e(e.message!!)
    }
  }

  private suspend fun onPurgeAppointment() =
    query<Appointment>(ResourceType.Appointment).collect { appointmentList ->
      appointmentList
        .filter {
          listOf(
              Appointment.AppointmentStatus.ENTEREDINERROR,
              Appointment.AppointmentStatus.CANCELLED,
              Appointment.AppointmentStatus.NOSHOW,
              Appointment.AppointmentStatus.FULFILLED,
            )
            .contains(it.status)
        }
        .purge()
    }

  private suspend fun onPurgeObservation() =
    query<Observation>(ResourceType.Observation).collect { obsList ->
      obsList
        .filter {
          listOf(
              Observation.ObservationStatus.ENTEREDINERROR,
              Observation.ObservationStatus.CANCELLED,
              Observation.ObservationStatus.FINAL,
              Observation.ObservationStatus.CORRECTED,
            )
            .contains(it.status)
        }
        .purge()
    }

  /** Purge Inactive [CarePlan] together with it's associated [Task] */
  private suspend fun onPurgeInActiveCarePlanWithTasks(carePlans: List<CarePlan>) =
    carePlans
      .filter { it.status != CarePlan.CarePlanStatus.ACTIVE }
      .also { onPurgeCarePlanWithAssociatedTask(it) }

  /** Purge Multiple [CarePlan.CarePlanStatus.ACTIVE] together with it's associated [Task] */
  private suspend fun onPurgeMultipleActiveCarePlanWithTasks(carePlans: List<CarePlan>) =
    carePlans
      .sortedBy { it.meta.lastUpdated }
      .filter { it.status == CarePlan.CarePlanStatus.ACTIVE }
      .also { if (it.size > 1) onPurgeCarePlanWithAssociatedTask(it.subList(1, it.size)) }

  // TODO: Filter out the care_plans in the query before hand
  private suspend fun onPurgeCarePlans() =
    with(fhirEngine) {
      search<Patient>(Search(ResourceType.Patient)).onEach { patient ->
        search<CarePlan> {
            filter(
              CarePlan.SUBJECT,
              { value = "${ResourceType.Patient.name}/${patient.resource.logicalId}" },
            )
            sort(CarePlan.DATE, Order.DESCENDING)
          }
          .also { searchResults ->
            if (searchResults.size > 1) {
              with(searchResults.map { it.resource }) {
                onPurgeInActiveCarePlanWithTasks(this)
                onPurgeMultipleActiveCarePlanWithTasks(this)
              }
            }
          }
      }
    }

  private suspend fun onPurgeCarePlanWithAssociatedTask(carePlans: List<CarePlan>) =
    with(carePlans) {
      onEach { carePlan ->
        carePlan.activity.onEach {
          it.outcomeReference.firstOrNull()?.let { reference ->
            val logicalId = reference.reference.substringAfter("/")
            Task().apply { id = logicalId }.purge()
          }
        }
        carePlan.setStatusEnteredInError().purge()
      }
    }

  private suspend fun CarePlan.setStatusEnteredInError(): CarePlan {
    val carePlan = setStatus(CarePlan.CarePlanStatus.ENTEREDINERROR)
    fhirEngine.update(carePlan)
    return carePlan
  }

  companion object {
    const val PAGE_COUNT = 500
  }
}
