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

package org.smartregister.fhircore.engine.task

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import com.google.android.fhir.search.search
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Encounter.EncounterStatus
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.PlanDefinition
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StructureMap
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.hl7.fhir.r4.utils.StructureMapUtilities
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices
import timber.log.Timber

@Singleton
class FhirCarePlanGenerator
@Inject
constructor(val fhirEngine: FhirEngine, val transformSupportServices: TransformSupportServices) {
  val structureMapUtilities by lazy {
    StructureMapUtilities(transformSupportServices.simpleWorkerContext, transformSupportServices)
  }
  val fhirPathEngine = FHIRPathEngine(transformSupportServices.simpleWorkerContext)

  suspend fun generateCarePlan(
    planDefinitionId: String,
    subject: Resource,
    data: Bundle? = null
  ): CarePlan? {
    return generateCarePlan(fhirEngine.get(planDefinitionId), subject, data)
  }

  suspend fun generateCarePlan(
    planDefinition: PlanDefinition,
    subject: Resource,
    data: Bundle? = null
  ): CarePlan? {
    val output =
      CarePlan().apply {
        this.title = planDefinition.title
        this.description = planDefinition.description
      }

    planDefinition.action.forEach { action ->
      val input = Bundle().apply { entry.addAll(data?.entry ?: listOf()) }

      if (action.condition.all {
          if (it.kind != PlanDefinition.ActionConditionKind.APPLICABILITY)
            throw UnsupportedOperationException(
              "PlanDefinition.action.kind=${it.kind} not supported"
            )

          if (it.expression.language != Expression.ExpressionLanguage.TEXT_FHIRPATH.toCode())
            throw UnsupportedOperationException(
              "PlanDefinition.expression.language=${it.expression.language} not supported"
            )

          fhirPathEngine.evaluateToBoolean(input, null, subject, it.expression.expression)
        }
      ) {
        val source =
          Parameters().apply {
            addParameter(
              Parameters.ParametersParameterComponent().apply {
                this.name = CarePlan.SP_SUBJECT
                this.resource = subject
              }
            )

            addParameter(
              Parameters.ParametersParameterComponent().apply {
                this.name = PlanDefinition.SP_DEFINITION
                this.resource =
                  planDefinition.contained.first { it.id == action.definitionCanonicalType.value }
              }
            )
          }

        val structureMap = fhirEngine.get<StructureMap>(IdType(action.transform).idPart)
        structureMapUtilities.transform(
          transformSupportServices.simpleWorkerContext,
          source,
          structureMap,
          output
        )
      }
    }

    if (!output.hasActivity()) return null

    return output.also { Timber.d(it.encodeResourceToString()) }.also { careplan ->
      // save embedded resources inside as independent entries, clear embedded and save careplan
      val dependents = careplan.contained.map { it.copy() }

      careplan.contained.clear()
      fhirEngine.create(careplan)

      dependents.forEach { fhirEngine.create(it) }
    }
  }

  suspend fun completeTask(id: String, encounterStatus: EncounterStatus?) {
    fhirEngine.run {
      val task = get<Task>(id).apply {
        this.status = encounterStatusToTaskStatus(encounterStatus)
        this.lastModified = Date()
      }
      create(
        task
      )
      if (task.status == Task.TaskStatus.COMPLETED) {
        val carePlans = search<CarePlan> {
          filter(CarePlan.SUBJECT, { value = task.`for`.reference })
        }
        var carePlanToUpdate: CarePlan? = null
        carePlans.forEach { carePlan ->
          for((index, value) in carePlan.activity.withIndex()) {
            val taskId = task.identifier.first()?.value
            if (taskId != null) {
              val outcome = value.outcomeReference.find { x -> x.reference.contains(taskId)}
              if (outcome != null) {
                carePlanToUpdate = carePlan.copy()
                carePlanToUpdate?.activity?.set(index, value.apply {
                  detail.status = CarePlan.CarePlanActivityStatus.COMPLETED
                })
                break
              }
            }
          }
        }
        carePlanToUpdate?.let { update(it) }
      }
    }
  }

  private fun encounterStatusToTaskStatus(encounterStatus: EncounterStatus?): Task.TaskStatus {
    if (encounterStatus == null) return Task.TaskStatus.COMPLETED

    return when (encounterStatus) {
      EncounterStatus.PLANNED -> Task.TaskStatus.DRAFT
      EncounterStatus.ARRIVED -> Task.TaskStatus.RECEIVED
      EncounterStatus.TRIAGED -> Task.TaskStatus.ACCEPTED
      EncounterStatus.ONLEAVE -> Task.TaskStatus.ONHOLD
      EncounterStatus.UNKNOWN -> Task.TaskStatus.FAILED
      EncounterStatus.INPROGRESS,
      EncounterStatus.CANCELLED,
      EncounterStatus.ENTEREDINERROR,
      EncounterStatus.NULL -> Task.TaskStatus.fromCode(encounterStatus.toCode())
      else -> Task.TaskStatus.COMPLETED
    }
  }
}
