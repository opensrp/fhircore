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

package org.smartregister.fhircore.quest.ui.shared.model

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.core.os.bundleOf
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Enumerations
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.quest.ui.shared.QuestionnaireHandler

@HiltAndroidTest
class QuestionnaireHandlerTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  private val context =
    mockk<Context>(
      moreInterfaces = arrayOf(QuestionnaireHandler::class),
      relaxUnitFun = true,
      relaxed = true,
    )

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @Test
  fun testLaunchQuestionnaire() {
    val questionnaire = QuestionnaireConfig(id = "444")
    val params =
      listOf(
        ActionParameter(
          paramType = ActionParameterType.PREPOPULATE,
          linkId = "25cc8d26-ac42-475f-be79-6f1d62a44881",
          dataType = Enumerations.DataType.INTEGER,
          key = "maleCondomPreviousBalance",
          value = "100",
        ),
      )

    every { (context as QuestionnaireHandler).startForResult.launch(any()) } returns Unit

    (context as QuestionnaireHandler)
      .startForResult
      .launch(
        Intent(context, QuestionnaireActivity::class.java)
          .putExtras(
            QuestionnaireActivity.intentBundle(
              questionnaireConfig = questionnaire,
              actionParams = params,
            ),
          )
          .putExtras(bundleOf()),
      )

    verify { (context as QuestionnaireHandler).startForResult.launch(any()) }
  }

  @Test
  fun testOnSubmitQuestionnaire() = runTest {
    val activityResult = mockk<ActivityResult>(relaxed = true)

    (context as QuestionnaireHandler).onSubmitQuestionnaire(activityResult)

    coVerify { (context as QuestionnaireHandler).onSubmitQuestionnaire(activityResult) }
  }
}
