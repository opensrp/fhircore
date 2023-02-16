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

package org.smartregister.fhircore.engine.util.fhirpath

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
class FhirPathDataExtractorTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @Inject lateinit var fhirPathDataExtractor: FhirPathDataExtractor

  private val patientDeceased = "patientDeceased"

  private val familyName = "familyName"

  private val expressions =
    mapOf(
      Pair(patientDeceased, "Patient.deceased.exists()"),
      Pair(familyName, "Patient.name.family & ' Family'")
    )

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
  }

  @Test
  fun testExtractDataShouldReturnMap() {
    val patient =
      Patient().apply {
        deceased = BooleanType(false)
        addName().family = "Doe"
      }
    val extractedData: Map<String, List<Base>> =
      fhirPathDataExtractor.extractData(patient, expressions)
    Assert.assertTrue(extractedData.isNotEmpty())

    Assert.assertTrue(extractedData.containsKey(patientDeceased))
    Assert.assertEquals(1, extractedData[patientDeceased]?.size)
    Assert.assertTrue((extractedData[patientDeceased]?.first() as BooleanType).value)
    Assert.assertTrue(extractedData.containsKey(familyName))
    val familyNameValue = extractedData[familyName]
    Assert.assertEquals(1, familyNameValue?.size)
    Assert.assertEquals("Doe Family", (familyNameValue?.first() as StringType).value)
  }

  @Test
  fun extractValueWithBasePatientAndFamilyNameExpressionShouldReturnStringValueOfFamily() {
    val patient =
      Patient().apply {
        deceased = BooleanType(false)
        addName().family = "Doe"
      }
    val expression = "Patient.name.family"
    Assert.assertEquals("Doe", fhirPathDataExtractor.extractValue(patient, expression))
  }

  @Test
  fun extractValueWithPatientWithNoGivenNameAndExpressionGivenNameShouldReturnEmptyString() {
    val patientNoGivenName =
      Patient().apply {
        deceased = BooleanType(false)
        addName().family = "Doe"
      }
    val expression = "Patient.name.given" // would evaluate to empty
    val result = fhirPathDataExtractor.extractValue(patientNoGivenName, expression)
    Assert.assertTrue(result.isEmpty())
  }

  @Test
  fun extractValueWithNullResourceShouldReturnEmptyString() {
    val expression = "Patient.name.given" // would evaluate to empty
    val result = fhirPathDataExtractor.extractValue(null, expression)
    Assert.assertTrue(result.isEmpty())
  }
}
