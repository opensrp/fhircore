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

import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

class EventBusTest : RobolectricTest() {

  var eventBus = EventBus()
  lateinit var emittedEvents: MutableList<AppEvent>

  @Before
  fun setUp() {
    emittedEvents = mutableListOf()
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun testTriggerEventEmitsLoginEvent1() {

    runBlockingTest {
      val collectJob = launch { eventBus.events.collect { emittedEvents.add(it) } }
      eventBus.triggerEvent(AppEvent.Login)
      collectJob.cancel()
    }

    assertEquals(AppEvent.Login, emittedEvents[0])
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun testTriggerEventEmitsLogoutEvent1() {

    runBlockingTest {
      val collectJob = launch { eventBus.events.collect { emittedEvents.add(it) } }
      eventBus.triggerEvent(AppEvent.Logout)
      collectJob.cancel()
    }

    assertEquals(AppEvent.Logout, emittedEvents[0])
  }
}
