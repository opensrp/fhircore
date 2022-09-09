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

package org.smartregister.fhircore.quest.util.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity.Companion.intentArgs

inline fun <reified Q : QuestionnaireActivity> Context.launchQuestionnaire(
  intentBundle: Bundle = Bundle.EMPTY,
  questionnaireConfig: QuestionnaireConfig? = null,
  computedValuesMap: Map<String, Any>? = emptyMap()
) {
  this.startActivity(
    Intent(this, Q::class.java)
      .putExtras(intentBundle)
      .putExtras(
        intentArgs(questionnaireConfig = questionnaireConfig, computedValuesMap = computedValuesMap)
      )
  )
}

inline fun <reified Q : QuestionnaireActivity> Context.launchQuestionnaireForResult(
  backReference: String? = null,
  intentBundle: Bundle = Bundle.EMPTY,
  questionnaireConfig: QuestionnaireConfig,
  computedValuesMap: Map<String, Any>? = emptyMap()
) {
  (this as Activity).startActivityForResult(
    Intent(this, Q::class.java)
      .putExtras(intentBundle)
      .putExtras(
        intentArgs(
          backReference = backReference,
          questionnaireConfig = questionnaireConfig,
          computedValuesMap = computedValuesMap
        )
      ),
    0
  )
}
