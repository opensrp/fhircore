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

package org.smartregister.fhircore.util

import android.content.Context
import android.content.Intent
import ca.uhn.fhir.context.FhirContext
import java.util.UUID
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RiskAssessment
import org.smartregister.fhircore.activity.core.QuestionnaireActivity
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PRE_ASSIGNED_ID
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_PATH_KEY
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_TITLE_KEY

object QuestionnaireUtils {
  private val parser = FhirContext.forR4().newJsonParser()

  fun buildQuestionnaireIntent(
    context: Context,
    questionnaireTitle: String,
    questionnaireId: String,
    patientId: String?,
    isNewPatient: Boolean
  ): Intent {
    return Intent(context, QuestionnaireActivity::class.java).apply {
      putExtra(QUESTIONNAIRE_TITLE_KEY, questionnaireTitle)
      putExtra(QUESTIONNAIRE_PATH_KEY, questionnaireId)

      patientId?.let {
        if (isNewPatient) putExtra(QUESTIONNAIRE_ARG_PRE_ASSIGNED_ID, patientId)
        else putExtra(QUESTIONNAIRE_ARG_PATIENT_KEY, patientId)
      }
    }
  }

  fun asQuestionnaireResponse(questionnaireResponse: String): QuestionnaireResponse {
    return parser.parseResource(questionnaireResponse) as QuestionnaireResponse
  }

  private fun asQuestionnaireItem(
    item: Questionnaire.QuestionnaireItemComponent,
    linkId: String
  ): Questionnaire.QuestionnaireItemComponent? {
    if (item.linkId.contentEquals(linkId)) {
      return item
    }

    item.item.forEach {
      val res = asQuestionnaireItem(it, linkId)
      if (res != null) {
        return res
      }
    }
    return null
  }

  private fun asQuestionnaireItem(
    questionnaire: Questionnaire,
    linkId: String
  ): Questionnaire.QuestionnaireItemComponent? {
    questionnaire.item.forEach {
      val res = asQuestionnaireItem(it, linkId)
      if (res != null) {
        return res
      }
    }
    return null
  }

  fun asCodeableConcept(linkId: String, q: Questionnaire): CodeableConcept {
    return CodeableConcept().apply {
      val qit = asQuestionnaireItem(q, linkId)!!

      this.text = qit.text
      this.coding = qit.code

      this.addCoding().apply {
        this.code = qit.linkId
        this.system = qit.definition
      }
    }
  }

  fun asObs(
    qr: QuestionnaireResponse.QuestionnaireResponseItemComponent,
    subject: Patient,
    questionnaire: Questionnaire
  ): Observation {
    val obs = Observation()
    obs.id = getUniqueId()
    obs.effective = DateTimeType.now()
    obs.code = asCodeableConcept(qr.linkId, questionnaire)
    obs.status = Observation.ObservationStatus.FINAL
    obs.value = if (qr.hasAnswer()) qr.answer[0].value else null
    obs.subject = Reference().apply { this.reference = "Patient/" + subject.id }

    return obs
  }

  // todo revisit this logic when ResourceMapper is stable
  fun extractObservations(
    questionnaireResponse: QuestionnaireResponse,
    questionnaire: Questionnaire,
    patient: Patient
  ): MutableList<Observation> {
    val observations = mutableListOf<Observation>()

    for (i in 0 until questionnaire.item.size) {
      // questionnaire and questionnaireResponse mapping go parallel
      val qItem = questionnaire.item[i]
      val qrItem = questionnaireResponse.item[i]

      if (qItem.definition?.contains("Observation") == true) {
        // get main group obs. only 1 level of obs nesting allowed for now //todo
        val main = asObs(qrItem, patient, questionnaire)

        // loop over all individual obs
        for (j in 0 until qrItem.item.size) {
          val mainRespItem = qrItem.item[j]

          if (mainRespItem.hasAnswer()) {
            val obs = asObs(mainRespItem, patient, questionnaire)

            // add reference to each comorbidity to main group obs
            main.addHasMember(Reference().apply { this.reference = "Observation/" + obs.id })

            observations.add(obs)
          }
        }

        observations.add(main)
      }
    }

    return observations
  }

  private fun flaggableFor(
    selectedFlagCode: CodeableConcept,
    questionnaireResponse: QuestionnaireResponse,
    questionnaire: Questionnaire
  ): Questionnaire.QuestionnaireItemComponent? {
    // is allowed for flagging with extension
    val flaggable = itemWithExtension(questionnaire, "flag-detail")

    // flag code of selected answer must match with given extension and there should only be one
    // flag of a type/code
    return flaggable.firstOrNull {
      val qrItem = itemWithLinkId(questionnaireResponse, it.linkId)

      doesIntersect(selectedFlagCode.coding, qrItem!!.answer.map { ans -> ans.valueCoding })
    }
  }

  fun extractFlagExtension(
    flag: Flag,
    questionnaireResponse: QuestionnaireResponse,
    questionnaire: Questionnaire
  ): Extension? {
    // flag code must match with given extension
    val item = flaggableFor(flag.code, questionnaireResponse, questionnaire) ?: return null

    return item.extension.single { it.url.contains("flag-detail") }
  }

