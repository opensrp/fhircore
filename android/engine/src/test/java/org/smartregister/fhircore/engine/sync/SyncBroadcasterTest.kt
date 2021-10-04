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
}
