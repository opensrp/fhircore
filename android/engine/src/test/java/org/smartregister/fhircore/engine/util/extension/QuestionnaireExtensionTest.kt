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
import org.hl7.fhir.r4.model.QuestionnaireResponse
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

  @Test
  fun testQuestionnaireResponseItemComponentAsLabel() {
    val item = QuestionnaireResponse().addItem().apply { linkId = "my_test_link" }

    Assert.assertEquals("My test link: ", item.asLabel())
  }

  @Test
  fun testShouldFindMatchingItemsByFieldType() {

    val questionnaire =
      Questionnaire().apply {
        id = "12345"
        item =
          listOf(
            Questionnaire.QuestionnaireItemComponent().apply {
              type = Questionnaire.QuestionnaireItemType.CHOICE
              linkId = "q1-gender"
              definition = "some-element-definition-identifier"
            },
            Questionnaire.QuestionnaireItemComponent().apply {
              type = Questionnaire.QuestionnaireItemType.CHOICE
              linkId = "q2-marital-status"
            },
            Questionnaire.QuestionnaireItemComponent().apply {
              type = Questionnaire.QuestionnaireItemType.DATE
              linkId = "q3-date"
            }
          )
      }

    Assert.assertEquals(3, questionnaire.item.size)

    val filtered =
      questionnaire.find(FieldType.TYPE, Questionnaire.QuestionnaireItemType.CHOICE.name)

    Assert.assertEquals(2, filtered.size)

    val filteredDates =
      questionnaire.find(FieldType.TYPE, Questionnaire.QuestionnaireItemType.DATE.name)

    Assert.assertEquals(1, filteredDates.size)

    val filteredDefinitions =
      questionnaire.find(FieldType.DEFINITION, "some-element-definition-identifier")

    Assert.assertEquals(1, filteredDefinitions.size)
  }
}
