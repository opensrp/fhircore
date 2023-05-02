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

package org.smartregister.fhircore.engine.util.extension

import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.DecimalType
import org.hl7.fhir.r4.model.Enumerations.DataType
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.TimeType
import org.hl7.fhir.r4.model.UriType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.domain.model.ActionParameter

class QuestionnaireExtensionTest {
  private lateinit var questionniare: Questionnaire
  private lateinit var questionniareResponse: QuestionnaireResponse
  private lateinit var questionniareResponseItemComponent:
    QuestionnaireResponse.QuestionnaireResponseItemComponent

  @Before
  fun setup() {
    questionniare = Questionnaire()
    questionniareResponse = QuestionnaireResponse()
    questionniareResponseItemComponent = QuestionnaireResponse.QuestionnaireResponseItemComponent()
  }

  @Test
  fun testAsLabelEmptyStringIfLinkIdNull() {
    val label = questionniareResponseItemComponent.asLabel()
    Assert.assertEquals("", label)
  }

  @Test
  fun testAsLabelCapitalizeReplaceUnderscoresWithSpacesAndAddColonSpace() {
    questionniareResponseItemComponent.linkId = "a_b_c"
    val label = questionniareResponseItemComponent.asLabel()
    Assert.assertEquals("A b c: ", label)
  }

  @Test
  fun testAsLabelReplaceUnderscoresWithSpacesAndAddColonSpace() {
    questionniareResponseItemComponent.linkId = "A_b_c"
    val label = questionniareResponseItemComponent.asLabel()
    Assert.assertEquals("A b c: ", label)
  }

  @Test
  fun testIsExtractionCandidateEmptyFalse() {
    Assert.assertEquals(false, questionniare.isExtractionCandidate())
  }

  @Test
  fun testIsExtractionCandidatePartialStringFalse() {
    questionniare.extension.add(Extension("https://questionnaire-itemExtractionContext/abc"))
    Assert.assertEquals(false, questionniare.isExtractionCandidate())
  }

  @Test
  fun testIsExtractionCandidateTrue() {
    questionniare.extension.add(Extension("https://sdc-questionnaire-itemExtractionContext/abc"))
    Assert.assertEquals(true, questionniare.isExtractionCandidate())
  }

  @Test
  fun testCqfLibraryIdsEmpty() {
    Assert.assertEquals(emptyList<Extension>(), questionniare.cqfLibraryIds())
  }

  @Test
  fun testCqfLibraryIdsIncludesCqfLibraryNullValue() {
    val extension = Extension("https://cqf-library/abc", null)
    questionniare.extension.add(extension)
    Assert.assertEquals(emptyList<Extension>(), questionniare.cqfLibraryIds())
  }

  @Test
  fun testCqfLibraryIdsIncludesCqfLibraryEmptyValue() {
    val value = StringType("")
    val extension = Extension("https://cqf-library/abc", value)
    questionniare.extension.add(extension)
    Assert.assertEquals(arrayListOf(""), questionniare.cqfLibraryIds())
  }

  @Test
  fun testCqfLibraryIdsIncludesCqfLibraryRemovesLibraryPrefix() {
    val value = StringType("Library/word")
    val extension = Extension("https://cqf-library/abc", value)
    questionniare.extension.add(extension)
    Assert.assertEquals(arrayListOf("word"), questionniare.cqfLibraryIds())
  }

  @Test
  fun testFindSubjectWithNull() {
    Assert.assertEquals(null, questionniareResponse.findSubject(null))
  }

  @Test
  fun testFindSubjectMatchingId() {
    val id = "1234"
    val patient = Faker.buildPatient(id)
    val bundleEntryComponent = Bundle.BundleEntryComponent().apply { resource = patient }
    val bundle = Bundle().apply { entry = listOf(bundleEntryComponent) }
    questionniareResponse.subject.reference = id
    Assert.assertEquals(patient, questionniareResponse.findSubject(bundle))
  }

