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

package org.smartregister.fhircore.engine.sync

import com.google.android.fhir.sync.State
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

internal class SyncBroadcasterTest {

  private val syncListenerSpy =
    spyk<OnSyncListener>(
      object : OnSyncListener {
        override fun onSync(state: State) {
          // onSync that does nothing
        }
      }
    )

  private val syncInitiatorSpy =
    spyk<SyncInitiator>(
      object : SyncInitiator {
        override fun runSync() {
          // SyncInitiator that does nothing
        }
      }
    )

  private val syncBroadcaster = SyncBroadcaster

  @BeforeEach
  fun setUp() {
    syncBroadcaster.apply {
      syncInitiator = null
      syncListeners.clear()
      registerSyncListener(syncListenerSpy)
    }
  }

  @Test
  @DisplayName("Should register sync listener")
  fun testListenerRegistration() {
    Assertions.assertEquals(1, syncBroadcaster.syncListeners.size)
    Assertions.assertNull(syncBroadcaster.syncInitiator)
  }

  @Test
  @DisplayName("Should call the onSync method when sync is triggered")
  fun testListenerOnSync() {
    val state = mockk<State>()
    syncBroadcaster.broadcastSync(state)
    verify { syncListenerSpy.onSync(state = state) }
    confirmVerified(syncListenerSpy)
  }

  @Test
  @DisplayName("Should register sync initiator once")
  fun testRegisterSyncInitiator() {
    syncBroadcaster.registerSyncInitiator(syncInitiatorSpy)
    Assertions.assertNotNull(syncBroadcaster.syncInitiator)
    verify { syncInitiatorSpy.runSync() }
    confirmVerified(syncInitiatorSpy)
  }

  @Test
  @DisplayName("Should remove listener")
  fun testUnRegisterSyncListener() {
    syncBroadcaster.unRegisterSyncListener(syncListenerSpy)
    Assertions.assertTrue(syncBroadcaster.syncListeners.isEmpty())
  }

  @Test
  @DisplayName("Should do nothing as a sync initiator has already been registered")
  fun testRegisterSyncInitiatorTwiceShouldDoNothing() {
    syncBroadcaster.registerSyncInitiator(syncInitiatorSpy)
    Assertions.assertNotNull(syncBroadcaster.syncInitiator)
  }
}
