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

import java.util.UUID
import org.hl7.fhir.r4.model.BooleanType
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
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.util.extension.find

object QuestionnaireUtils {
  private const val flaggableKey = "flag-detail"

  private const val ITEM_CONTEXT_EXTENSION_URL: String =
    "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-observationExtract"
  private val Questionnaire.QuestionnaireItemComponent.isExtractableObservation: Boolean?
    get() {
      return this.extension.firstOrNull { it.url == ITEM_CONTEXT_EXTENSION_URL }?.let {
        (it.value as BooleanType).booleanValue()
      }
    }

  fun asPatientReference(id: String): Reference {
    return Reference().apply { this.reference = "Patient/$id" }
  }

  // https://github.com/opensrp/fhircore/issues/525
  // use ResourceMapper when supported by SDK
  // DO NOT remove unless you know what you are doing
  fun extractObservations(
    questionnaireResponse: QuestionnaireResponse,
    items: List<Questionnaire.QuestionnaireItemComponent>,
    subject: Resource,
    target: MutableList<Observation>
  ) {
    items.forEach {
      val response = questionnaireResponse.find(it.linkId)!!
      if (it.isExtractableObservation == true &&
          (response.hasAnswer() || it.type == Questionnaire.QuestionnaireItemType.GROUP)
      ) {
        target.add(response.asObservation(it, subject.asReference()))
      }

      extractObservations(questionnaireResponse, it.item, subject, target)
    }
  }

  fun extractTags(
    questionnaireResponse: QuestionnaireResponse,
    questionnaire: Questionnaire
  ): MutableList<Coding> {
    val taggable = mutableListOf<Questionnaire.QuestionnaireItemComponent>()

    itemsWithDefinition("Patient.meta.tag", questionnaire.item, taggable)

    val tags = mutableListOf<Coding>()

    taggable.forEach { qi ->
      // only add flags where answer is true or answer code matches flag value
      questionnaireResponse
        .find(qi.linkId)
        ?.answer
        ?.firstOrNull { it.hasValue() }
        ?.let {
          when (it.value) {
            is Coding -> it.valueCoding // for coding tag with any option selected by user
            is BooleanType -> qi.code[0] // for boolean tag wih question code if true
            else -> null
          }
        }
        ?.let { tags.add(it) }
    }

    return tags
  }

  fun extractFlags(
    questionnaireResponse: QuestionnaireResponse,
    questionnaire: Questionnaire,
    patient: Patient
  ): MutableList<Pair<Flag, Extension>> {
    val flaggableItems = mutableListOf<Questionnaire.QuestionnaireItemComponent>()

    extractFlaggables(questionnaire.item, flaggableItems)

    val flags = mutableListOf<Pair<Flag, Extension>>()

    flaggableItems.forEach { qi ->
      // only add flags where answer is true or answer code matches flag value
      questionnaireResponse.find(qi.linkId)?.answer?.firstOrNull { it.hasValue() }?.let {
        val flag = Flag()
        flag.id = getUniqueId()
        flag.status = Flag.FlagStatus.ACTIVE
        flag.subject = asPatientReference(patient.id)
        // todo simplify
        if (it.hasValueCoding() && extractFlagExtension(it.valueCoding, qi) != null) {
          flag.code = asCodeableConcept(it)
          val ext = extractFlagExtension(it.valueCoding, qi)

          flags.add(Pair(flag, ext!!))
        } else if (it.hasValueBooleanType() && it.valueBooleanType.booleanValue()) {
          flag.code = qi.asCodeableConcept()

          val ext = extractFlagExtension(qi)

          flags.add(Pair(flag, ext!!))
        }
      }
    }

    return flags
  }

  fun extractFlaggables(
    items: List<Questionnaire.QuestionnaireItemComponent>,
    target: MutableList<Questionnaire.QuestionnaireItemComponent>
  ) {
    items.forEach { qi ->
      qi.extension.firstOrNull { it.url.contains(flaggableKey) }?.let { target.add(qi) }

      if (qi.item.isNotEmpty()) {
        extractFlaggables(qi.item, target)
      }
    }
  }

  fun extractFlagExtension(item: Questionnaire.QuestionnaireItemComponent): Extension? {
    return item.extension.firstOrNull { it.url.contains(flaggableKey) }
  }

  fun extractFlagExtension(
    code: Coding,
    item: Questionnaire.QuestionnaireItemComponent
  ): Extension? {
    return item.extension.firstOrNull {
      it.url.contains(flaggableKey) &&
        (it.value.toString().contains(code.display) || code.display.contains(it.value.toString()))
    }
  }

  private fun itemsWithDefinition(
    definition: String,
    items: List<Questionnaire.QuestionnaireItemComponent>,
    target: MutableList<Questionnaire.QuestionnaireItemComponent>
  ) {
    items.forEach {
      if (it.definition?.contains(definition, true) == true) {
        target.add(it)
      }

      if (it.item.isNotEmpty()) {
        itemsWithDefinition(definition, it.item, target)
      }
    }
  }

  fun getUniqueId(): String {
    return UUID.randomUUID().toString()
  }

  fun Resource.asReference(): Reference {
    val referenceValue = "${fhirType()}/$id"

    return Reference().apply { this.reference = referenceValue }
  }

  fun Questionnaire.QuestionnaireItemComponent.asCodeableConcept(): CodeableConcept {
    val qit = this
    return CodeableConcept().apply {
      this.text = qit.text
      this.coding = qit.code

      this.addCoding().apply {
        this.code = qit.linkId
        this.system = qit.definition
      }
    }
  }

  fun QuestionnaireResponse.QuestionnaireResponseItemComponent.asObservation(
    questionnaireItemComponent: Questionnaire.QuestionnaireItemComponent,
    subject: Reference
  ): Observation {
    return Observation().apply {
      this.id = getUniqueId()
      this.effective = DateTimeType.now()
      this.code = questionnaireItemComponent.asCodeableConcept()
      this.status = Observation.ObservationStatus.FINAL
      this.value = answer?.firstOrNull()?.value
      this.subject = subject
    }
  }

  fun asCodeableConcept(linkId: String, q: Questionnaire): CodeableConcept {
    val qit = q.find(linkId)!!
    return qit.asCodeableConcept()
  }

  fun asCodeableConcept(
    qit: QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent
  ): CodeableConcept {
    return CodeableConcept().apply { this.addCoding(qit.valueCoding) }
  }
}
