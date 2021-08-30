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

package org.smartregister.fhircore.eir.ui.questionnaire

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import java.math.BigDecimal
import java.util.UUID
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.RiskAssessment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.eir.robolectric.RobolectricTest
import org.smartregister.fhircore.eir.shadow.EirApplicationShadow
import org.smartregister.fhircore.eir.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
import org.smartregister.fhircore.eir.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PRE_ASSIGNED_ID
import org.smartregister.fhircore.eir.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_PATH_KEY
import org.smartregister.fhircore.eir.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_TITLE_KEY

@Config(shadows = [EirApplicationShadow::class])
class QuestionnaireUtilsTest : RobolectricTest() {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private lateinit var questionnaire: Questionnaire
  private lateinit var questionnaireResponse: QuestionnaireResponse

  @Before
  fun setup() {
    val iParser: IParser = FhirContext.forR4().newJsonParser()

    val qJson =
      context.assets.open("sample_patient_registration.json").bufferedReader().use { it.readText() }

    val qrJson =
      context.assets.open("sample_registration_questionnaireresponse.json").bufferedReader().use {
        it.readText()
      }

    questionnaire = iParser.parseResource(qJson) as Questionnaire
    questionnaireResponse = iParser.parseResource(qrJson) as QuestionnaireResponse
  }

