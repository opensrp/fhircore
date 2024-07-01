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

package org.smartregister.fhircore.engine.util.extension

import ca.uhn.fhir.validation.FhirValidator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.spyk
import io.mockk.verify
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
class FhirValidatorExtensionTest : RobolectricTest() {

  @get:Rule var hiltRule = HiltAndroidRule(this)

  @Inject lateinit var validator: FhirValidator

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @Test
  fun testCheckResourceValidRunsNoValidationWhenBuildTypeIsNotDebug() = runTest {
    val basicResource = CarePlan()
    val fhirValidatorSpy = spyk(validator)
    val results = fhirValidatorSpy.checkResourceValid(basicResource, isDebug = false)
    Assert.assertTrue(results.isEmpty())
    verify(exactly = 0) { fhirValidatorSpy.validateWithResult(any<IBaseResource>()) }
    verify(exactly = 0) { fhirValidatorSpy.validateWithResult(any<String>()) }
  }

  @Test
  fun testCheckResourceValidValidatesResourceStructureWhenCarePlanResourceInvalid() = runTest {
    val basicCarePlan = CarePlan()
    val results = validator.checkResourceValid(basicCarePlan)
    Assert.assertFalse(results.isEmpty())
    Assert.assertTrue(
      results.any {
        it.errorMessages.contains(
          "CarePlan.status: minimum required = 1, but only found 0",
          ignoreCase = true,
        )
      },
    )
    Assert.assertTrue(
      results.any {
        it.errorMessages.contains(
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
    val results = validator.checkResourceValid(carePlan)
    Assert.assertFalse(results.isEmpty())
    Assert.assertEquals(1, results.size)
    Assert.assertTrue(
      results
        .first()
        .errorMessages
        .contains(
          "The type 'Task' implied by the reference URL Task/unknown is not a valid Target for this element (must be one of [Group, Patient]) - CarePlan.subject",
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
    val results = validator.checkResourceValid(carePlan)
    Assert.assertFalse(results.isEmpty())
    Assert.assertEquals(1, results.size)
    Assert.assertTrue(
      results
        .first()
        .errorMessages
        .contains(
          "The syntax of the reference 'unknown' looks incorrect, and it should be checked - CarePlan.subject",
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
    val results = validator.checkResourceValid(carePlan)
    Assert.assertFalse(results.isEmpty())
    Assert.assertEquals(1, results.size)
    Assert.assertTrue(results.first().errorMessages.isBlank())
  }
}
