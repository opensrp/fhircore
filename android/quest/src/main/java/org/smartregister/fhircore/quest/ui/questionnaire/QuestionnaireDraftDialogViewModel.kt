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

package org.smartregister.fhircore.quest.ui.questionnaire

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import org.hl7.fhir.r4.model.AuditEvent
import org.hl7.fhir.r4.model.AuditEvent.AuditEventSourceComponent
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid

@HiltViewModel
class QuestionnaireDraftDialogViewModel
@Inject
constructor(
  val defaultRepository: DefaultRepository,
  val sharedPreferencesHelper: SharedPreferencesHelper,
) : ViewModel() {

  private val practitionerId: String? by lazy {
    sharedPreferencesHelper
      .read(SharedPreferenceKey.PRACTITIONER_ID.name, null)
      ?.extractLogicalIdUuid()
  }

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
      defaultRepository.applyDbTransaction {
        defaultRepository.update(questionnaireResponse)
        defaultRepository.addOrUpdate(
          resource = createDeleteDraftAuditEvent(questionnaireConfig, questionnaireResponse),
        )
      }
    }
  }

  fun createDeleteDraftAuditEvent(
    questionnaireConfig: QuestionnaireConfig,
    questionnaireResponse: QuestionnaireResponse,
  ): AuditEvent {
    return AuditEvent().apply {
      entity =
        listOf(
          AuditEvent.AuditEventEntityComponent().apply {
            what = Reference(questionnaireResponse.id)
          },
        )
      source =
        AuditEventSourceComponent().apply {
          observer =
            questionnaireConfig.resourceType?.let {
              questionnaireConfig.resourceIdentifier?.asReference(
                it,
              )
            }
        }
      agent =
        listOf(
          AuditEvent.AuditEventAgentComponent().apply {
            who = practitionerId?.asReference(ResourceType.Practitioner)
          },
        )
      type =
        Coding().apply {
          system = AUDIT_EVENT_SYSTEM
          code = AUDIT_EVENT_CODE
          display = AUDIT_EVENT_DISPLAY
        }
      period =
        Period().apply {
          start = Date()
          end = Date()
        }
    }
  }

  companion object {
    const val AUDIT_EVENT_SYSTEM = "http://smartregister.org/"
    const val AUDIT_EVENT_CODE = "delete_draft"
    const val AUDIT_EVENT_DISPLAY = "Delete Draft"
  }
}
