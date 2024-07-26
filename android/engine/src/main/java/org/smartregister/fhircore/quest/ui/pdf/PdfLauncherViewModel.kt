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

package org.smartregister.fhircore.quest.ui.pdf

import androidx.lifecycle.ViewModel
import com.google.android.fhir.search.Search
import dagger.hilt.android.lifecycle.HiltViewModel
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Patient
import javax.inject.Inject
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DispatcherProvider

@HiltViewModel
class PdfLauncherViewModel
@Inject
constructor(
  val defaultRepository: DefaultRepository,
  val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

  /**
   * Retrieve the QuestionnaireResponse for the given questionnaire and subject.
   *
   * @param questionnaireId The ID of the questionnaire.
   * @param subjectId The ID of the subject.
   * @param subjectType The type of the subject.
   * @return The QuestionnaireResponse if found, otherwise null.
   */
  suspend fun retrieveQuestionnaireResponse(
    questionnaireId: String,
    subjectId: String,
    subjectType: ResourceType
  ): QuestionnaireResponse? {
    val searchQuery = createQuestionnaireResponseSearchQuery(questionnaireId, subjectId, subjectType)
    return defaultRepository.search<QuestionnaireResponse>(searchQuery).firstOrNull()
  }

  /**
   * Create a search query for QuestionnaireResponse.
   *
   * @param questionnaireId The ID of the questionnaire.
   * @param subjectId The ID of the subject.
   * @param subjectType The type of the subject.
   * @return The search query for QuestionnaireResponse.
   */
  private fun createQuestionnaireResponseSearchQuery(
    questionnaireId: String,
    subjectId: String,
    subjectType: ResourceType
  ): Search {
    return Search(ResourceType.QuestionnaireResponse).apply {
      filter(QuestionnaireResponse.SUBJECT, { value = "$subjectType/$subjectId" })
      filter(QuestionnaireResponse.QUESTIONNAIRE, { value = "${ResourceType.Questionnaire}/$questionnaireId" })
      count = 1
      from = 0
    }
  }

  /**
   * Retrieve the Binary resource for the given binary ID.
   *
   * @param binaryId The ID of the binary resource.
   * @return The Binary resource if found, otherwise null.
   */
  suspend fun retrieveBinary(binaryId: String): Binary? {
    return defaultRepository.loadResource<Binary>(binaryId)
  }

  /**
   * Retrieve the Patient resource for the given patient ID.
   *
   * @param patientId The ID of the binary resource.
   * @return The Patient resource if found, otherwise null.
   */
  suspend fun retrievePatient(patientId: String): Patient? {
    return defaultRepository.loadResource<Patient>(patientId)
  }
}
