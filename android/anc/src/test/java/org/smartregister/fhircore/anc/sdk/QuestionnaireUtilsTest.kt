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

package org.smartregister.fhircore.anc.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.datacapture.utilities.SimpleWorkerContextProvider
import io.mockk.unmockkObject
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.shadow.AncApplicationShadow

@Config(shadows = [AncApplicationShadow::class])
class QuestionnaireUtilsTest : RobolectricTest() {
  private val iParser: IParser = FhirContext.forR4().newJsonParser()

  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Test
  fun testExtractObservations() {
    val questionnaire = loadQuestionnaire(context, "sample_anc_service_enrollment.json")
    val questionnaireResponse =
      loadQuestionnaireResponse(context, "sample_anc_service_enrollment_questionnaireresponse.json")

    val target = mutableListOf<Observation>()
    val patient = Patient().apply { id = "test_patient_1_id" }

    QuestionnaireUtils.extractObservations(
      questionnaireResponse,
      questionnaire.item,
      patient,
      target
    )

    val grObs = target[3]
    Assert.assertEquals(5, target.size)
    Assert.assertEquals("Number of previous pregnancies", grObs.code.coding[0].display)
    Assert.assertEquals("246211005", grObs.code.coding[0].code)
    Assert.assertEquals("Patient/test_patient_1_id", grObs.subject.reference)
    Assert.assertEquals(5, grObs.valueIntegerType.value)

    unmockkObject(SimpleWorkerContextProvider)
  }

  @Test
  fun testExtractTagsShouldReturnListOfCoding() {
    val questionnaire =
      Questionnaire().apply {
        item =
          listOf(
            Questionnaire.QuestionnaireItemComponent().apply {
              linkId = "1"
              definition = "http://hl7.org/fhir/StructureDefinition/Patient#Patient.meta.tag"
              code = listOf(Coding("system_1", "code_1", "display_1"))
            },
            Questionnaire.QuestionnaireItemComponent().apply {
              linkId = "2"
              definition = "http://hl7.org/fhir/StructureDefinition/Patient#Patient.meta.tag"
            },
            Questionnaire.QuestionnaireItemComponent().apply {
              linkId = "3"
              definition = "http://hl7.org/fhir/StructureDefinition/Patient#Patient.meta.tag"
            }
          )
      }
    val questionnaireResponse =
      QuestionnaireResponse().apply {
        item =
          listOf(
            QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
              linkId = "1"
              answer =
                listOf(
                  QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                    value = BooleanType(true)
                  }
                )
            },
            QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
              linkId = "2"
              answer =
                listOf(
                  QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                    value = Coding("system_2", "code_2", "display_2")
                  }
                )
            },
            QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
              linkId = "3"
              answer =
                listOf(
                  QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                    value = StringType("")
                  }
                )
            }
          )
      }

    val listOfCoding = QuestionnaireUtils.extractTags(questionnaireResponse, questionnaire)

    Assert.assertEquals(2, listOfCoding.size)
    for (i in 1 until 3) {
      Assert.assertEquals("system_$i", listOfCoding[i - 1].system)
      Assert.assertEquals("code_$i", listOfCoding[i - 1].code)
      Assert.assertEquals("display_$i", listOfCoding[i - 1].display)
    }
  }

  @Test
  fun testExtractFlagsShouldReturnListOfFlagAndExtensionPairs() {
    val questionnaire =
      Questionnaire().apply {
        item =
          listOf(
            Questionnaire.QuestionnaireItemComponent().apply {
              linkId = "1"
              addExtension(
                "http://hl7.org/fhir/StructureDefinition/flag-detail",
                StringType("display_1")
              )
              addItem().apply {
                linkId = "1.1"
                definition = "http://hl7.org/fhir/StructureDefinition/Patient#Patient.meta.tag"
                addExtension(
                  "http://hl7.org/fhir/StructureDefinition/flag-detail",
                  StringType("display_1_1")
                )
                code = mutableListOf(Coding("system_1_1", "code_1_1", "display_1_1"))
              }
            }
          )
      }

    val questionnaireResponse =
      QuestionnaireResponse().apply {
        item =
          listOf(
            QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
              linkId = "1"
              addAnswer().apply { value = Coding("system_1", "code_1", "display_1") }
            },
            QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
              linkId = "1.1"
              addAnswer().apply { value = BooleanType(true) }
            }
          )
      }

    val patient = Patient().apply { id = "patient_1" }

    val listOfFlags = QuestionnaireUtils.extractFlags(questionnaireResponse, questionnaire, patient)

    Assert.assertEquals(2, listOfFlags.size)

    val flagFirst = listOfFlags[0].first
    val extensionFirst = listOfFlags[0].second

    Assert.assertEquals(Flag.FlagStatus.ACTIVE, flagFirst.status)
    Assert.assertEquals("system_1", flagFirst.code.coding.first().system)
    Assert.assertEquals("code_1", flagFirst.code.coding.first().code)
    Assert.assertEquals("display_1", flagFirst.code.coding.first().display)
    Assert.assertEquals("Patient/patient_1", flagFirst.subject.reference)
    Assert.assertEquals("http://hl7.org/fhir/StructureDefinition/flag-detail", extensionFirst.url)
    Assert.assertEquals("display_1", extensionFirst.value.toString())

    val flagSecond = listOfFlags[1].first
    val extensionSecond = listOfFlags[1].second

    Assert.assertEquals(Flag.FlagStatus.ACTIVE, flagSecond.status)
    Assert.assertEquals("Patient/patient_1", flagSecond.subject.reference)
    Assert.assertEquals("http://hl7.org/fhir/StructureDefinition/flag-detail", extensionSecond.url)
    Assert.assertEquals("display_1_1", extensionSecond.value.toString())
  }

  private fun loadQuestionnaire(context: Context, id: String): Questionnaire {
    val qJson = context.assets.open(id).bufferedReader().use { it.readText() }
    return iParser.parseResource(qJson) as Questionnaire
  }

  private fun loadQuestionnaireResponse(context: Context, id: String): QuestionnaireResponse {
    val qrJson = context.assets.open(id).bufferedReader().use { it.readText() }
    return iParser.parseResource(qrJson) as QuestionnaireResponse
  }
}
