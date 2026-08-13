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
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PlanDefinition
import org.hl7.fhir.r4.model.RequestGroup
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.TriggerDefinition
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.batchedSearch
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import timber.log.Timber

/**
 * Extension URL suffixes stamped by TRICC on each Intervention PD's per-process action — see
 * `feature/20260812-intervention-order-and-dedup.md` (tricc). Matched by suffix, not exact URL,
 * since the base URL is per-project configurable at export time.
 */
private const val TRICC_PROCESS_EXT_SUFFIX = "tricc-process"
private const val TRICC_PROCESS_ORDER_EXT_SUFFIX = "tricc-process-order"

/**
 * Discovers synced PlanDefinitions by named-event trigger, evaluates applicability (trigger +
 * conditions), and returns intervention options that can launch a **Questionnaire**. Catalog is
 * FHIR content only — see `feature/register-tricc.md`.
 *
 * Options without a resolvable Questionnaire id are **omitted** (no empty toast entries).
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
    /**
     * cpg-common-process order (10, 20, 30…), read from the `tricc-process-order` extension —
     * see `feature/20260812-intervention-order-and-dedup.md` (tricc). Fixed/canonical, so it is
     * comparable across options originating from different PlanDefinitions. Options without the
     * extension (e.g. resolved via workflow `$apply`, which may not propagate custom action
     * extensions onto the RequestGroup) sort last.
     */
    val order: Int = Int.MAX_VALUE,
    /** cpg-common-process name, read from the `tricc-process` extension, for display/debug. */
    val process: String? = null,
  )

  /**
   * One row of the "select available care" picker: a PlanDefinition carrying the named-event
   * trigger, together with its own already-resolved (`$apply`/valid-action) options. Grouping by
   * originating PlanDefinition — rather than the flat per-questionnaire list [listInterventions]
   * returns — lets the picker show one checkbox per care option, and lets the caller consolidate
   * several *selected* PDs' options together afterwards without re-running `$apply`.
   */
  data class AvailableCarePlan(
    val planDefinitionId: String,
    val title: String,
    val options: List<InterventionOption>,
  )

  /**
   * Returns one [AvailableCarePlan] per PlanDefinition matching [namedEvent] that has at least
   * one launchable (Questionnaire-resolvable) option for [subjectPatientId] — i.e. exactly the
   * set of rows the "select available care" picker should list, each with its own checkbox.
   * PlanDefinitions with no launchable option are omitted (nothing to check).
   */
  suspend fun listAvailableCarePlans(
    namedEvent: String,
    subjectPatientId: String,
  ): List<AvailableCarePlan> {
    val patientId = subjectPatientId.extractLogicalIdUuid()
    val patient =
      runCatching { fhirEngine.get<Patient>(patientId) }
        .onFailure { Timber.e(it, "Patient/$patientId not found for named-event apply") }
        .getOrNull() ?: return emptyList()

    val matching = loadPlanDefinitions().filter { it.hasNamedEventTrigger(namedEvent) }
    if (matching.isEmpty()) {
      Timber.i("No PlanDefinitions with named-event '$namedEvent'")
      return emptyList()
    }

    return matching.mapNotNull { planDefinition ->
      val options = linkedMapOf<String, InterventionOption>()
      collectFromPlanDefinition(planDefinition, namedEvent, patient, options)
      val launchable =
        options.values.filter { !it.questionnaireId.isNullOrBlank() }.sortedBy { it.order }
      if (launchable.isEmpty()) {
        null
      } else {
        AvailableCarePlan(
          planDefinitionId = planDefinition.logicalId,
          title = planDefinition.title ?: planDefinition.name ?: "Care",
          options = launchable,
        )
      }
    }
  }

  /**
   * Returns applicable interventions for [subjectPatientId] whose PlanDefinition actions declare
   * named-event [namedEvent] (default `available-care`).
   *
   * Prefers evaluating action.condition with FHIRPath (no side effects). Falls back to workflow
   * `$apply` when conditions use non-FHIRPath languages. Browse path is read-only (`persist =
   * false`).
   *
   * Only options with a **Questionnaire** launch target are returned. A PD that matches the
   * named-event but has no applicable action that points at a Questionnaire contributes nothing.
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

    // Only launchable questionnaires — drop Task/AD/#fragment or empty apply results.
    // Sorted by cpg-common-process order (stable — ties keep discovery/insertion order) so a
    // caller juggling several selected PlanDefinitions can pick the lowest-order option overall.
    val launchable =
      options.values
        .filter { !it.questionnaireId.isNullOrBlank() }
        .sortedBy { it.order }
        .also { list ->
          Timber.i(
            "Named-event '$namedEvent': ${list.size} launchable intervention(s) " +
              "(${options.size} raw option(s) before Questionnaire filter)",
          )
        }
    return launchable
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
        // Strategy PD: evaluate nested children; leaf PD: evaluate the matching action itself
        actionsWithEvent.flatMap { parent ->
          if (parent.action.isNullOrEmpty()) listOf(parent) else parent.action
        }
      } else {
        emptyList()
      }

    if (actionsToEvaluate.isEmpty()) {
      Timber.d(
        "PlanDefinition/${planDefinition.logicalId} matched named-event but has no actions to evaluate",
      )
      return
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
      if (!action.passesFhirPathConditions(patient)) {
        Timber.d(
          "Skipping non-applicable action '${action.title}' on PlanDefinition/${planDefinition.logicalId}",
        )
        return@forEach
      }
      addResolvedOption(action.toInterventionOption(planDefinition), options)
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
            addResolvedOption(rgAction.toInterventionOption(planDefinition), options)
          }
        }
        // Do not invent toast-only options from CarePlan.activity without a Questionnaire.
      }
      .onFailure {
        Timber.e(it, "Workflow \$apply failed for PlanDefinition/${planDefinition.logicalId}")
      }
  }

  /**
   * Prefer options that already resolve to a Questionnaire. If the action points at another
   * PlanDefinition, follow one level to its first applicable Questionnaire action.
   */
  private suspend fun addResolvedOption(
    option: InterventionOption?,
    options: MutableMap<String, InterventionOption>,
  ) {
    if (option == null) return
    if (!option.questionnaireId.isNullOrBlank()) {
      // Dedupe by questionnaire id so catalog + leaf do not double-list
      options.putIfAbsent(option.questionnaireId!!, option.copy(id = option.questionnaireId!!))
      return
    }
    val nestedPlanId = option.planDefinitionId
    if (!nestedPlanId.isNullOrBlank() && nestedPlanId != option.id) {
      val nested =
        runCatching { fhirEngine.get<PlanDefinition>(nestedPlanId) }
          .onFailure {
            Timber.w(it, "Could not load nested PlanDefinition/$nestedPlanId for intervention")
          }
          .getOrNull()
      if (nested != null) {
        nested.action.forEach { nestedAction ->
          val nestedOption = nestedAction.toInterventionOption(nested)
          if (nestedOption != null && !nestedOption.questionnaireId.isNullOrBlank()) {
            options.putIfAbsent(
              nestedOption.questionnaireId!!,
              nestedOption.copy(id = nestedOption.questionnaireId!!),
            )
          }
        }
      }
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
        // Non-FHIRPath handled by needsApply branch; treat as not applicable on this path
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
    val id =
      questionnaireId
        ?: id
        ?: definition
        ?: "${planDefinition.logicalId}-${title.hashCode()}"
    return InterventionOption(
      id = id,
      title = title,
      description = description,
      definitionCanonical = definition,
      planDefinitionId = nestedPlanId ?: planDefinition.logicalId,
      questionnaireId = questionnaireId,
      order = triccProcessOrder() ?: Int.MAX_VALUE,
      process = triccProcessName(),
    )
  }

  private fun PlanDefinition.PlanDefinitionActionComponent.triccProcessOrder(): Int? =
    extension
      .firstOrNull { it.url?.endsWith(TRICC_PROCESS_ORDER_EXT_SUFFIX) == true }
      ?.value
      ?.let { (it as? IntegerType)?.value }

  private fun PlanDefinition.PlanDefinitionActionComponent.triccProcessName(): String? =
    extension
      .firstOrNull { it.url?.endsWith(TRICC_PROCESS_EXT_SUFFIX) == true }
      ?.value
      ?.let { (it as? StringType)?.value }

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
    val id =
      questionnaireId
        ?: this.id
        ?: definition
        ?: "${planDefinition.logicalId}-${title.hashCode()}"
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
