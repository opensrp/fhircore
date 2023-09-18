/*
 * Copyright 2021-2023 Ona Systems, Inc
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
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.knowledge.KnowledgeManager
import com.google.android.fhir.search.search
import com.google.android.fhir.workflow.FhirOperator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.MetadataResource
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PlanDefinition
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task

@Singleton
class WorkflowCarePlanGenerator
@Inject
constructor(
  val knowledgeManager: KnowledgeManager,
  val fhirOperator: FhirOperator,
  val fhirEngine: FhirEngine,
  @ApplicationContext val context: Context,
) {

  private var cqlLibraryIdList = ArrayList<String>()
  private val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()

  private fun writeToFile(resource: Resource): File {
    val fileName =
      if (resource is MetadataResource && resource.name != null) {
        resource.name
      } else {
        resource.idElement.idPart
      }
    return File(context.filesDir, fileName).apply {
      writeText(jsonParser.encodeResourceToString(resource))
    }
  }

  /**
   * Extracts resources present in PlanDefinition.contained field
   *
   * We cannot use $data-requirements on the [PlanDefinition] yet. So, we assume that all knowledge
   * resources required to $apply a [PlanDefinition] are present within `PlanDefinition.contained`
   *
   * @param planDefinition PlanDefinition resource for which dependent resources are extracted
   */
  suspend fun getPlanDefinitionDependentResources(
    planDefinition: PlanDefinition,
  ): Collection<Resource> {
    var bundleCollection: Collection<Resource> = mutableListOf()

    for (resource in planDefinition.contained) {
      resource.meta.lastUpdated = planDefinition.meta.lastUpdated
      if (resource is Library) {
        cqlLibraryIdList.add(IdType(resource.id).idPart)
      }
      knowledgeManager.install(writeToFile(resource))

      bundleCollection += resource
    }
    return bundleCollection
  }

  /**
   * Knowledge resources are loaded from [FhirEngine] and installed so that they may be used when
   * running $apply on a [PlanDefinition]
   */
  private suspend fun loadPlanDefinitionResourcesFromDb() {
    // Load Library resources
    val availableCqlLibraries = fhirEngine.search<Library> {}
    val availablePlanDefinitions = fhirEngine.search<PlanDefinition> {}
    for (cqlLibrary in availableCqlLibraries) {
      fhirOperator.loadLib(cqlLibrary)
      knowledgeManager.install(writeToFile(cqlLibrary))
      cqlLibraryIdList.add(IdType(cqlLibrary.id).idPart)
    }
    for (planDefinition in availablePlanDefinitions) {
      getPlanDefinitionDependentResources(planDefinition)
    }
  }

  /**
   * Executes $apply on a [PlanDefinition] for a [Patient] and creates the request resources as per
   * the proposed [CarePlan]
   *
   * @param planDefinitionId PlanDefinition resource ID for which $apply is run
   * @param patient Patient resource for which the [PlanDefinition] $apply is run
   * @param requestResourceConfigs List of configurations that need to be applied to the request
   *   resources as a result of the proposed [CarePlan]
   */
  suspend fun applyPlanDefinitionOnPatient(
    planDefinition: PlanDefinition,
    patient: Patient,
    data: Bundle = Bundle(),
    output: CarePlan,
  ) {
    val patientId = IdType(patient.id).idPart
    val planDefinitionId = IdType(planDefinition.id).idPart

    if (cqlLibraryIdList.isEmpty()) {
      loadPlanDefinitionResourcesFromDb()
    }

    val carePlanProposal =
      fhirOperator.generateCarePlan(planDefinitionId = planDefinitionId, patientId = patientId)
        as CarePlan

    // Accept the proposed (transient) CarePlan by default and add tasks to the CarePlan of record
    acceptCarePlan(carePlanProposal, output)
  }

  /** Update the [CarePlan] to include a reference to the FHIR-define protocol or guideline */
  private fun updateCarePlanWithProtocol(carePlan: CarePlan, uris: List<CanonicalType>) {
    for (uri in uris) carePlan.addInstantiatesCanonical(uri.value)
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
        "ServiceRequest" -> TODO("Not supported yet")
        "MedicationRequest" -> TODO("Not supported yet")
        "SupplyRequest" -> TODO("Not supported yet")
        "Procedure" -> TODO("Not supported yet")
        "DiagnosticReport" -> TODO("Not supported yet")
        "Communication" -> TODO("Not supported yet")
        "CommunicationRequest" -> TODO("Not supported yet")
        else -> TODO("Not a valid request resource")
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
        "Task" -> {
          fhirEngine.create(resource)
          createdRequestResources.add(resource)
        }
        "ServiceRequest" -> TODO("Not supported yet")
        "MedicationRequest" -> TODO("Not supported yet")
        "SupplyRequest" -> TODO("Not supported yet")
        "Procedure" -> TODO("Not supported yet")
        "DiagnosticReport" -> TODO("Not supported yet")
        "Communication" -> TODO("Not supported yet")
        "CommunicationRequest" -> TODO("Not supported yet")
        "RequestGroup" -> {}
        else -> TODO("Not a valid request resource")
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

    // fhirEngine.create(carePlanOfRecord)
    // linkRequestResourcesToCarePlan(carePlanOfRecord, resourceList)
  }

  /** Update status of a [CarePlan] activity */
  private suspend fun updateCarePlanStatus(
    carePlan: CarePlan,
    requestedActivityResource: Resource,
    carePlanActivityStatus: CarePlan.CarePlanActivityStatus,
    outcomeReferences: List<Reference>,
  ) {
    if (carePlan.isEmpty) return
    for (activity in carePlan.activity) {
      if (
        activity.reference.reference.equals(
          requestedActivityResource.fhirType() + "/" + IdType(requestedActivityResource.id).idPart,
        )
      ) {
        activity.detail.status = carePlanActivityStatus
        activity.outcomeReference = outcomeReferences
        fhirEngine.update(carePlan)
        break
      }
    }
  }

  /**
   * Find and update the status of the [CarePlan] activity as per the corresponding request resource
   * status
   */
  suspend fun updateCarePlanActivity(
    requestResource: Resource,
    requestResourceStatus: String,
    outcomeReferences: List<Reference>,
    updateCarePlan: Boolean = true,
  ) {
    val carePlanActivityStatus: CarePlan.CarePlanActivityStatus
    val carePlan: CarePlan
    when (requestResource.fhirType()) {
      "Task" -> {
        updateRequestResourceStatus(requestResource as Task, requestResourceStatus)
        if (updateCarePlan) {
          carePlanActivityStatus = mapRequestResourceStatusToCarePlanStatus(requestResource)
          carePlan =
            if (requestResource.hasBasedOn()) {
              fhirEngine.get(
                ResourceType.CarePlan,
                IdType(requestResource.basedOnFirstRep.referenceElement.value).idPart,
              ) as CarePlan
            } else {
              return
            }
          updateCarePlanStatus(carePlan, requestResource, carePlanActivityStatus, outcomeReferences)
        }
      }
      "ServiceRequest" -> TODO("Not supported yet")
      "MedicationRequest" -> TODO("Not supported yet")
      "SupplyRequest" -> TODO("Not supported yet")
      "Procedure" -> TODO("Not supported yet")
      "DiagnosticReport" -> TODO("Not supported yet")
      "Communication" -> TODO("Not supported yet")
      "CommunicationRequest" -> TODO("Not supported yet")
      "RequestGroup" -> {}
      else -> TODO("Not a valid request resource")
    }
  }

  /** Update [Task] status */
  suspend fun updateRequestResourceStatus(resource: Task, status: String) {
    resource.status = Task.TaskStatus.valueOf(status)
    fhirEngine.update(resource)
  }

  /** Map [Task] status to [CarePlan] status */
  fun mapRequestResourceStatusToCarePlanStatus(
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
}
