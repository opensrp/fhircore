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

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.util.TerserUtil
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import org.hl7.fhir.r4.model.ActivityDefinition
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.BaseDateTimeType
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.PlanDefinition
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StructureMap
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.model.Task.TaskStatus
import org.hl7.fhir.r4.model.Timing
import org.hl7.fhir.r4.model.Type
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.hl7.fhir.r4.utils.StructureMapUtilities
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.extension.addResourceParameter
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.extractFhirpathDuration
import org.smartregister.fhircore.engine.util.extension.extractFhirpathPeriod
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.isIn
import org.smartregister.fhircore.engine.util.extension.referenceValue
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FhirCarePlanGenerator
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val fhirPathEngine: FHIRPathEngine,
  val transformSupportServices: TransformSupportServices,
  val defaultRepository: DefaultRepository,
  val workManager: WorkManager
) {
  val structureMapUtilities by lazy {
    StructureMapUtilities(transformSupportServices.simpleWorkerContext, transformSupportServices)
  }

  suspend fun generateOrUpdateCarePlan(
    planDefinitionId: String,
    subject: Resource,
    data: Bundle = Bundle()
  ): CarePlan? {
    return generateOrUpdateCarePlan(fhirEngine.get(planDefinitionId), subject, data)
  }

  suspend fun generateOrUpdateCarePlan(
    planDefinition: PlanDefinition,
    subject: Resource,
    data: Bundle = Bundle()
  ): CarePlan? {
    // Only one CarePlan per plan , update or init a new one if not exists
    val output =
      fhirEngine
        .search<CarePlan> {
          filter(CarePlan.INSTANTIATES_CANONICAL, { value = planDefinition.referenceValue() })
        }
        .firstOrNull()
        ?: CarePlan()

    var carePlanModified = false

    planDefinition.action.forEach { action ->
      val input = Bundle().apply { entry.addAll(data.entry) }

      if (action.passesConditions(input, planDefinition, subject)) {
        val definition = action.activityDefinition(planDefinition)

        val source =
          Parameters().apply {
            addResourceParameter(CarePlan.SP_SUBJECT,subject)
            addResourceParameter(PlanDefinition.SP_DEFINITION, definition)
            // TODO find some other way (activity definition based) to pass additional data
            addResourceParameter(PlanDefinition.SP_DEPENDS_ON, data)
          }

        if (action.hasTransform()) {
          val taskPeriods = action.taskPeriods(definition.timingTiming, output)

          taskPeriods.forEach {
            source.setParameter(Task.SP_PERIOD, it)

            val structureMap = fhirEngine.get<StructureMap>(IdType(action.transform).idPart)
            structureMapUtilities.transform(
              transformSupportServices.simpleWorkerContext,
              source,
              structureMap,
              output
            )
          }
        }

        if (definition.hasDynamicValue()) {
          definition.dynamicValue.forEach { dynamicValue ->
            if (definition.kind == ActivityDefinition.ActivityDefinitionKind.CAREPLAN)
              dynamicValue.expression.expression
                .let { fhirPathEngine.evaluate(null, input, planDefinition, subject, it) }
                ?.let { evaluatedValue ->
                  TerserUtil.setFieldByFhirPath(
                    FhirContext.forR4Cached(),
                    dynamicValue.path,
                    output,
                    evaluatedValue.firstOrNull()
                  )
                }
            else throw UnsupportedOperationException("${definition.kind} not supported")
          }
        }
        carePlanModified = true
      }
    }

    if (carePlanModified) saveCarePlan(output)

    // Schedule onetime immediate job that updates the status of the tasks
    workManager.enqueue(OneTimeWorkRequestBuilder<FhirTaskPlanWorker>().build())

    return if (output.hasActivity()) output else null
  }

  private suspend fun saveCarePlan(output: CarePlan) {
    output.also { Timber.d(it.encodeResourceToString()) }.also { carePlan ->
      // Save embedded resources inside as independent entries, clear embedded and save carePlan
      val dependents = carePlan.contained.map { it }

      carePlan.contained.clear()

      // Save CarePlan only if it has activity, otherwise just save contained/dependent resources
      if (output.hasActivity()) defaultRepository.create(true, carePlan)

      dependents.forEach { defaultRepository.create(true, it) }

      if (carePlan.status == CarePlan.CarePlanStatus.COMPLETED)
        carePlan
          .activity
          .flatMap { it.outcomeReference }
          .filter { it.reference.startsWith(ResourceType.Task.name) }
          .mapNotNull { getTask(it.extractId()) }
          .forEach {
            if (it.status.isIn(TaskStatus.REQUESTED, TaskStatus.READY, TaskStatus.INPROGRESS)) {
              cancelTask(it.logicalId, "${carePlan.fhirType()} ${carePlan.status}")
            }
          }
    }
  }

  suspend fun transitionTaskTo(id: String, status: TaskStatus, reason: String? = null) {
    getTask(id)
      ?.apply {
        this.status = status
        this.lastModified = Date()
        if (reason != null) this.statusReason = CodeableConcept().apply { text = reason }
      }
      ?.run { defaultRepository.addOrUpdate(addMandatoryTags = true, resource = this) }
  }

  suspend fun cancelTask(id: String, reason: String) {
    transitionTaskTo(id, TaskStatus.CANCELLED, reason)
  }

  suspend fun getTask(id: String) =
    kotlin.runCatching { fhirEngine.get<Task>(id) }.onFailure { Timber.e(it) }.getOrNull()

  private fun evaluateToDate(base: Base?, expression: String): BaseDateTimeType? =
    base?.let { fhirPathEngine.evaluate(it, expression).firstOrNull()?.dateTimeValue() }

  private fun PlanDefinition.PlanDefinitionActionComponent.passesConditions(focus: Resource?, root: Resource?, base: Base)=
    this.condition.all {
      require (it.kind == PlanDefinition.ActionConditionKind.APPLICABILITY) {
        "PlanDefinition.action.kind=${it.kind} not supported"
      }

      require (it.expression.language == Expression.ExpressionLanguage.TEXT_FHIRPATH.toCode()) {
          "PlanDefinition.expression.language=${it.expression.language} not supported"
      }

      fhirPathEngine.evaluateToBoolean(focus, root, base, it.expression.expression)
    }

  private fun PlanDefinition.PlanDefinitionActionComponent.activityDefinition(planDefinition: PlanDefinition) =
    planDefinition.contained.filter { it.resourceType == ResourceType.ActivityDefinition }
      .first { it.logicalId == this.definitionCanonicalType.value } as
            ActivityDefinition

  private fun PlanDefinition.PlanDefinitionActionComponent.taskPeriods(timing: Type, carePlan: CarePlan): List<Period> {
    val taskPeriods = mutableListOf<Period>()

    require(timing is Timing) {
      "Timing component should only be Timing. Can not handle ${timing.fhirType()}"
    }

    val count = timing.repeat.count
    val periodExpression = timing.extractFhirpathPeriod()
    val durationExpression = timing.extractFhirpathDuration()

    // offset date for current task period; careplan start if all tasks generated at once
    // otherwise today means that tasks are generated on demand
    var offsetDate: BaseDateTimeType =
      DateType(if (count > 1) carePlan.period.start else Date())

    var i = 1 // task index
    do {
      if (periodExpression.isNotBlank())
        evaluateToDate(offsetDate, "\$this + $periodExpression")?.let { offsetDate = it }

      Period().apply {
        start = offsetDate.value

        end =
          if (durationExpression.isNotBlank())
            evaluateToDate(offsetDate, "\$this + $durationExpression")?.value
          else carePlan.period.end
      }
        .also { taskPeriods.add(it) }

      i++
    }
    while (count >= i)

    return taskPeriods
  }
}
