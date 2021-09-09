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

package org.smartregister.fhircore.anc.data

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.anc.data.model.AncPatientItem
import org.smartregister.fhircore.anc.ui.anccare.register.AncItemMapper
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireUtils
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireUtils.getUniqueId
import org.smartregister.fhircore.engine.util.extension.find

// TODO add own item mapper with https://github.com/opensrp/fhircore/issues/276
class FamilyRepository(
  override val fhirEngine: FhirEngine,
  override val domainMapper: DomainMapper<Patient, AncPatientItem>
) : RegisterRepository<Patient, AncPatientItem> {

  private val ancPatientRepository = AncPatientRepository(fhirEngine, AncItemMapper)

  // would be done with https://github.com/opensrp/fhircore/issues/276
  override suspend fun loadData(
    query: String,
    pageNumber: Int,
  ): List<AncPatientItem> {
    return listOf()
  }

  // TODO refactor when Elle is done with questionnaire refactor
  suspend fun postProcessFamilyMember(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse
  ) {
    val patient =
      ResourceMapper.extract(questionnaire, questionnaireResponse).entry[0].resource as Patient
    patient.id = getUniqueId()
    fhirEngine.save(patient)

    // TODO https://github.com/opensrp/fhircore/issues/525
    // use ResourceMapper when supported by SDK
    val target = mutableListOf<Observation>()
    QuestionnaireUtils.extractObservations(
      questionnaireResponse,
      questionnaire.item,
      patient,
      target
    )
    target.forEach { fhirEngine.save(it) }

    val pregnantItem = questionnaireResponse.find("is_pregnant")
    if (pregnantItem?.hasAnswer() == true && pregnantItem.answer[0].valueBooleanType.booleanValue())
      ancPatientRepository.enrollIntoAnc(patient)
  }
}
