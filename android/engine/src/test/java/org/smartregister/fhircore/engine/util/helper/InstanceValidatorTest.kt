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

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.validation.FhirValidator
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.util.extension.errorMessages

class InstanceValidatorTest {

  private lateinit var validator: FhirValidator

  @Before
  fun setUp() {
    val ctx = FhirContext.forR4Cached()
    validator = ctx.newValidator()

    val validatorModule = FhirInstanceValidator(ctx)
    validator.registerValidatorModule(validatorModule)
  }

  @Test
  fun validateCarePlan() {
    val bundle =
      "{\"resourceType\": \"Bundle\", \"type\": \"collection\", \"entry\": [{\"resource\": {\"resourceType\": \"CarePlan\", \"intent\": \"plan\", \"subject\": {\"reference\": \"Patient/TEST_PATIENT\"}}}]}"
    val carePlan =
      "{\"resourceType\": \"CarePlan\", \"status\": \"active\", \"intent\": \"plan\", \"subject\": {\"reference\": \"Patient/TEST_PATIENT\"}}"
    val result = validator.validateWithResult(carePlan)

    // The result object now contains the validation results
    // The result object now contains the validation results
    if (!result.isSuccessful) print(result.errorMessages)
    Assert.assertTrue(result.isSuccessful)
  }
}
