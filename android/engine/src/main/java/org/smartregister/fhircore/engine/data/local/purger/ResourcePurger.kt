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
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import timber.log.Timber

class ResourcePurger(private val fhirEngine: FhirEngine) {

  suspend operator fun invoke() {
    onPurge<Observation>(ResourceType.Observation)
    onPurge<Encounter>(ResourceType.Encounter)
    onPurgeCarePlans()
  }

  private suspend fun <R : Resource> onPurge(type: ResourceType) =
    fhirEngine.search<R>(Search(type)).onEach { it.resource.purge() }

  private suspend fun Resource.purge() =
    try {
      fhirEngine.purge(this.resourceType, logicalId)
      Timber.tag("purge").d("Purged $resourceType with id: $logicalId")
    } catch (e: Exception) {
      Timber.tag("purge:Exception").e(e.message!!)
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
        carePlan.purge()
      }
    }
}
