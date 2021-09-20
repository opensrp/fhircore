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

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse

class ResourceMapperExtended(val fhirEngine: FhirEngine) {

  // TODO https://github.com/opensrp/fhircore/issues/525
  // use ResourceMapper when supported by SDK
  suspend fun saveParsedResource(
    questionnaireResponse: QuestionnaireResponse,
    questionnaire: Questionnaire,
    patientId: String,
    relatedTo: String?
  ) {
    if (!questionnaire.hasSubjectType("Patient")) return

    val patient =
      kotlin.runCatching { fhirEngine.load(Patient::class.java, patientId) }.getOrElse {
        ResourceMapper.extract(questionnaire, questionnaireResponse).entry[0].resource as Patient
      }

    patient.id = patientId

    fhirEngine.save(patient)

    val obsList = mutableListOf<Observation>()
    QuestionnaireUtils.extractObservations(
      questionnaireResponse,
      questionnaire.item,
      patient,
      obsList
    )
    obsList.forEach { fhirEngine.save(it) }

    val tags = QuestionnaireUtils.extractTags(questionnaireResponse, questionnaire)
    tags.forEach { patient.meta.addTag(it) }

    val flags = QuestionnaireUtils.extractFlags(questionnaireResponse, questionnaire, patient)
    flags.forEach {
      patient.addExtension(it.second)

      fhirEngine.save(it.first)
    }

    relatedTo?.let {
      val related =
        kotlin.runCatching { fhirEngine.load(Patient::class.java, relatedTo) }.getOrNull()

      patient.addLink(getLink(relatedTo))
      // TODO remove when sync is implemented
      patient.addressFirstRep.district = related?.addressFirstRep?.district
      patient.addressFirstRep.city = related?.addressFirstRep?.city
    }

    fhirEngine.update(patient)
  }

  private fun getLink(relatedTo: String): Patient.PatientLinkComponent {
    val link = Patient.PatientLinkComponent()
    link.other = QuestionnaireUtils.asPatientReference(relatedTo)
    link.type = Patient.LinkType.REFER
    return link
  }
}
