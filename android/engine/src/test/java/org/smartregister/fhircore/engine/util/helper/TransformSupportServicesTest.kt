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

import io.mockk.mockk
import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Consent
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.EpisodeOfCare
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.RiskAssessment
import org.hl7.fhir.r4.model.TimeType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 24-09-2021. */
class TransformSupportServicesTest : RobolectricTest() {

  lateinit var transformSupportServices: TransformSupportServices

  @Before
  fun setUp() {
    transformSupportServices = TransformSupportServices(mockk())
  }

  @Test
  fun `createType() should return RiskAssessmentPrediction when given RiskAssessment_Prediction`() {
    Assert.assertTrue(
      transformSupportServices.createType("", "RiskAssessment_Prediction")
        is RiskAssessment.RiskAssessmentPredictionComponent,
    )
  }

  @Test
  fun `createType() should return ImmunizationProtocol when given Immunization_VaccinationProtocol`() {
    Assert.assertTrue(
      transformSupportServices.createType("", "Immunization_AppliedProtocol")
        is Immunization.ImmunizationProtocolAppliedComponent,
    )
  }

  @Test
  fun `createType() should return ImmunizationReaction when given Immunization_Reaction`() {
    Assert.assertTrue(
      transformSupportServices.createType("", "Immunization_Reaction")
        is Immunization.ImmunizationReactionComponent,
    )
  }

  @Test
  fun `createType() should return Diagnosis when given EpisodeOfCare_Diagnosis`() {
    Assert.assertTrue(
      transformSupportServices.createType("", "EpisodeOfCare_Diagnosis")
        is EpisodeOfCare.DiagnosisComponent,
    )
  }

  @Test
  fun `createType() should return Diagnosis when given Encounter_Diagnosis`() {
    Assert.assertTrue(
      transformSupportServices.createType("", "Encounter_Diagnosis")
        is Encounter.DiagnosisComponent,
    )
  }

  @Test
  fun `createType() should return EncounterParticipant when given Encounter_Participant`() {
    Assert.assertTrue(
      transformSupportServices.createType("", "Encounter_Participant")
        is Encounter.EncounterParticipantComponent,
    )
  }

  @Test
  fun `createType() should return CarePlanActivity when given CarePlan_Activity`() {
    Assert.assertTrue(
      transformSupportServices.createType("", "CarePlan_Activity")
        is CarePlan.CarePlanActivityComponent,
    )
  }

  @Test
  fun `createType() should return CarePlanActivityDetail when given CarePlan_ActivityDetail`() {
    Assert.assertTrue(
      transformSupportServices.createType("", "CarePlan_ActivityDetail")
        is CarePlan.CarePlanActivityDetailComponent,
    )
  }

  @Test
  fun `createType() should return PatientLink when given Patient_Link`() {
    Assert.assertTrue(
      transformSupportServices.createType("", "Patient_Link") is Patient.PatientLinkComponent,
    )
  }

  @Test
  fun `createType() should return GroupCharacteristicComponent when given Group_Characteristic`() {
    Assert.assertTrue(
      transformSupportServices.createType("", "Group_Characteristic")
        is Group.GroupCharacteristicComponent,
    )
  }

  @Test
  fun `createType() should return ObservationComponentComponent when given Observation_Component`() {
    Assert.assertTrue(
      transformSupportServices.createType("", "Observation_Component")
        is Observation.ObservationComponentComponent,
    )
  }

  @Test
  fun `createType() should return ConsentPolicyComponent when given Consent_Policy`() {
    Assert.assertTrue(
      transformSupportServices.createType("", "Consent_Policy") is Consent.ConsentPolicyComponent,
    )
  }

  @Test
  fun `createType() should return ConsentVerificationComponent when given Consent_Verification`() {
    Assert.assertTrue(
      transformSupportServices.createType("", "Consent_Verification")
        is Consent.ConsentVerificationComponent,
    )
  }

  @Test
  fun `createType() should return provisionComponent when given Consent_Provision`() {
    Assert.assertTrue(
      transformSupportServices.createType("", "Consent_Provision") is Consent.provisionComponent,
    )
  }

  @Test
  fun `createType() should return provisionActorComponent when given Consent_ProvisionActor`() {
    Assert.assertTrue(
      transformSupportServices.createType("", "Consent_ProvisionActor")
        is Consent.provisionActorComponent,
    )
  }

  @Test
  fun `createType() should return provisionDataComponent when given Consent_ProvisionData`() {
    Assert.assertTrue(
      transformSupportServices.createType("", "Consent_ProvisionData")
        is Consent.provisionDataComponent,
    )
  }

  @Test
  fun `createType() should return Time when given time`() {
    Assert.assertTrue(transformSupportServices.createType("", "time") is TimeType)
  }

  @Test
  fun `createResource() should add resource into output when given Patient and atRootOfTransForm as True`() {
    Assert.assertEquals(transformSupportServices.outputs.size, 0)
    transformSupportServices.createResource("", Patient(), true)
    Assert.assertEquals(transformSupportServices.outputs.size, 1)
  }

  @Test
  fun `createResource() should not add resource into output when given Patient and atRootOfTransForm as False`() {
    Assert.assertEquals(transformSupportServices.outputs.size, 0)
    transformSupportServices.createResource("", Patient(), false)
    Assert.assertEquals(transformSupportServices.outputs.size, 0)
  }

  @Test
  fun `resolveReference() should throw FHIRException this is not supported yet when given url`() {
    Assert.assertThrows("resolveReference is not supported yet", FHIRException::class.java) {
      transformSupportServices.resolveReference("", "https://url.com")
    }
  }

  @Test
  fun `performSearch() should throw FHIRException this is not supported yet when given url`() {
    Assert.assertThrows("performSearch is not supported yet", FHIRException::class.java) {
      transformSupportServices.performSearch("", "https://url.com")
    }
  }
}
