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

package org.smartregister.fhircore.engine.util.helper

import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.AdverseEvent
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Consent
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.EpisodeOfCare
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PlanDefinition
import org.hl7.fhir.r4.model.ResourceFactory
import org.hl7.fhir.r4.model.RiskAssessment.RiskAssessmentPredictionComponent
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.model.Timing
import org.hl7.fhir.r4.terminologies.ConceptMapEngine
import org.hl7.fhir.r4.utils.StructureMapUtilities.ITransformerServices
import timber.log.Timber

/**
 * Copied from
 * https://github.com/hapifhir/org.hl7.fhir.core/blob/master/org.hl7.fhir.validation/src/main/java/org/hl7/fhir/validation/TransformSupportServices.java
 * and adapted for R4. This class enables us to implement generation of Types and Resources not in
 * the original Hapi Fhir source code here
 * https://github.com/hapifhir/org.hl7.fhir.core/blob/master/org.hl7.fhir.r4/src/main/java/org/hl7/fhir/r4/model/ResourceFactory.java.
 * The missing Types and Resources are internal model types eg RiskAssessment.Prediction,
 * Immunization.Reaction
 */
@Singleton
class TransformSupportServices @Inject constructor(val simpleWorkerContext: SimpleWorkerContext) :
  ITransformerServices {

  val outputs: MutableList<Base> = mutableListOf()

  override fun log(message: String) {
    Timber.i(message)
  }

  @Throws(FHIRException::class)
  override fun createType(appInfo: Any, name: String): Base {
    return when (name) {
      "RiskAssessment_Prediction" -> RiskAssessmentPredictionComponent()
      "Immunization_AppliedProtocol" -> Immunization.ImmunizationProtocolAppliedComponent()
      "Immunization_Reaction" -> Immunization.ImmunizationReactionComponent()
      "EpisodeOfCare_Diagnosis" -> EpisodeOfCare.DiagnosisComponent()
      "Encounter_Diagnosis" -> Encounter.DiagnosisComponent()
      "Encounter_Participant" -> Encounter.EncounterParticipantComponent()
      "Encounter_Location" -> Encounter.EncounterLocationComponent()
      "CarePlan_Activity" -> CarePlan.CarePlanActivityComponent()
      "CarePlan_ActivityDetail" -> CarePlan.CarePlanActivityDetailComponent()
      "Patient_Link" -> Patient.PatientLinkComponent()
      "Timing_Repeat" -> Timing.TimingRepeatComponent()
      "PlanDefinition_Action" -> PlanDefinition.PlanDefinitionActionComponent()
      "Group_Characteristic" -> Group.GroupCharacteristicComponent()
      "Observation_Component" -> Observation.ObservationComponentComponent()
      "Task_Input" -> Task.ParameterComponent()
      "Task_Output" -> Task.TaskOutputComponent()
      "Task_Restriction" -> Task.TaskRestrictionComponent()
      "AdverseEvent_SuspectEntity" -> AdverseEvent.AdverseEventSuspectEntityComponent()
      "AdverseEvent_SuspectEntityCausality" ->
        AdverseEvent.AdverseEventSuspectEntityCausalityComponent()
      "Location_Position" -> Location.LocationPositionComponent()
      "List_Entry" -> ListResource.ListEntryComponent()
      "Consent_Policy" -> Consent.ConsentPolicyComponent()
      "Consent_Verification" -> Consent.ConsentVerificationComponent()
      "Consent_Provision" -> Consent.provisionComponent()
      "Consent_ProvisionActor" -> Consent.provisionActorComponent()
      "Consent_ProvisionData" -> Consent.provisionDataComponent()
      else -> ResourceFactory.createResourceOrType(name)
    }
  }

  override fun createResource(appInfo: Any, res: Base, atRootofTransform: Boolean): Base {
    if (atRootofTransform) outputs.add(res)
    return res
  }

  @Throws(FHIRException::class)
  override fun translate(appInfo: Any, source: Coding, conceptMapUrl: String): Coding {
    val cme = ConceptMapEngine(simpleWorkerContext)
    return cme.translate(source, conceptMapUrl)
  }

  @Throws(FHIRException::class)
  override fun resolveReference(appContext: Any, url: String): Base {
    throw FHIRException("resolveReference is not supported yet")
  }

  @Throws(FHIRException::class)
  override fun performSearch(appContext: Any, url: String): List<Base> {
    throw FHIRException("performSearch is not supported yet")
  }
}
