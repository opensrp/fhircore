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

package org.smartregister.fhircore.quest.ui.questionnaire

import androidx.lifecycle.ViewModel
import ca.uhn.fhir.parser.IParser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.task.FhirCarePlanGenerator
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString

@HiltViewModel
class QuestionnaireFragmentViewModel
@Inject
constructor(
  val parser: IParser,
  val defaultRepository: DefaultRepository,
  val dispatcherProvider: DispatcherProvider,
  val fhirCarePlanGenerator: FhirCarePlanGenerator
) : ViewModel() {

  suspend fun retrieveQuestionnaireJson(questionnaireConfig: QuestionnaireConfig): String {
    if (questionnaireConfig.id.isEmpty()) return ""
    return defaultRepository
      .loadResource<Questionnaire>(questionnaireConfig.id)
      ?.encodeResourceToString()
      ?: ""
  }

  fun handleQuestionnaireSubmission(
    questionnaireResponse: QuestionnaireResponse,
    questionnaireConfig: QuestionnaireConfig
  ) {
    TODO("Not yet implemented")
  }
}
