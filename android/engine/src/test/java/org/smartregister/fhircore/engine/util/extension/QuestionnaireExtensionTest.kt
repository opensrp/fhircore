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

import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker

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
    val bundle = Bundle()
    val bundleEntryComponent = Bundle.BundleEntryComponent()
    val patient = Faker.buildPatient(id)
    bundleEntryComponent.resource = patient
    bundle.entry = listOf(bundleEntryComponent)
    questionniareResponse.subject.reference = id
    Assert.assertEquals(patient, questionniareResponse.findSubject(bundle))
  }

  @Test
  fun testFindSubjectMismatchingId() {
    val id = "1234"
    val bundle = Bundle()
    val bundleEntryComponent = Bundle.BundleEntryComponent()
    val patient = Faker.buildPatient(id)
    bundleEntryComponent.resource = patient
    bundle.entry = listOf(bundleEntryComponent)
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
    val questionnaireItemComponent = Questionnaire.QuestionnaireItemComponent()
    questionnaireItemComponent.linkId = id
    questionniare.item = listOf(questionnaireItemComponent)
    Assert.assertEquals(null, questionniare.find("5678"))
  }

  @Test
  fun testFindMatchingLinkId() {
    val id = "1234"
    val questionnaireItemComponent = Questionnaire.QuestionnaireItemComponent()
    questionnaireItemComponent.linkId = id
    questionniare.item = listOf(questionnaireItemComponent)
    Assert.assertEquals(questionnaireItemComponent, questionniare.find(id))
  }
}
