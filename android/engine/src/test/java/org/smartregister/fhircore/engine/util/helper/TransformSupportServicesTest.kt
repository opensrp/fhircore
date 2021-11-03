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

package org.smartregister.fhircore.engine.util.helper

import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.RiskAssessment
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 24-09-2021. */
class TransformSupportServicesTest : RobolectricTest() {

  lateinit var transformSupportServices: TransformSupportServices

  @Before
  fun setUp() {
    transformSupportServices = TransformSupportServices(mutableListOf(), SimpleWorkerContext())
  }

  @Test
  fun `createType() should return RiskAssessmentPrediction when given RiskAssessment_Prediction`() {
    Assert.assertTrue(
      transformSupportServices.createType("", "RiskAssessment_Prediction") is
        RiskAssessment.RiskAssessmentPredictionComponent
    )
  }

  @Test
  fun `createType() should return ImmunizationProtocol when given Immunization_VaccinationProtocol`() {
    Assert.assertTrue(
      transformSupportServices.createType("", "Immunization_VaccinationProtocol") is
        Immunization.ImmunizationProtocolAppliedComponent
    )
  }

  @Test
  fun `createType() should return ImmunizationReaction when given Immunization_Reaction`() {
    Assert.assertTrue(
      transformSupportServices.createType("", "Immunization_Reaction") is
        Immunization.ImmunizationReactionComponent
    )
  }
}
