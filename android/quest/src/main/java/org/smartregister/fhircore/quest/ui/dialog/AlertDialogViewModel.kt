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
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DispatcherProvider

@HiltViewModel
class AlertDialogViewModel
@Inject
constructor(
  val defaultRepository: DefaultRepository,
  val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
  fun deleteDraft(questionnaireConfig: QuestionnaireConfig?) {
    if (
      questionnaireConfig == null ||
        questionnaireConfig.resourceIdentifier.isNullOrBlank() ||
        questionnaireConfig.resourceType == null
    ) {
      return
    }

    viewModelScope.launch {
      withContext(dispatcherProvider.io()) {
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
        }
      }
    }
  }
}
