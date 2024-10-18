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

package org.smartregister.fhircore.engine.util.validation

import ca.uhn.fhir.validation.FhirValidator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import timber.log.Timber

@HiltAndroidTest
class ResourceValidationRequestTest : RobolectricTest() {
  @get:Rule var hiltRule = HiltAndroidRule(this)

  @Inject lateinit var validator: FhirValidator

  @Inject lateinit var resourceValidationRequestHandler: ResourceValidationRequestHandler

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @Test
  fun testHandleResourceValidationRequestValidatesInvalidResourceLoggingErrors() = runTest {
    mockkObject(Timber)
    val resource =
      CarePlan().apply {
        id = "test-careplan"
        status = CarePlan.CarePlanStatus.ACTIVE
        intent = CarePlan.CarePlanIntent.PLAN
        subject = Reference("f4bd3e29-f0f8-464e-97af-923b83664ccc")
      }
    val validationRequest = ResourceValidationRequest(resource)
    resourceValidationRequestHandler.handleResourceValidationRequest(validationRequest)
    verify {
      Timber.e(
        withArg<String> {
          Assert.assertTrue(
            it.contains(
              "CarePlan.subject - The syntax of the reference 'f4bd3e29-f0f8-464e-97af-923b83664ccc' looks incorrect, and it should be checked -- (WARNING)",
            ),
          )
        },
      )
    }
    unmockkObject(Timber)
  }

  @Test
  fun testCheckResourceValidValidatesResourceStructureWhenCarePlanResourceInvalid() = runTest {
    val basicCarePlan = CarePlan()
    val resultsWrapper = validator.checkResources(listOf(basicCarePlan))
    Assert.assertTrue(
      resultsWrapper.errorMessages.any {
        it.contains(
          "CarePlan.status: minimum required = 1, but only found 0",
          ignoreCase = true,
        )
      },
    )
    Assert.assertTrue(
      resultsWrapper.errorMessages.any {
        it.contains(
          "CarePlan.intent: minimum required = 1, but only found 0",
          ignoreCase = true,
        )
      },
    )
  }

  @Test
  fun testCheckResourceValidValidatesReferenceType() = runTest {
    val carePlan =
      CarePlan().apply {
        status = CarePlan.CarePlanStatus.ACTIVE
        intent = CarePlan.CarePlanIntent.PLAN
        subject = Reference("Task/unknown")
      }
    val resultsWrapper = validator.checkResources(listOf(carePlan))
    Assert.assertEquals(1, resultsWrapper.errorMessages.size)
    Assert.assertTrue(
      resultsWrapper.errorMessages
        .first()
        .contains(
          "CarePlan.subject - The type 'Task' implied by the reference URL Task/unknown is not a valid Target for this element (must be one of [Group, Patient])",
          ignoreCase = true,
        ),
    )
  }

  @Test
  fun testCheckResourceValidValidatesReferenceWithNoType() = runTest {
    val carePlan =
      CarePlan().apply {
        status = CarePlan.CarePlanStatus.ACTIVE
        intent = CarePlan.CarePlanIntent.PLAN
        subject = Reference("unknown")
      }
    val resultsWrapper = validator.checkResources(listOf(carePlan))
    Assert.assertEquals(1, resultsWrapper.errorMessages.size)
    Assert.assertTrue(
      resultsWrapper.errorMessages
        .first()
        .contains(
          "CarePlan.subject - The syntax of the reference 'unknown' looks incorrect, and it should be checked",
          ignoreCase = true,
        ),
    )
  }

  @Test
  fun testCheckResourceValidValidatesResourceCorrectly() = runTest {
    val patient = Patient()
    val carePlan =
      CarePlan().apply {
        status = CarePlan.CarePlanStatus.ACTIVE
        intent = CarePlan.CarePlanIntent.PLAN
        subject = Reference(patient)
      }
    val resultsWrapper = validator.checkResources(listOf(carePlan))
    Assert.assertEquals(1, resultsWrapper.errorMessages.size)
    Assert.assertTrue(resultsWrapper.errorMessages.first().isBlank())
  }
}
