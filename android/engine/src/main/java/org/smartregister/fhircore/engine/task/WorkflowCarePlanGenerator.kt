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

import android.content.Context
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.util.TerserUtil
import com.google.android.fhir.knowledge.KnowledgeManager
import com.google.android.fhir.workflow.FhirOperator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.ParameterDefinition
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PlanDefinition
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.opencds.cqf.fhir.cql.LibraryEngine
import org.opencds.cqf.fhir.cr.plandefinition.PlanDefinitionProcessor
import org.opencds.cqf.fhir.utility.monad.Eithers
import org.opencds.cqf.fhir.utility.r4.Parameters.part
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import timber.log.Timber

@Singleton
class WorkflowCarePlanGenerator
@Inject
constructor(
  val knowledgeManager: KnowledgeManager,
  val fhirOperator: FhirOperator,
  val defaultRepository: DefaultRepository,
  val fhirPathEngine: FHIRPathEngine,
  @ApplicationContext val context: Context,
) {

  /**
   * Executes $apply on a [PlanDefinition] for a [Patient] and creates the request resources as per
   * the proposed [CarePlan]
   *
   * @param planDefinition PlanDefinition for which $apply is run
   * @param patient Patient resource for which the [PlanDefinition] $apply is run
   * @param data Bundle resource containing the input resource/data
   * @param output [CarePlan] resource object with the generated care plan
   */
  suspend fun applyPlanDefinitionOnPatient(
    planDefinition: PlanDefinition,
    patient: Patient,
    data: Bundle = Bundle(),
    output: CarePlan,
  ) {
    withContext(Dispatchers.IO) {
      val carePlanProposal =
        fhirOperator.generateCarePlan(
          planDefinition,
          patient,
          data,
        ) as CarePlan

      acceptCarePlan(carePlanProposal, output)

      resolveDynamicValues(
        planDefinition = planDefinition,
        input = data,
        subject = patient,
        output,
      )
    }
  }

  private fun FhirOperator.generateCarePlan(
    planDefinition: PlanDefinition,
    subject: Patient,
    data: Bundle,
  ): IBaseResource {
    // TODO: Open this method on the Android SDK
    val planDefProcessor =
      javaClass.getDeclaredField("planDefinitionProcessor").let {
        it.isAccessible = true
        return@let it.get(this) as PlanDefinitionProcessor
      }

    val libraryProcessor =
      javaClass.getDeclaredField("libraryProcessor").let {
        it.isAccessible = true
        return@let it.get(this) as LibraryEngine
      }

    val params = Parameters()
    params.addParameter(
      part("%resource", data).apply {
        extension.add(
          Extension(
            "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition",
            ParameterDefinition().apply {
              type = data.fhirType()
              min = 0
              max = data.entry.size.toString()
            },
          ),
        )
      },
    )
    params.addParameter(part("%rootResource", planDefinition))
    params.addParameter(part("%subject", subject))

    return planDefProcessor.apply(
      Eithers.forRight3(planDefinition),
      "Patient/${subject.id}",
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      params,
      null,
      data,
      null,
      libraryProcessor,
    ) as IBaseResource
  }

  private fun resolveDynamicValues(
    planDefinition: PlanDefinition,
    input: Bundle,
    subject: Patient,
    output: CarePlan,
  ) {
    planDefinition.action
      .filter { it.hasDynamicValue() }
      .flatMap { it.dynamicValue }
      .forEach { dynamicValue ->
        dynamicValue.expression.expression
          .let { fhirPathEngine.evaluate(null, input, planDefinition, subject, it) }
          ?.takeIf { it.isNotEmpty() }
          ?.let { evaluatedValue ->
            Timber.d("${dynamicValue.path}, evaluatedValue: $evaluatedValue")
            TerserUtil.setFieldByFhirPath(
              FhirContext.forR4Cached(),
              dynamicValue.path,
              output,
              evaluatedValue.first(),
            )
          }
      }
  }

  /** Link the request resources created for the [Patient] back to the [CarePlan] of record */
  private fun addRequestResourcesToCarePlanOfRecord(
    carePlan: CarePlan,
    requestResourceList: List<Resource>,
  ) {
    for (resource in requestResourceList) {
      when (resource.fhirType()) {
        "Task" ->
          carePlan.addActivity().setReference(Reference(resource)).detail.status =
            mapRequestResourceStatusToCarePlanStatus(resource as Task)
        "QuestionnaireResponse" -> carePlan.addActivity().setReference(Reference(resource))
        "OperationOutcome" -> carePlan.addActivity().setReference(Reference(resource))
        "ServiceRequest" -> TODO("Not supported yet")
        "MedicationRequest" -> carePlan.addActivity().reference = Reference(resource)
        "SupplyRequest" -> TODO("Not supported yet")
        "Procedure" -> TODO("Not supported yet")
        "DiagnosticReport" -> TODO("Not supported yet")
        "Communication" -> TODO("Not supported yet")
        "CommunicationRequest" -> TODO("Not supported yet")
        else -> TODO("Not a valid request resource ${resource.fhirType()}")
      }
    }
  }

  /**
   * Invokes the respective [RequestResourceManager] to create new request resources as per the
   * proposed [CarePlan]
   *
   * @param resourceList List of request resources to be created
   * @param requestResourceConfigs Application-specific configurations to be applied on the created
   *   request resources
   */
  private suspend fun createProposedRequestResources(resourceList: List<Resource>): List<Resource> {
    val createdRequestResources = ArrayList<Resource>()
    for (resource in resourceList) {
      when (resource.fhirType()) {
        "Task",
        "QuestionnaireResponse",
        "OperationOutcome",
        "MedicationRequest",
        "CarePlan", -> {
          defaultRepository.create(true, resource)
          createdRequestResources.add(resource)
        }
        "ServiceRequest" -> TODO("Not supported yet")
        "SupplyRequest" -> TODO("Not supported yet")
        "Procedure" -> TODO("Not supported yet")
        "DiagnosticReport" -> TODO("Not supported yet")
        "Communication" -> TODO("Not supported yet")
        "CommunicationRequest" -> TODO("Not supported yet")
        "RequestGroup" -> {}
        else -> TODO("Not a valid request resource ${resource.fhirType()}")
      }
    }
    return createdRequestResources
  }

  /**
   * Accept the proposed [CarePlan] and create the proposed request resources as per the
   * configurations
   *
   * @param proposedCarePlan Proposed [CarePlan] generated when $apply is run on a [PlanDefinition]
   * @param carePlanOfRecord CarePlan of record for a [Patient] which needs to be updated with the
   *   new request resources created as per the proposed CarePlan
   * @param requestResourceConfigs Application-specific configurations to be applied on the created
   *   request resources
   */
  private suspend fun acceptCarePlan(
    proposedCarePlan: CarePlan,
    carePlanOfRecord: CarePlan,
  ) {
    val resourceList = createProposedRequestResources(proposedCarePlan.contained)
    addRequestResourcesToCarePlanOfRecord(carePlanOfRecord, resourceList)
  }

  /** Map [Task] status to [CarePlan] status */
  private fun mapRequestResourceStatusToCarePlanStatus(
    resource: Task,
  ): CarePlan.CarePlanActivityStatus {
    // Refer: http://hl7.org/fhir/R4/valueset-care-plan-activity-status.html for some mapping
    // guidelines
    return when (resource.status) {
      Task.TaskStatus.ACCEPTED -> CarePlan.CarePlanActivityStatus.SCHEDULED
      Task.TaskStatus.DRAFT -> CarePlan.CarePlanActivityStatus.NOTSTARTED
      Task.TaskStatus.REQUESTED -> CarePlan.CarePlanActivityStatus.NOTSTARTED
      Task.TaskStatus.RECEIVED -> CarePlan.CarePlanActivityStatus.NOTSTARTED
      Task.TaskStatus.REJECTED -> CarePlan.CarePlanActivityStatus.STOPPED
      Task.TaskStatus.READY -> CarePlan.CarePlanActivityStatus.NOTSTARTED
      Task.TaskStatus.CANCELLED -> CarePlan.CarePlanActivityStatus.CANCELLED
      Task.TaskStatus.INPROGRESS -> CarePlan.CarePlanActivityStatus.INPROGRESS
      Task.TaskStatus.ONHOLD -> CarePlan.CarePlanActivityStatus.ONHOLD
      Task.TaskStatus.FAILED -> CarePlan.CarePlanActivityStatus.STOPPED
      Task.TaskStatus.COMPLETED -> CarePlan.CarePlanActivityStatus.COMPLETED
      Task.TaskStatus.ENTEREDINERROR -> CarePlan.CarePlanActivityStatus.ENTEREDINERROR
      Task.TaskStatus.NULL -> CarePlan.CarePlanActivityStatus.NULL
      else -> CarePlan.CarePlanActivityStatus.NULL
    }
  }

  private inline fun <reified T : Any> getPrivateProperty(property: String, obj: T): Any? {
    return T::class
      .declaredMemberProperties
      .find { it.name == property }!!
      .apply { isAccessible = true }
      .get(obj)
  }
}
