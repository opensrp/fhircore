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

package org.smartregister.fhircore.quest.event

import com.google.android.fhir.datacapture.extensions.logicalId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.shared.models.QuestionnaireSubmission

@HiltAndroidTest
class EventBusTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var eventBus: EventBus

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @Test
  fun testTriggerEventEmitsLogoutEvent1() {
    runTest {
      val onSubmitQuestionnaireEvent =
        AppEvent.OnSubmitQuestionnaire(
          QuestionnaireSubmission(
            questionnaireConfig = QuestionnaireConfig(id = "questionnaire1"),
            questionnaireResponse = QuestionnaireResponse().apply { id = "questionnaireResponse1" },
          ),
        )

      val job =
        eventBus.events
          .getFor("thisConsumer")
          .onEach {
            assertTrue(it is AppEvent.OnSubmitQuestionnaire)
            assertEquals(
              onSubmitQuestionnaireEvent.questionnaireSubmission.questionnaireConfig.id,
              it.questionnaireSubmission.questionnaireConfig.id,
            )
            assertEquals(
              onSubmitQuestionnaireEvent.questionnaireSubmission.questionnaireResponse.logicalId,
              it.questionnaireSubmission.questionnaireResponse.logicalId,
            )
          }
          .launchIn(this)

      eventBus.triggerEvent(onSubmitQuestionnaireEvent)
      job.cancel()
    }
  }
}