  @Test
  fun testFindSubjectMismatchingId() {
    val id = "1234"
    val patient = Faker.buildPatient(id)
    val bundleEntryComponent = Bundle.BundleEntryComponent().apply { resource = patient }
    val bundle = Bundle().apply { entry = listOf(bundleEntryComponent) }
    questionniareResponse.subject.reference = "5678"
    Assert.assertEquals(null, questionniareResponse.findSubject(bundle))
  }

  @Test
  fun testFindWithNull() {
    Assert.assertEquals(null, questionniare.find(""))
  }

  @Test
  fun testFindMismatchingLinkId() {
    val id = "1234"
    val questionnaireItemComponent =
      Questionnaire.QuestionnaireItemComponent().apply { linkId = id }
    questionniare.item = listOf(questionnaireItemComponent)
    Assert.assertEquals(null, questionniare.find("5678"))
  }

  @Test
  fun testFindMatchingLinkId() {
    val id = "1234"
    val questionnaireItemComponent =
      Questionnaire.QuestionnaireItemComponent().apply { linkId = id }
    questionniare.item = listOf(questionnaireItemComponent)
    Assert.assertEquals(questionnaireItemComponent, questionniare.find(id))
  }

  @Test
  fun testFindQuestionnaireResponseItemComponentWhenItem() {
    val id = "1234"
    questionniareResponseItemComponent.item =
      listOf(QuestionnaireResponse.QuestionnaireResponseItemComponent())
    Assert.assertEquals(null, listOf(questionniareResponseItemComponent).find(id, null))
  }

  @Test
  fun testQuestionnaireFindWithFieldTypeDefinition() {
    Assert.assertEquals(
      emptyList<Questionnaire.QuestionnaireItemComponent>(),
      questionniare.find(FieldType.DEFINITION, "")
    )
  }

  @Test
  fun testQuestionnaireFindWithFieldTypeType() {
    Assert.assertEquals(
      emptyList<Questionnaire.QuestionnaireItemComponent>(),
      questionniare.find(FieldType.TYPE, "")
    )
  }

  @Test
  fun testQuestionnaireItemComponentFindWithFieldTypeDefinition() {
    val value = "value"
    val questionnaireItemComponent =
      Questionnaire.QuestionnaireItemComponent().apply { definition = value }
    questionniare.item = listOf(questionnaireItemComponent)
    Assert.assertEquals(
      listOf(questionnaireItemComponent),
      questionniare.find(FieldType.DEFINITION, value)
    )
  }

  @Test
  fun testQuestionnaireItemComponentFindWithFieldTypeType() {
    val value = Questionnaire.QuestionnaireItemType.BOOLEAN
    val questionnaireItemComponent =
      Questionnaire.QuestionnaireItemComponent().apply { type = value }
    questionniare.item = listOf(questionnaireItemComponent)
    Assert.assertEquals(
      listOf(questionnaireItemComponent),
      questionniare.find(FieldType.TYPE, value.toString())
    )
  }

  @Test
  fun testQuestionnaireItemComponentFindRecursive() {
    val id = "1234"
    val innerQuestionnaireItemComponent =
      Questionnaire.QuestionnaireItemComponent().apply { linkId = id }
    val questionnaireItemComponent =
      Questionnaire.QuestionnaireItemComponent().apply {
        item = listOf(innerQuestionnaireItemComponent)
      }
    questionniare.item = listOf(questionnaireItemComponent)
    Assert.assertEquals(
      listOf(innerQuestionnaireItemComponent),
      questionniare.find(FieldType.LINK_ID, id)
    )
  }

  @Test
  fun testFindQuestionnaireItemComponentPrepopulateNoChange() {
    val theLinkId = "linkId"
    val questionnaireItemComponent =
      Questionnaire.QuestionnaireItemComponent().apply { linkId = theLinkId }
    listOf(questionnaireItemComponent).prePopulateInitialValues("", emptyList())
    Assert.assertEquals(
      emptyList<Questionnaire.QuestionnaireItemInitialComponent>(),
      questionnaireItemComponent.initial
    )
  }

