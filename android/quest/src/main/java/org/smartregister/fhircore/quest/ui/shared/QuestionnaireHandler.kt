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

interface QuestionnaireHandler {

  val startForResult: ActivityResultLauncher<Intent>

  fun <T> launchQuestionnaire(
    context: Context,
    intentBundle: Bundle = bundleOf(),
    questionnaireConfig: QuestionnaireConfig? = null,
    computedValuesMap: Map<String, Any>?,
    actionParams: List<ActionParameter> = emptyList()
  ) {
    startForResult.launch(
      Intent(context, QuestionnaireActivity::class.java)
        .putExtras(
          QuestionnaireActivity.intentArgs(
            questionnaireConfig = questionnaireConfig,
            computedValuesMap = computedValuesMap,
            actionParams = actionParams
          )
        )
        .putExtras(intentBundle)
    )
  }

  fun onSubmitQuestionnaire(activityResult: ActivityResult)
}
