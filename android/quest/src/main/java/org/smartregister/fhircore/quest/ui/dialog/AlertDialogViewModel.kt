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

package org.smartregister.fhircore.quest.ui.dialog

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.asReference

@HiltViewModel
class AlertDialogViewModel
@Inject
constructor(
  val defaultRepository: DefaultRepository,
  val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
  suspend fun deleteDraft(questionnaireConfig: QuestionnaireConfig?) {
    if (
      questionnaireConfig == null ||
        questionnaireConfig.resourceIdentifier.isNullOrBlank() ||
        questionnaireConfig.resourceType == null
    ) {
      return
    }

    val questionnaireResponse =
      defaultRepository.searchQuestionnaireResponse(
        resourceId = questionnaireConfig.resourceIdentifier!!,
        resourceType = questionnaireConfig.resourceType!!,
        questionnaireId = questionnaireConfig.id,
        encounterId = null,
        questionnaireResponseStatus = QuestionnaireResponseStatus.INPROGRESS.toCode(),
      )

    if (questionnaireResponse != null) {
      questionnaireResponse.status = QuestionnaireResponseStatus.STOPPED
      defaultRepository.update(questionnaireResponse)
      defaultRepository.addOrUpdate(
        resource = createDeleteDraftFlag(questionnaireConfig, questionnaireResponse),
      )
    }
  }

  fun createDeleteDraftFlag(
    questionnaireConfig: QuestionnaireConfig,
    questionnaireResponse: QuestionnaireResponse,
  ): Flag {
    return Flag().apply {
      subject =
        questionnaireConfig.resourceType?.let {
          questionnaireConfig.resourceIdentifier?.asReference(
            it,
          )
        }
      identifier =
        listOf(
          Identifier().apply { value = questionnaireResponse.id },
        )
      status = Flag.FlagStatus.ACTIVE
      code =
        CodeableConcept().apply {
          coding =
            listOf(
              Coding().apply {
                system = FLAG_SYSTEM
                code = FLAG_CODE
                display = FLAG_DISPLAY
              },
            )
          text = FLAG_TEXT
        }
      period =
        Period().apply {
          start = Date()
          end = Date()
        }
    }
  }

  companion object {
    const val FLAG_SYSTEM = "http://smartregister.org/"
    const val FLAG_CODE = "delete_draft"
    const val FLAG_DISPLAY = "Delete Draft"
    const val FLAG_TEXT = "QR Draft has been deleted"
  }
}
