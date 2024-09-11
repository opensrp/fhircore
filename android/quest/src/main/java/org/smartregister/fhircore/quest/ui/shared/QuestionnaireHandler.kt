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

package org.smartregister.fhircore.quest.ui.shared

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.core.os.bundleOf
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity

const val ON_RESULT_TYPE = "onResultType"

enum class ActivityOnResultType {
  LOCATION,
  QUESTIONNAIRE,
}

interface QuestionnaireHandler {

  val startForResult: ActivityResultLauncher<Intent>

  /** This function launches [QuestionnaireActivity] and returns [ActivityResult] on submission. */
  fun launchQuestionnaire(
    context: Context,
    extraIntentBundle: Bundle = bundleOf(),
    questionnaireConfig: QuestionnaireConfig? = null,
    actionParams: List<ActionParameter>,
  ) {
    if (questionnaireConfig != null) {
      startForResult.launch(
        Intent(context, QuestionnaireActivity::class.java)
          .putExtras(
            QuestionnaireActivity.intentBundle(
              questionnaireConfig = questionnaireConfig,
              actionParams = actionParams,
            ),
          )
          .putExtras(extraIntentBundle),
      )
      onQuestionnaireLaunched(questionnaireConfig)
    }
  }

  fun onQuestionnaireLaunched(questionnaireConfig: QuestionnaireConfig)

  suspend fun onSubmitQuestionnaire(activityResult: ActivityResult)
}
