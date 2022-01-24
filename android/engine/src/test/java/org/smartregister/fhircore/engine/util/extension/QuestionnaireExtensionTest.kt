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

package org.smartregister.fhircore.engine.util.extension

import dagger.hilt.android.testing.HiltAndroidTest
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
class QuestionnaireExtensionTest : RobolectricTest() {

  @Test
  fun testIsExtractionCandidateShouldVerifyAllScenarios() {
    Assert.assertFalse(Questionnaire().isExtractionCandidate())

    Assert.assertTrue(
      Questionnaire()
        .apply {
          addExtension().apply {
            url =
              "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-targetStructureMap"
            setValue(CanonicalType("test"))
          }
        }
        .isExtractionCandidate()
    )

    Assert.assertTrue(
      Questionnaire()
        .apply {
          addExtension().apply {
            url =
              "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-itemExtractionContext"
          }
        }
        .isExtractionCandidate()
    )
  }

  @Test
  fun testCqfLibraryIdShouldReturnExpectedUrl() {
    Assert.assertTrue(Questionnaire().cqfLibraryIds().isEmpty())

    Assert.assertEquals(
      "",
      Questionnaire()
        .apply {
          addExtension().apply {
            url = "cqf-library"
            setValue(StringType("Library/"))
          }
        }
        .cqfLibraryIds()
        .first()
    )

    Assert.assertEquals(
      "112233",
      Questionnaire()
        .apply {
          addExtension().apply {
            url = "cqf-library"
            setValue(StringType("Library/112233"))
          }
        }
        .cqfLibraryIds()
        .first()
    )
  }

  @Test
  fun testShouldFindMatchingItems() {

    val questionnaire =
      Questionnaire().apply {
        addItem().apply {
          linkId = "family"
          addInitial(Questionnaire.QuestionnaireItemInitialComponent(StringType("Mr")))
        }
      }

    Assert.assertEquals("Mr", questionnaire.find("family")?.initialFirstRep?.valueStringType?.value)

    questionnaire.find("family")?.addItem()?.apply {
      linkId = "name"
      addInitial(Questionnaire.QuestionnaireItemInitialComponent(StringType("John")))
    }

    Assert.assertEquals("John", questionnaire.find("name")?.initialFirstRep?.valueStringType?.value)
  }
}
