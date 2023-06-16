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

package org.smartregister.fhircore.quest.event

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
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
  @Inject lateinit var eventQueue: EventQueue<AppEvent>
  private lateinit var eventBus: EventBus
  lateinit var emittedEvents: MutableList<AppEvent>

  @Before
  fun setUp() {
    hiltRule.inject()
    emittedEvents = mutableListOf()
    eventBus = EventBus(eventQueue)
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun testTriggerEventEmitsRefreshCacheEvent() {
    val refreshCacheEvent = AppEvent.RefreshCache(QuestionnaireConfig("test-config"))

    runBlockingTest {
      val collectJob = launch {
        eventBus
          .events
          .getFor("TestTag")
          .onEach { appEvent -> emittedEvents.add(appEvent) }
          .launchIn(this)
      }
      eventBus.triggerEvent(refreshCacheEvent)
      collectJob.cancel()
    }

    assertEquals(refreshCacheEvent, emittedEvents[0])
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun testTriggerEventEmitsLogoutEvent1() {
    val onSubmitQuestionnaireEvent =
      AppEvent.OnSubmitQuestionnaire(
        QuestionnaireSubmission(
          questionnaireConfig = QuestionnaireConfig(id = "submit-questionnaire"),
          QuestionnaireResponse()
        )
      )

    runBlockingTest {
      val collectJob = launch {
        eventBus
          .events
          .getFor("TestTag")
          .onEach { appEvent -> emittedEvents.add(appEvent) }
          .launchIn(this)
      }
      eventBus.triggerEvent(onSubmitQuestionnaireEvent)
      collectJob.cancel()
    }

    assertEquals(onSubmitQuestionnaireEvent, emittedEvents[0])
  }
}