  fun extractFlag(
    questionnaireResponse: QuestionnaireResponse,
    questionnaire: Questionnaire,
    riskAssessment: RiskAssessment
  ): Flag? {
    // no risk then no flag
    if (riskAssessment.prediction[0].relativeRisk.equals(0) ||
        !riskAssessment.prediction[0].hasOutcome()
    ) {
      return null
    }

    val code = riskAssessment.prediction[0].outcome

    // if no flagging is needed return
    flaggableFor(code, questionnaireResponse, questionnaire) ?: return null

    val flag = Flag()
    flag.id = getUniqueId()
    flag.status = Flag.FlagStatus.ACTIVE
    flag.code = code
    flag.subject = riskAssessment.subject

    return flag
  }

  private fun itemWithDefinition(
    item: Questionnaire.QuestionnaireItemComponent,
    definition: String
  ): Questionnaire.QuestionnaireItemComponent? {
    if (item.definition?.contains(definition) == true) {
      return item
    }

    item.item.forEach {
      val qit = itemWithDefinition(it, definition)
      if (qit != null) return qit
    }

    return null
  }

  private fun itemWithDefinition(
    questionnaire: Questionnaire,
    definition: String
  ): List<Questionnaire.QuestionnaireItemComponent> {
    return questionnaire.item.mapNotNull { itemWithDefinition(it, definition) }
  }

  private fun itemWithExtension(
    item: Questionnaire.QuestionnaireItemComponent,
    extension: String
  ): Questionnaire.QuestionnaireItemComponent? {
    if (item.extension.singleOrNull { ro -> ro.url.contains(extension) } != null) {
      return item
    }

    for (i in item.item) {
      val qit = itemWithExtension(i, extension)
      if (qit != null) {
        return qit
      }
    }

    return null
  }

  private fun itemWithExtension(
    questionnaire: Questionnaire,
    extension: String
  ): List<Questionnaire.QuestionnaireItemComponent> {
    return questionnaire.item.mapNotNull { itemWithExtension(it, extension) }
  }

  private fun itemWithLinkId(
    item: QuestionnaireResponse.QuestionnaireResponseItemComponent,
    linkId: String
  ): QuestionnaireResponse.QuestionnaireResponseItemComponent? {
    if (item.linkId.contentEquals(linkId)) {
      return item
    }

    for (i in item.item) {
      val qit = itemWithLinkId(i, linkId)
      if (qit != null) {
        return qit
      }
    }

    return null
  }

  private fun itemWithLinkId(
    questionnaireResponse: QuestionnaireResponse,
    linkId: String
  ): QuestionnaireResponse.QuestionnaireResponseItemComponent? {
    return questionnaireResponse.item.mapNotNull { itemWithLinkId(it, linkId) }.firstOrNull()
  }

  fun valueStringWithLinkId(questionnaireResponse: QuestionnaireResponse, linkId: String): String? {
    val ans = itemWithLinkId(questionnaireResponse, linkId)?.answerFirstRep
    return ans?.valueStringType?.asStringValue()
  }

  private fun doesIntersect(codingList: List<Coding>, other: List<Coding>): Boolean {
    val codes = codingList.map { it.code }

    return other.any { codes.contains(it.code) }
  }

  private fun getItem(
    obs: Observation,
    item: Questionnaire.QuestionnaireItemComponent?
  ): Questionnaire.QuestionnaireItemComponent? {
    item ?: return null

    val codes = item.code.map { it.code }.toMutableList()
    codes.add(item.linkId)
    codes.add(item.text)

    if (obs.code.coding.any { codes.contains(it.code) }) {
      return item
    }

    item.item.forEach {
      val res = getItem(obs, it)
      if (res != null) {
        return res
      }
    }

    return null
  }

  // only one risk per questionnaire supported for now //todo
  fun extractRiskAssessment(
    observations: List<Observation>,
    questionnaireResponse: QuestionnaireResponse,
    questionnaire: Questionnaire
  ): RiskAssessment? {
    val qItem = itemWithDefinition(questionnaire, "RiskAssessment").firstOrNull() ?: return null
    var qrItem = itemWithLinkId(questionnaireResponse, qItem.linkId)

    var riskScore = 0
    val risk = RiskAssessment()

    observations.forEach { obs ->
      val qObs = questionnaire.item.map { getItem(obs, it) }.find { it != null }

      if (qObs != null) {
        val isRiskObs =
          qObs.extension.singleOrNull { ro -> ro.url.contains("RiskAssessment") } != null

        // todo revisit this when calculate expression is working
        if (isRiskObs &&
            ((obs.hasValueBooleanType() && obs.valueBooleanType.booleanValue()) ||
              (obs.hasValueStringType() && obs.hasValue()))
        ) {
          riskScore++

          risk.addBasis(Reference().apply { this.reference = "Observation/" + obs.id })
        }
      }
    }

    risk.status = RiskAssessment.RiskAssessmentStatus.FINAL
    risk.code = asCodeableConcept(qrItem!!.linkId, questionnaire)
    risk.id = getUniqueId()
    risk.subject = observations[0].subject
    risk.occurrence = DateTimeType.now()
    risk.addPrediction().apply {
      this.relativeRisk = riskScore.toBigDecimal()

      // todo change when calculated expression is working
      if (qrItem.hasAnswer() && riskScore > 0) {
        this.outcome =
          CodeableConcept().apply {
            this.text = qrItem.answer[0].valueCoding.display
            this.coding = listOf(qrItem.answer[0].valueCoding)
          }
      }
    }

    return risk
  }

  private fun getUniqueId(): String {
    return UUID.randomUUID().toString()
  }
}
