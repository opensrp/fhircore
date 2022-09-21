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

import android.content.Context
import android.content.Intent
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity

class QuestionnaireExtensionTest : RobolectricTest() {

  private val context = mockk<Context>(relaxUnitFun = true, relaxed = true)
  private val computedValuesMap: Map<String, Any> = mapOf(Pair("firstName", "John"))
  private val questionnaireConfig = QuestionnaireConfig(id = "remove_family")

  @Test
  fun testLaunchQuestionnaireLaunchesIntentWithCorrectValues() {
    context.launchQuestionnaire<QuestionnaireActivity>(
      questionnaireConfig = questionnaireConfig,
      computedValuesMap = computedValuesMap
    )

    val intentSlot = slot<Intent>()
    verify { context.startActivity(capture(intentSlot)) }

    val capturedQuestionnaireConfig =
      intentSlot.captured.getSerializableExtra(QuestionnaireActivity.QUESTIONNAIRE_CONFIG_KEY) as
        QuestionnaireConfig

    Assert.assertEquals("remove_family", capturedQuestionnaireConfig.id)

    val capturedComputedValuesMap =
      intentSlot.captured.getSerializableExtra(
        QuestionnaireActivity.QUESTIONNAIRE_COMPUTED_VALUES_MAP_KEY
      ) as
        Map<String, Any>?
    Assert.assertEquals("John", capturedComputedValuesMap!!["firstName"])
  }
}
