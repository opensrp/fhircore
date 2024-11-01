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

import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Enumerations.DataType
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.Type
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.QuestionnaireType
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class QuestionnaireExtensionTest : RobolectricTest() {
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
    Assert.assertEquals(false, questionniare.extractByStructureMap())
  }

  @Test
  fun testIsExtractionCandidatePartialStringFalse() {
    questionniare.extension.add(Extension("https://questionnaire-itemExtractionContext/abc"))
    Assert.assertEquals(false, questionniare.extractByStructureMap())
  }

  @Test
  fun testIsExtractionCandidateTrue() {
    questionniare.extension.add(Extension("https://sdc-questionnaire-itemExtractionContext/abc"))
    Assert.assertEquals(true, questionniare.extractByStructureMap())
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
      questionniare.find(FieldType.DEFINITION, ""),
    )
  }

  @Test
  fun testQuestionnaireFindWithFieldTypeType() {
    Assert.assertEquals(
      emptyList<Questionnaire.QuestionnaireItemComponent>(),
      questionniare.find(FieldType.TYPE, ""),
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
      questionniare.find(FieldType.DEFINITION, value),
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
      questionniare.find(FieldType.TYPE, value.toString()),
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
      questionniare.find(FieldType.LINK_ID, id),
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
      questionnaireItemComponent.initial,
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
      questionnaireItemComponent.initial,
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
      questionnaireItemComponent.initial,
    )
    Assert.assertNotEquals(
      emptyList<Questionnaire.QuestionnaireItemInitialComponent>(),
      innerQuestionnaireItemComponent.initial,
    )
  }

  @Test
  fun testQuestionnaireItemAnswerOptionComponentPrepopulateNoChange() {
    val theLinkId = "linkId"
    val questionnaireItemComponent =
      Questionnaire.QuestionnaireItemComponent().apply { linkId = theLinkId }
    listOf(questionnaireItemComponent).prePopulateInitialValues("", emptyList())
    Assert.assertEquals(
      emptyList<Questionnaire.QuestionnaireItemAnswerOptionComponent>(),
      questionnaireItemComponent.answerOption,
    )
  }

  @Test
  fun testQuestionnaireItemAnswerOptionComponentPrepopulateSingleChoiceItem() {
    val theLinkId = "Diagnosis"
    val valueToSelect = "malaria"
    val malariaQuestionnaireItemAnswerOptionComponent =
      Questionnaire.QuestionnaireItemAnswerOptionComponent().apply {
        value = Coding().apply { code = "malaria" }
      }
    val fluQuestionnaireItemAnswerOptionComponent =
      Questionnaire.QuestionnaireItemAnswerOptionComponent().apply {
        value = Coding().apply { code = "flu" }
      }
    val prePopulationParams =
      listOf(ActionParameter("key", linkId = theLinkId, value = valueToSelect))
    val questionnaireItemComponent =
      Questionnaire.QuestionnaireItemComponent().apply {
        linkId = theLinkId
        answerOption =
          listOf(
            malariaQuestionnaireItemAnswerOptionComponent,
            fluQuestionnaireItemAnswerOptionComponent,
          )
        type = Questionnaire.QuestionnaireItemType.CHOICE
      }

    Assert.assertFalse(questionnaireItemComponent.answerOption[0].initialSelected)
    Assert.assertFalse(questionnaireItemComponent.answerOption[1].initialSelected)

    listOf(questionnaireItemComponent).prePopulateInitialValues("@{", prePopulationParams)

    Assert.assertTrue(questionnaireItemComponent.answerOption[0].initialSelected)
    Assert.assertFalse(questionnaireItemComponent.answerOption[1].initialSelected)
  }

  @Test
  fun testQuestionnaireItemAnswerOptionComponentPrepopulateMultipleChoiceItems() {
    val theLinkId = "Diagnosis"
    val valueToSelect = "malaria,flu"
    val malariaQuestionnaireItemAnswerOptionComponent =
      Questionnaire.QuestionnaireItemAnswerOptionComponent().apply {
        value = Coding().apply { code = "malaria" }
      }
    val fluQuestionnaireItemAnswerOptionComponent =
      Questionnaire.QuestionnaireItemAnswerOptionComponent().apply {
        value = Coding().apply { code = "flu" }
      }
    val prePopulationParams =
      listOf(ActionParameter("key", linkId = theLinkId, value = valueToSelect))
    val questionnaireItemComponent =
      Questionnaire.QuestionnaireItemComponent().apply {
        linkId = theLinkId
        answerOption =
          listOf(
            malariaQuestionnaireItemAnswerOptionComponent,
            fluQuestionnaireItemAnswerOptionComponent,
          )
        type = Questionnaire.QuestionnaireItemType.CHOICE
      }
    Assert.assertFalse(questionnaireItemComponent.answerOption[0].initialSelected)
    Assert.assertFalse(questionnaireItemComponent.answerOption[1].initialSelected)

    listOf(questionnaireItemComponent).prePopulateInitialValues("@{", prePopulationParams)

    Assert.assertTrue(questionnaireItemComponent.answerOption[0].initialSelected)
    Assert.assertTrue(questionnaireItemComponent.answerOption[1].initialSelected)
  }

  @Test
  fun testFindQuestionnaireItemComponentPrepopulateShallRemoveInitialExpressionWhenHavingTheSameLinkIdBetweenInitialExpressionAndPrePopulateFromConfigs() {
    val theLinkId = "linkId"
    val questionnaireItemComponent =
      Questionnaire.QuestionnaireItemComponent().apply {
        linkId = theLinkId
        addExtension(
          EXTENSION_INITIAL_EXPRESSION_URL,
          Expression().apply {
            language = "text/fhirpath"
            expression = "expression"
          },
        )
      }
    val actionParams =
      listOf(
        ActionParameter(
          paramType = ActionParameterType.PREPOPULATE,
          linkId = theLinkId,
          dataType = DataType.INTEGER,
          key = "my-key",
          value = "100",
        ),
      )
    listOf(questionnaireItemComponent).prePopulateInitialValues("@{", actionParams)
    Assert.assertFalse(questionnaireItemComponent.hasExtension(EXTENSION_INITIAL_EXPRESSION_URL))
  }

  @Test
  fun testFindQuestionnaireItemComponentPrepopulateShallNotRemoveInitialExpressionWhenThatLinkIdHasNoPrePopulateFromConfigs() {
    val theLinkId = "linkId"
    val questionnaireItemComponent =
      Questionnaire.QuestionnaireItemComponent().apply {
        linkId = theLinkId
        addExtension(
          EXTENSION_INITIAL_EXPRESSION_URL,
          Expression().apply {
            language = "text/fhirpath"
            expression = "expression"
          },
        )
      }
    listOf(questionnaireItemComponent).prePopulateInitialValues("", emptyList())
    Assert.assertTrue(questionnaireItemComponent.hasExtension(EXTENSION_INITIAL_EXPRESSION_URL))
  }

  @Test
  fun testPrepopulateQuestionnaireWithComputedValues() = runTest {
    val questionnaireConfig =
      QuestionnaireConfig(
        id = UUID.randomUUID().toString(),
        resourceIdentifier = "patient.id",
        resourceType = ResourceType.Patient,
        barcodeLinkId = "patient-barcode",
        type = QuestionnaireType.READ_ONLY.name,
        configRules =
          listOf(
            RuleConfig(
              name = "rule1",
              actions = listOf("data.put('rule1', 'Sample Rule')"),
            ),
          ),
        extraParams =
          listOf(
            ActionParameter(
              key = "rule1",
              value = "@{rule1}",
              paramType = ActionParameterType.PARAMDATA,
            ),
          ),
      )
    val patientAgeLinkId = "patient-age"
    val actionParameter =
      listOf(
        ActionParameter(
          paramType = ActionParameterType.PREPOPULATE,
          linkId = patientAgeLinkId,
          dataType = Enumerations.DataType.INTEGER,
          key = patientAgeLinkId,
          value = "20",
        ),
      )
    val questionnaire =
      Questionnaire().apply {
        addItem(
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = patientAgeLinkId
            type = Questionnaire.QuestionnaireItemType.INTEGER
            readOnly = true
          },
        )
      }

    questionnaire.prepopulateWithComputedConfigValues(
      questionnaireConfig,
      actionParameter,
      { mapOf(patientAgeLinkId to "20") },
      { _, _ -> "" },
    )

    // Questionnaire.item pre-populated
    val questionnairePatientAgeItem = questionnaire.find(patientAgeLinkId)
    val itemValue: Type? = questionnairePatientAgeItem?.initial?.firstOrNull()?.value
    Assert.assertTrue(itemValue is IntegerType)
    Assert.assertEquals(20, itemValue?.primitiveValue()?.toInt())

    // Barcode linkId updated
    val questionnaireBarcodeItem = questionnaireConfig.barcodeLinkId?.let { questionnaire.find(it) }
    val barCodeItemValue: Type? = questionnaireBarcodeItem?.initial?.firstOrNull()?.value
    Assert.assertFalse(barCodeItemValue is StringType)
    Assert.assertNull(
      questionnaireConfig.resourceIdentifier,
      barCodeItemValue?.primitiveValue(),
    )
  }
}
