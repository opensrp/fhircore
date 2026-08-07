/*
 * Copyright 2021-2024 Ona Systems, Inc
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
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.get
import com.google.android.fhir.search.Search
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PlanDefinition
import org.hl7.fhir.r4.model.RequestGroup
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.TriggerDefinition
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.batchedSearch
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import timber.log.Timber

/**
 * Discovers synced PlanDefinitions by named-event trigger, evaluates applicability (trigger +
 * conditions), and returns intervention options for the user to pick. Catalog is FHIR content only
 * — see `feature/register-tricc.md`.
 */
@Singleton
class NamedEventInterventionService
@Inject
constructor(
  private val fhirEngine: FhirEngine,
  private val fhirPathEngine: FHIRPathEngine,
  private val workflowCarePlanGenerator: WorkflowCarePlanGenerator,
) {

  data class InterventionOption(
    val id: String,
    val title: String,
    val description: String? = null,
    val definitionCanonical: String? = null,
    val planDefinitionId: String? = null,
    val questionnaireId: String? = null,
  )

  /**
   * Returns applicable interventions for [subjectPatientId] whose PlanDefinition actions declare
   * named-event [namedEvent] (default `available-care`).
   *
   * Prefers evaluating action.condition with FHIRPath (no side effects). Falls back to workflow
   * `$apply` when RequestGroup recommendations are needed or conditions use non-FHIRPath languages.
   * This is a read-only discovery/preview operation: the `$apply` fallback runs with
   * `persist = false`, so browsing for applicable care never writes Task/RequestGroup/CarePlan
   * resources to the local database — only actually starting an intervention should do that.
   */
  suspend fun listInterventions(
    namedEvent: String,
    subjectPatientId: String,
  ): List<InterventionOption> {
    val patientId = subjectPatientId.extractLogicalIdUuid()
    val patient =
      runCatching { fhirEngine.get<Patient>(patientId) }
        .onFailure { Timber.e(it, "Patient/$patientId not found for named-event apply") }
        .getOrNull() ?: return emptyList()

    val planDefinitions = loadPlanDefinitions()
    if (planDefinitions.isEmpty()) {
      Timber.w("No PlanDefinitions in local store for named-event '$namedEvent'")
      return emptyList()
    }

    val matching = planDefinitions.filter { it.hasNamedEventTrigger(namedEvent) }
    if (matching.isEmpty()) {
      Timber.i("No PlanDefinitions with named-event '$namedEvent'")
      return emptyList()
    }

    val options = linkedMapOf<String, InterventionOption>()

    matching.forEach { planDefinition ->
      collectFromPlanDefinition(
        planDefinition = planDefinition,
        namedEvent = namedEvent,
        patient = patient,
        options = options,
      )
    }

    return options.values.toList()
  }

  private suspend fun loadPlanDefinitions(): List<PlanDefinition> {
    return runCatching {
        fhirEngine.batchedSearch<PlanDefinition>(Search(ResourceType.PlanDefinition)).map {
          it.resource
        }
      }
      .onFailure { Timber.e(it, "Failed to search PlanDefinitions") }
      .getOrDefault(emptyList())
  }

  private suspend fun collectFromPlanDefinition(
    planDefinition: PlanDefinition,
    namedEvent: String,
    patient: Patient,
    options: MutableMap<String, InterventionOption>,
  ) {
    val actionsWithEvent = planDefinition.action.filter { it.hasNamedEventTrigger(namedEvent) }
    val actionsToEvaluate =
      if (actionsWithEvent.isNotEmpty()) {
        // Include nested actions under matching top-level actions (strategy PD)
        actionsWithEvent.flatMap { parent ->
          if (parent.action.isNullOrEmpty()) listOf(parent) else parent.action
        }
      } else {
        emptyList()
      }

    val needsApply =
      actionsToEvaluate.any { action ->
        action.condition.any {
          it.hasExpression() &&
            it.expression.language != Expression.ExpressionLanguage.TEXT_FHIRPATH.toCode()
        }
      }

    if (needsApply) {
      collectFromWorkflowApply(planDefinition, patient, options)
      return
    }

    actionsToEvaluate.forEach { action ->
      if (!action.passesFhirPathConditions(patient)) return@forEach
      val option = action.toInterventionOption(planDefinition)
      if (option != null) {
        options.putIfAbsent(option.id, option)
      }
    }

    // Nested strategy actions may not carry the named-event themselves; if top-level matched and
    // we only evaluated children via flatMap above, also try apply when no options yet.
    if (options.isEmpty() && planDefinition.action.any { it.action.isNotEmpty() }) {
      collectFromWorkflowApply(planDefinition, patient, options)
    }
  }

  private suspend fun collectFromWorkflowApply(
    planDefinition: PlanDefinition,
    patient: Patient,
    options: MutableMap<String, InterventionOption>,
  ) {
    runCatching {
        val carePlan =
          CarePlan().apply {
            status = CarePlan.CarePlanStatus.DRAFT
            intent = CarePlan.CarePlanIntent.PROPOSAL
            subject = patient.asReference()
          }
        workflowCarePlanGenerator.applyPlanDefinitionOnPatient(
          planDefinition = planDefinition,
          patient = patient,
          data = Bundle(),
          output = carePlan,
          persist = false,
        )
        carePlan.contained.filterIsInstance<RequestGroup>().forEach { requestGroup ->
          requestGroup.action.forEach { rgAction ->
            val option = rgAction.toInterventionOption(planDefinition)
            if (option != null) options.putIfAbsent(option.id, option)
          }
        }
        // Fallback: activities with descriptions
        if (options.isEmpty()) {
          carePlan.activity.forEachIndexed { index, activity ->
            val title =
              activity.detail?.description
                ?: activity.detail?.code?.codingFirstRep?.display
                ?: planDefinition.title
                ?: planDefinition.name
                ?: "Intervention ${index + 1}"
            val id = "${planDefinition.logicalId}-activity-$index"
            options.putIfAbsent(
              id,
              InterventionOption(
                id = id,
                title = title,
                description = activity.detail?.description,
                planDefinitionId = planDefinition.logicalId,
              ),
            )
          }
        }
      }
      .onFailure {
        Timber.e(it, "Workflow \$apply failed for PlanDefinition/${planDefinition.logicalId}")
      }
  }

  private fun PlanDefinition.PlanDefinitionActionComponent.passesFhirPathConditions(
    patient: Patient,
  ): Boolean {
    if (condition.isNullOrEmpty()) return true
    return condition.all { conditionComponent ->
      if (conditionComponent.kind != PlanDefinition.ActionConditionKind.APPLICABILITY) {
        return@all true
      }
      if (!conditionComponent.hasExpression()) return@all true
      val language = conditionComponent.expression.language
      if (language != Expression.ExpressionLanguage.TEXT_FHIRPATH.toCode()) {
        Timber.w("Skipping non-FHIRPath condition language=$language")
        return@all false
      }
      runCatching {
          fhirPathEngine.evaluateToBoolean(
            null,
            null,
            patient,
            conditionComponent.expression.expression,
          )
        }
        .onFailure {
          Timber.e(it, "Applicability FHIRPath failed: ${conditionComponent.expression.expression}")
        }
        .getOrDefault(false)
    }
  }

  private fun PlanDefinition.PlanDefinitionActionComponent.toInterventionOption(
    planDefinition: PlanDefinition,
  ): InterventionOption? {
    val title = title ?: description ?: planDefinition.title ?: planDefinition.name ?: return null
    val definition =
      when {
        hasDefinitionCanonicalType() -> definitionCanonicalType.value
        hasDefinitionUriType() -> definitionUriType.valueAsString
        else -> null
      }
    val questionnaireId =
      definition
        ?.substringAfter("Questionnaire/", missingDelimiterValue = "")
        ?.takeIf { definition.contains("Questionnaire/") && it.isNotBlank() }
        ?.extractLogicalIdUuid()
    val nestedPlanId =
      definition
        ?.substringAfter("PlanDefinition/", missingDelimiterValue = "")
        ?.takeIf { definition.contains("PlanDefinition/") && it.isNotBlank() }
        ?.extractLogicalIdUuid()
    val id = id ?: definition ?: "${planDefinition.logicalId}-${title.hashCode()}"
    return InterventionOption(
      id = id,
      title = title,
      description = description,
      definitionCanonical = definition,
      planDefinitionId = nestedPlanId ?: planDefinition.logicalId,
      questionnaireId = questionnaireId,
    )
  }

  private fun RequestGroup.RequestGroupActionComponent.toInterventionOption(
    planDefinition: PlanDefinition,
  ): InterventionOption? {
    val title = title ?: description ?: return null
    val resourceRef = resource?.reference
    val definition = resourceRef
    val questionnaireId =
      resourceRef
        ?.substringAfter("Questionnaire/", missingDelimiterValue = "")
        ?.takeIf { resourceRef.contains("Questionnaire/") && it.isNotBlank() }
        ?.extractLogicalIdUuid()
    val planId =
      resourceRef
        ?.substringAfter("PlanDefinition/", missingDelimiterValue = "")
        ?.takeIf { resourceRef.contains("PlanDefinition/") && it.isNotBlank() }
        ?.extractLogicalIdUuid()
    val id = this.id ?: definition ?: "${planDefinition.logicalId}-${title.hashCode()}"
    return InterventionOption(
      id = id,
      title = title,
      description = description,
      definitionCanonical = definition,
      planDefinitionId = planId ?: planDefinition.logicalId,
      questionnaireId = questionnaireId,
    )
  }

  private fun PlanDefinition.hasNamedEventTrigger(namedEvent: String): Boolean {
    return action.any { it.hasNamedEventTrigger(namedEvent) }
  }

  private fun PlanDefinition.PlanDefinitionActionComponent.hasNamedEventTrigger(
    namedEvent: String,
  ): Boolean {
    if (
      trigger.any {
        it.type == TriggerDefinition.TriggerType.NAMEDEVENT &&
          it.name.equals(namedEvent, ignoreCase = true)
      }
    ) {
      return true
    }
    return action.any { it.hasNamedEventTrigger(namedEvent) }
  }
}
