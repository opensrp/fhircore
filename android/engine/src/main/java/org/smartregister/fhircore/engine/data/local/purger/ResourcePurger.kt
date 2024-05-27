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

  private suspend fun <R : Resource> query(type: ResourceType) =
    fhirEngine.search<R>(Search(type)).map { it.resource }

  private suspend fun <R : Resource> onPurge(type: ResourceType) =
    query<R>(type).onEach { it.purge() }

  private suspend fun Resource.purge() =
    try {
      fhirEngine.purge(this.resourceType, logicalId)
      Timber.tag("purge").d("Purged $resourceType with id: $logicalId")
    } catch (e: Exception) {
      Timber.tag("purge:Exception").e(e.message!!)
    }

  private suspend fun onPurgeAppointment() =
    query<Appointment>(ResourceType.Appointment)
      .filter { app ->
        listOf(
            Appointment.AppointmentStatus.ENTEREDINERROR,
            Appointment.AppointmentStatus.CANCELLED,
            Appointment.AppointmentStatus.NOSHOW,
            Appointment.AppointmentStatus.FULFILLED,
          )
          .contains(app.status)
      }
      .onEach { it.purge() }

  private suspend fun onPurgeObservation() =
    query<Observation>(ResourceType.Observation)
      .filter { app ->
        listOf(
            Observation.ObservationStatus.ENTEREDINERROR,
            Observation.ObservationStatus.CANCELLED,
            Observation.ObservationStatus.FINAL,
            Observation.ObservationStatus.CORRECTED,
          )
          .contains(app.status)
      }
      .onEach { it.purge() }

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
}
