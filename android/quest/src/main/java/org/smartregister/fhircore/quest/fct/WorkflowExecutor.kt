package org.smartregister.fhircore.quest.fct

import android.content.Context
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.util.TerserUtil
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.get
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.ActivityDefinition
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.BaseDateTimeType
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Dosage
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.PlanDefinition
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StructureMap
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.model.Timing
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.hl7.fhir.r4.utils.StructureMapUtilities
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.task.WorkflowCarePlanGenerator
import org.smartregister.fhircore.engine.util.extension.addResourceParameter
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.encodeJson
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.extractFhirpathDuration
import org.smartregister.fhircore.engine.util.extension.extractFhirpathPeriod
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices
import timber.log.Timber
import java.util.Date

class WorkflowExecutor(
    private val context: Context,
    private val fhirEngine: FhirEngine,
    private val fhirPathEngine: FHIRPathEngine,
    private val transformSupportServices: TransformSupportServices,
    private val workflowCarePlanGenerator: WorkflowCarePlanGenerator
) {

    private val structureMapUtilities by lazy {
        StructureMapUtilities(
            transformSupportServices.simpleWorkerContext,
            transformSupportServices,
        )
    }

    fun execute(arg: String): String {

        return try {

            val workflowRequest = arg.decodeJson<WorkflowRequest>()
            val planDefinition =
                workflowRequest.planDefinition.decodeResourceFromString<PlanDefinition>()
            val subject = workflowRequest.subject.decodeResourceFromString<Resource>()

            val relatedEntityLocationTags =
                subject.meta.tag.filter {
                    it.system == context.getString(R.string.sync_strategy_related_entity_location_system)
                }

            val carePlan = CarePlan().apply {
                this.title = planDefinition.title
                this.description = planDefinition.description
                this.instantiatesCanonical =
                    listOf(CanonicalType(planDefinition.asReference().reference))
                relatedEntityLocationTags.forEach(this.meta::addTag)
            }

            when (workflowRequest.type) {

                // generate apply workflow
                WorkflowType.Apply -> generateApplyCareplan(
                    carePlan = carePlan,
                    planDefinition = planDefinition,
                    subject = subject as Patient,
                    workflowRequest = workflowRequest
                )

                // generate apply workflow
                WorkflowType.Lite -> runBlocking {
                    generateLiteCareplan(
                        workflowRequest = workflowRequest,
                        carePlan = carePlan,
                        planDefinition = planDefinition,
                        subject = subject,
                        relatedEntityLocationTags = relatedEntityLocationTags
                    )
                }
            }
        } catch (ex: Exception) {
            return Response(
                error = ex.message ?: "Query Error"
            ).encodeJson()
        }

    }

    private fun generateApplyCareplan(
        carePlan: CarePlan,
        planDefinition: PlanDefinition,
        subject: Patient,
        workflowRequest: WorkflowRequest,
    ): String {
        return runBlocking {

            val data = Bundle().apply {
                workflowRequest.otherResource.forEach { resourceString ->
                    addEntry().apply {
                        resource = resourceString.decodeResourceFromString()
                    }
                }
            }

            workflowCarePlanGenerator.applyPlanDefinitionOnPatient(
                planDefinition = planDefinition,
                patient = subject,
                data = data,
                output = carePlan,
            )

            Response(
                error = null,
                result = listOf(
                    carePlan.encodeResourceToString()
                )
            ).encodeJson()
        }
    }

    private suspend fun generateLiteCareplan(
        planDefinition: PlanDefinition,
        subject: Resource,
        carePlan: CarePlan,
        workflowRequest: WorkflowRequest,
        relatedEntityLocationTags: List<Coding>
    ): String {


        val data = Bundle().apply {
            workflowRequest.otherResource.forEach { resourceString ->
                addEntry().apply {
                    resource = resourceString.decodeResourceFromString()
                }
            }
        }

        planDefinition.action.forEach { action ->
            val input = Bundle().apply { entry.addAll(data.entry) }

            if (action.passesConditions(input, planDefinition, subject)) {
                val definition = action.activityDefinition(planDefinition)

                if (action.hasTransform()) {
                    val taskPeriods = action.taskPeriods(definition, carePlan)

                    taskPeriods.forEachIndexed { index, period ->
                        val source =
                            Parameters().apply {
                                addResourceParameter(CarePlan.SP_SUBJECT, subject)
                                addResourceParameter(PlanDefinition.SP_DEFINITION, definition)
                                addResourceParameter(PlanDefinition.SP_DEPENDS_ON, data)
                            }
                        source.setParameter(Task.SP_PERIOD, period)
                        source.setParameter(ActivityDefinition.SP_VERSION, IntegerType(index))

                        val id = IdType(action.transform).idPart

                        val structureMap = workflowRequest.otherResource
                            .map { it.decodeResourceFromString<Resource>() }
                            .filterIsInstance<StructureMap>()
                            .firstOrNull { it.logicalId == id } ?: fhirEngine.get<StructureMap>(id)

                        structureMapUtilities.transform(
                            transformSupportServices.simpleWorkerContext,
                            source,
                            structureMap,
                            carePlan,
                        )
                    }
                }

                if (definition.hasDynamicValue()) {
                    definition.dynamicValue.forEach { dynamicValue ->
                        if (definition.kind == ActivityDefinition.ActivityDefinitionKind.CAREPLAN) {
                            dynamicValue.expression.expression
                                .let {
                                    fhirPathEngine.evaluate(
                                        null,
                                        input,
                                        planDefinition,
                                        subject,
                                        it,
                                    )
                                }
                                ?.takeIf { it.isNotEmpty() }
                                ?.let { evaluatedValue ->
                                    Timber.d("${dynamicValue.path}, evaluatedValue: $evaluatedValue")
                                    TerserUtil.setFieldByFhirPath(
                                        FhirContext.forR4Cached(),
                                        dynamicValue.path.removePrefix("${definition.kind.display}."),
                                        carePlan,
                                        evaluatedValue.first(),
                                    )
                                }
                        } else {
                            throw UnsupportedOperationException("${definition.kind} not supported")
                        }
                    }
                }
            }
        }

        //val carePlanTasks = carePlan.contained.filterIsInstance<Task>()
        carePlan.cleanPlanDefinitionCanonical()
        val dependents = extractDependents(carePlan, relatedEntityLocationTags)

        /*if (carePlanTasks.isNotEmpty()) {
            fhirResourceUtil.updateUpcomingTasksToDue(
                subject = subject.asReference(),
                taskResourcesToFilterBy = carePlanTasks,
            )
        }*/

        return Response(
            error = null,
            result = listOf(carePlan.encodeResourceToString()) + dependents.map { it.encodeResourceToString() }
        ).encodeJson()
    }

    private fun PlanDefinition.PlanDefinitionActionComponent.passesConditions(
        focus: Resource?,
        root: Resource?,
        base: Base,
    ) =
        this.condition.all {
            require(it.kind == PlanDefinition.ActionConditionKind.APPLICABILITY) {
                "PlanDefinition.action.kind=${it.kind} not supported"
            }

            require(it.expression.language == Expression.ExpressionLanguage.TEXT_FHIRPATH.toCode()) {
                "PlanDefinition.expression.language=${it.expression.language} not supported"
            }

            fhirPathEngine.evaluateToBoolean(focus, root, base, it.expression.expression)
        }

    private fun PlanDefinition.PlanDefinitionActionComponent.activityDefinition(
        planDefinition: PlanDefinition,
    ) =
        planDefinition.contained
            .filter { it.resourceType == ResourceType.ActivityDefinition }
            .first { it.logicalId == this.definitionCanonicalType.value } as ActivityDefinition

    private fun PlanDefinition.PlanDefinitionActionComponent.taskPeriods(
        definition: ActivityDefinition,
        carePlan: CarePlan,
    ): List<Period> {
        return when {
            definition.hasDosage() -> extractTaskPeriodsFromDosage(definition.dosage, carePlan)
            definition.hasTiming() && !definition.hasTimingTiming() ->
                throw IllegalArgumentException(
                    "Timing component should only be Timing. Can not handle ${timing.fhirType()}",
                )

            else -> extractTaskPeriodsFromTiming(definition.timingTiming, carePlan)
        }
    }

    private fun extractTaskPeriodsFromTiming(timing: Timing, carePlan: CarePlan): List<Period> {
        val taskPeriods = mutableListOf<Period>()
        // TODO handle properties used by older PlanDefintions. If any PlanDefinition is using
        // countMax, frequency, durationUnit of hour, consider as non compliant and assume
        // all handling would be done with a structure map. Once all PlanDefinitions are using
        // recommended approach and using plan definitions properly change line below to use
        // timing.repeat.count only
        val isLegacyPlanDefinition =
            (timing.repeat.hasFrequency() ||
                    timing.repeat.hasCountMax() ||
                    timing.repeat.durationUnit?.equals(Timing.UnitsOfTime.H) == true)
        val count =
            if (isLegacyPlanDefinition || !timing.repeat.hasCount()) 1 else timing.repeat.count

        val periodExpression = timing.extractFhirpathPeriod()
        val durationExpression = timing.extractFhirpathDuration()

        // Offset date for current task period; CarePlan start if all tasks generated at once
        // otherwise today means that tasks are generated on demand
        var offsetDate: BaseDateTimeType =
            DateTimeType(if (timing.repeat.hasCount()) carePlan.period.start else Date())

        for (i in 1..count) {
            if (periodExpression.isNotBlank() && offsetDate.hasValue()) {
                evaluateToDate(offsetDate, "\$this + $periodExpression")?.let { offsetDate = it }
            }

            Period()
                .apply {
                    start = offsetDate.value
                    end =
                        if (durationExpression.isNotBlank() && offsetDate.hasValue()) {
                            evaluateToDate(offsetDate, "\$this + $durationExpression")?.value
                        } else {
                            carePlan.period.end
                        }
                }
                .also { taskPeriods.add(it) }
        }

        return taskPeriods
    }

    private fun extractTaskPeriodsFromDosage(
        dosage: List<Dosage>,
        carePlan: CarePlan,
    ): List<Period> {
        val taskPeriods = mutableListOf<Period>()
        dosage
            .flatMap { extractTaskPeriodsFromTiming(it.timing, carePlan) }
            .also { taskPeriods.addAll(it) }

        return taskPeriods
    }

    private fun evaluateToDate(base: Base?, expression: String): BaseDateTimeType? =
        base?.let { fhirPathEngine.evaluate(it, expression).firstOrNull()?.dateTimeValue() }

    private fun CarePlan.cleanPlanDefinitionCanonical() {
        val canonicalValue = this.instantiatesCanonical.first().value
        if (canonicalValue.contains('/').not()) {
            this.instantiatesCanonical = listOf(CanonicalType("PlanDefinition/$canonicalValue"))
        }
    }

    private fun extractDependents(
        carePlan: CarePlan,
        relatedEntityLocationTags: List<Coding>
    ): List<Resource> {

        // Save embedded resources inside as independent entries, clear embedded and save carePlan
        val dependents = carePlan.contained.map { it }

        carePlan.contained.clear()

        dependents.forEach {
            relatedEntityLocationTags.forEach(it.meta::addTag)
        }

        return dependents
    }

    @Serializable
    private data class Response(
        var error: String?,
        val result: List<String> = listOf()
    )

    @Serializable
    private data class WorkflowRequest(
        val type: WorkflowType,
        val planDefinition: String,
        val subject: String,
        val otherResource: List<String>
    )

    private enum class WorkflowType {
        Lite, Apply
    }
}