  @Test
  fun testFindQuestionnaireItemComponentPrepopulateSetsInitial() {
    val theLinkId = "linkId"
    val prePopulationParams = listOf(ActionParameter("key", linkId = theLinkId, value = "value"))
    val questionnaireItemComponent =
      Questionnaire.QuestionnaireItemComponent().apply { linkId = theLinkId }
    listOf(questionnaireItemComponent).prePopulateInitialValues("!", prePopulationParams)
    Assert.assertNotEquals(
      emptyList<Questionnaire.QuestionnaireItemInitialComponent>(),
      questionnaireItemComponent.initial
    )
  }

  @Test
  fun testFindQuestionnaireItemComponentPrepopulateRecursToSetInitial() {
    val theLinkId = "linkId"
    val prePopulationParams = listOf(ActionParameter("key", linkId = theLinkId, value = "value"))
    val innerQuestionnaireItemComponent =
      Questionnaire.QuestionnaireItemComponent().apply { linkId = theLinkId }
    val questionnaireItemComponent =
      Questionnaire.QuestionnaireItemComponent().apply {
        item = listOf(innerQuestionnaireItemComponent)
      }
    listOf(questionnaireItemComponent).prePopulateInitialValues("!", prePopulationParams)
    Assert.assertEquals(
      emptyList<Questionnaire.QuestionnaireItemInitialComponent>(),
      questionnaireItemComponent.initial
    )
    Assert.assertNotEquals(
      emptyList<Questionnaire.QuestionnaireItemInitialComponent>(),
      innerQuestionnaireItemComponent.initial
    )
  }

  @Test
  fun testFindQuestionnaireItemComponentPrepopulateRemovesInitialExpression() {
    val theLinkId = "linkId"
    val questionnaireItemComponent =
      Questionnaire.QuestionnaireItemComponent().apply {
        linkId = theLinkId
        addExtension(
          ITEM_INITIAL_EXPRESSION_URL,
          Expression().apply {
            language = "text/fhirpath"
            expression = "expression"
          }
        )
      }
    listOf(questionnaireItemComponent).prePopulateInitialValues("", emptyList())
    Assert.assertFalse(questionnaireItemComponent.hasExtension(ITEM_INITIAL_EXPRESSION_URL))
  }

  @Test
  fun testCastToTypeReturnsCorrectTypes() {
    val booleanType = "true".castToType(DataType.BOOLEAN)
    Assert.assertEquals(BooleanType().fhirType(), booleanType?.fhirType())
    Assert.assertEquals("true", booleanType.valueToString())

    val decimalType = "6.4".castToType(DataType.DECIMAL)
    Assert.assertEquals(DecimalType().fhirType(), decimalType?.fhirType())
    Assert.assertEquals("6.4", decimalType.valueToString())

    val integerType = "4".castToType(DataType.INTEGER)
    Assert.assertEquals(IntegerType().fhirType(), integerType?.fhirType())
    Assert.assertEquals("4", integerType.valueToString())

    val dateType = "2020-02-02".castToType(DataType.DATE)
    Assert.assertEquals(DateType().fhirType(), dateType?.fhirType())
    Assert.assertEquals("02-Feb-2020", dateType.valueToString())

    val dateTimeType = "2020-02-02T13:00:32".castToType(DataType.DATETIME)
    Assert.assertEquals(DateTimeType().fhirType(), dateTimeType?.fhirType())
    Assert.assertEquals("02-Feb-2020", dateTimeType.valueToString())

    val timeType = "T13:00:32".castToType(DataType.TIME)
    Assert.assertEquals(TimeType().fhirType(), timeType?.fhirType())
    Assert.assertEquals("T13:00:32", timeType.valueToString())

    val stringType = "str".castToType(DataType.STRING)
    Assert.assertEquals(StringType().fhirType(), stringType?.fhirType())
    Assert.assertEquals("str", stringType.valueToString())

    val uriType = "https://str.org".castToType(DataType.URI)
    Assert.assertEquals(UriType().fhirType(), uriType?.fhirType())
    Assert.assertEquals("https://str.org", uriType.valueToString())

    // test invalid JSON
    val codingType = "invalid".castToType(DataType.CODING)
    Assert.assertEquals(null, codingType)

    val quantityType = "invalid".castToType(DataType.QUANTITY)
    Assert.assertEquals(null, quantityType)

    // TODO: test valid JSON
  }
}
