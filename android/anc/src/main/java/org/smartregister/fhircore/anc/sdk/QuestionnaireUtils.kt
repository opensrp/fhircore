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
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.util.extension.find

object QuestionnaireUtils {
    private const val ITEM_CONTEXT_EXTENSION_URL: String =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-observationExtract"
    private val Questionnaire.QuestionnaireItemComponent.isExtractableObservation: Boolean?
        get() {
            return this.extension.firstOrNull { it.url == ITEM_CONTEXT_EXTENSION_URL }?.let {
                (it.value as BooleanType).booleanValue()
            }
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

    // https://github.com/opensrp/fhircore/issues/525
    // use ResourceMapper when supported by SDK
    // DO NOT remove unless you know what you are doing
    fun extractObservationsChoice(
        questionnaireResponse: QuestionnaireResponse,
        items: List<Questionnaire.QuestionnaireItemComponent>,
        subject: Resource,
        target: MutableList<Observation>
    ) {
        items.forEach {
            val response = questionnaireResponse.find(it.linkId)!!
            if (it.isExtractableObservation == true &&
                (response.hasAnswer() || it.type == Questionnaire.QuestionnaireItemType.CHOICE)
            ) {
                target.add(response.asObservation(it, subject.asReference()))
            }
            extractObservations(questionnaireResponse, it.item, subject, target)
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
}