  @Test
  fun testBuildQuestionnaireIntent_shouldReturnIntentWithExtrasWithPatientId() {
    val result =
      QuestionnaireUtils.buildQuestionnaireIntent(context, "My Q title", "my-q-id", "12345", false)

    assertEquals(QuestionnaireActivity::class.java.name, result.component!!.className)

    assertEquals("My Q title", result.getStringExtra(QUESTIONNAIRE_TITLE_KEY))
    assertEquals("my-q-id", result.getStringExtra(QUESTIONNAIRE_PATH_KEY))

    // should set patient-arg key and not the pre-assigned-key which is for new patients
    assertEquals("12345", result.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY))
    assertNull(result.getStringExtra(QUESTIONNAIRE_ARG_PRE_ASSIGNED_ID))
  }

  @Test
  fun testBuildQuestionnaireIntent_shouldReturnIntentWithExtrasWithNewPatient() {
    val result =
      QuestionnaireUtils.buildQuestionnaireIntent(context, "My Q title", "my-q-id", "12345", true)

    assertEquals(QuestionnaireActivity::class.java.name, result.component!!.className)

    assertEquals("My Q title", result.getStringExtra(QUESTIONNAIRE_TITLE_KEY))
    assertEquals("my-q-id", result.getStringExtra(QUESTIONNAIRE_PATH_KEY))

    // should set patient-arg key and not the pre-assigned-key which is for new patients
    assertEquals("12345", result.getStringExtra(QUESTIONNAIRE_ARG_PRE_ASSIGNED_ID))
    assertNull(result.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY))
  }

  @Test
  fun testAsCodeableConcept_shouldReturnCorrectTextAndCodeSystemMapping() {
    val result = QuestionnaireUtils.asCodeableConcept("diabetes_mellitus", questionnaire)

    assertEquals("Diabetes Mellitus (DM)", result.text)

    assertEquals("73211009", result.coding[0].code)
    assertEquals("https://www.snomed.org", result.coding[0].system)

    assertEquals("diabetes_mellitus", result.coding[1].code)
    assertEquals("http://hl7.org/fhir/StructureDefinition/Observation", result.coding[1].system)
  }

  @Test
  fun testAsObs_shouldReturnCorrectObsData() {
    val patient = Patient()
    patient.id = "1122"

    val qrItem = getQuestionnaireResponseItem("diabetes_mellitus")
    qrItem.addAnswer().value = BooleanType(true)

    val result = QuestionnaireUtils.asObs(qrItem, patient, questionnaire)

    assertEquals("Diabetes Mellitus (DM)", result.code.text)

    assertTrue(UUID.fromString(result.id).toString().isNotEmpty())
    assertTrue(result.effectiveDateTimeType.isToday)
    assertTrue(result.valueBooleanType.booleanValue())

    assertEquals("73211009", result.code.coding[0].code)
    assertEquals("https://www.snomed.org", result.code.coding[0].system)

    assertEquals("diabetes_mellitus", result.code.coding[1].code)
    assertEquals(
      "http://hl7.org/fhir/StructureDefinition/Observation",
      result.code.coding[1].system
    )

    assertEquals(Observation.ObservationStatus.FINAL, result.status)
    assertEquals("Patient/" + patient.id, result.subject.reference)
  }

  @Test
  fun testExtractObservation_shouldReturnValidObservations() {
    val patient = Patient()
    patient.id = "1122"

    setResponsesToTrue("diabetes_mellitus", "hypertension")

    val result =
      QuestionnaireUtils.extractObservations(questionnaireResponse, questionnaire, patient)

    // obs1 with diabetes
    val obs1 = result[0]

    assertTrue(obs1.effectiveDateTimeType.isToday)
    assertTrue(obs1.valueBooleanType.booleanValue())

    assertEquals("73211009", obs1.code.coding[0].code)
    assertEquals("diabetes_mellitus", obs1.code.coding[1].code)
    assertEquals("Patient/" + patient.id, obs1.subject.reference)

    // obs2 with hypertension
    val obs2 = result[1]

    assertTrue(obs2.effectiveDateTimeType.isToday)
    assertTrue(obs2.valueBooleanType.booleanValue())

    assertEquals("59621000", obs2.code.coding[0].code)
    assertEquals("hypertension", obs2.code.coding[1].code)
    assertEquals("Patient/" + patient.id, obs2.subject.reference)

    // one group obs with hasMembers
    val main = result[2]
    assertEquals("Do you have any of the following conditions?", main.code.text)
    assertEquals("991381000000107", main.code.coding[0].code)
    assertEquals("https://www.snomed.org", main.code.coding[0].system)
    assertEquals("Observation/" + obs1.id, main.hasMember[0].reference)
    assertEquals("Observation/" + obs2.id, main.hasMember[1].reference)
  }

  @Test
  fun testExtractRiskAssessment_shouldReturnValidRiskAssessmentWithNoOutCome() {
    val patient = Patient()
    patient.id = "1122"

    setResponsesToFalse("diabetes_mellitus", "hypertension")

    val observations =
      QuestionnaireUtils.extractObservations(questionnaireResponse, questionnaire, patient)

    val risk =
      QuestionnaireUtils.extractRiskAssessment(observations, questionnaireResponse, questionnaire)!!

    // obs1 with diabetes
    val obs1 = observations[0]
    assertEquals("diabetes_mellitus", obs1.code.coding[1].code)

    // obs2 with hypertension
    val obs2 = observations[1]
    assertEquals("hypertension", obs2.code.coding[1].code)

    assertEquals(obs1.subject.reference, risk.subject.reference)
    assertEquals("Patient/1122", risk.subject.reference)

    assertEquals(BigDecimal(0), risk.prediction[0].relativeRisk)
    assertTrue(risk.basis.isEmpty())

    assertEquals("225338004", risk.code.coding[0].code)
    assertEquals("https://www.snomed.org", risk.code.coding[0].system)

    assertTrue(risk.occurrence.dateTimeValue().isToday)
    assertEquals(RiskAssessment.RiskAssessmentStatus.FINAL, risk.status)

    assertFalse(risk.prediction[0].hasOutcome())
  }

  @Test
  fun testExtractRiskAssessment_shouldReturnValidRiskAssessmentWithValidOutcome() {
    val patient = Patient()
    patient.id = "1122"

    setResponsesToTrue("diabetes_mellitus", "hypertension")

    val observations =
      QuestionnaireUtils.extractObservations(questionnaireResponse, questionnaire, patient)

    val risk =
      QuestionnaireUtils.extractRiskAssessment(observations, questionnaireResponse, questionnaire)!!

    // obs1 with diabetes
    val obs1 = observations[0]
    assertEquals("diabetes_mellitus", obs1.code.coding[1].code)

    // obs2 with hypertension
    val obs2 = observations[1]
    assertEquals("hypertension", obs2.code.coding[1].code)

    assertEquals(obs1.subject.reference, risk.subject.reference)
    assertEquals("Patient/1122", risk.subject.reference)

    assertEquals(BigDecimal(2), risk.prediction[0].relativeRisk)
    assertEquals("Observation/" + obs1.id, risk.basis[0].reference)
    assertEquals("Observation/" + obs2.id, risk.basis[1].reference)

    assertEquals("225338004", risk.code.coding[0].code)
    assertEquals("https://www.snomed.org", risk.code.coding[0].system)

    assertTrue(risk.occurrence.dateTimeValue().isToday)
    assertEquals(RiskAssessment.RiskAssessmentStatus.FINAL, risk.status)

    assertEquals("High Risk for COVID-19", risk.prediction[0].outcome.text)

    assertEquals("870577009", risk.prediction[0].outcome.coding[0].code)
    assertEquals("https://www.snomed.org", risk.prediction[0].outcome.coding[0].system)
  }

  @Test
  fun testExtractFlag_shouldReturnValidFlagWithData() {
    val patient = Patient()
    patient.id = "1122"

    setResponsesToTrue("diabetes_mellitus", "hypertension")

    val observations =
      QuestionnaireUtils.extractObservations(questionnaireResponse, questionnaire, patient)

    val risk =
      QuestionnaireUtils.extractRiskAssessment(observations, questionnaireResponse, questionnaire)!!

    val flag = QuestionnaireUtils.extractFlag(questionnaireResponse, questionnaire, risk)!!

    assertEquals(risk.subject.reference, flag.subject.reference)
    assertEquals(Flag.FlagStatus.ACTIVE, flag.status)

    assertEquals("870577009", flag.code.coding[0].code)
    assertEquals("https://www.snomed.org", flag.code.coding[0].system)
  }

  @Test
  fun testExtractFlag_shouldReturnValidExtensionWithData() {
    val patient = Patient()
    patient.id = "1122"

    setResponsesToTrue("diabetes_mellitus", "hypertension")

    val observations =
      QuestionnaireUtils.extractObservations(questionnaireResponse, questionnaire, patient)

    val risk =
      QuestionnaireUtils.extractRiskAssessment(observations, questionnaireResponse, questionnaire)!!
    val flag = QuestionnaireUtils.extractFlag(questionnaireResponse, questionnaire, risk)!!

    val flagExt =
      QuestionnaireUtils.extractFlagExtension(flag, questionnaireResponse, questionnaire)!!

    assertEquals("http://hl7.org/fhir/StructureDefinition/flag-detail", flagExt.url)
    assertEquals("at risk", flagExt.value.toString())
  }

  private fun getQuestionnaireResponseItem(
    linkId: String
  ): QuestionnaireResponse.QuestionnaireResponseItemComponent {
    // comorbidities section is on 2 index
    return questionnaireResponse.item[2].item.single { it.linkId!!.contentEquals(linkId) }
  }

  private fun setResponsesToTrue(vararg linkIds: String) {
    linkIds.forEach { getQuestionnaireResponseItem(it).addAnswer().value = BooleanType(true) }
  }

  private fun setResponsesToFalse(vararg linkIds: String) {
    linkIds.forEach { getQuestionnaireResponseItem(it).addAnswer().value = BooleanType(false) }
  }
}
