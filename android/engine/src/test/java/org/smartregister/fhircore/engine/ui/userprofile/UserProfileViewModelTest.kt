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

package org.smartregister.fhircore.engine.ui.userprofile

import com.google.android.fhir.sync.State
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.auth.AuthenticationService
import org.smartregister.fhircore.engine.impl.FhirApplication
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.sync.SyncInitiator

class UserProfileViewModelTest {

  private val userProfileViewModel = mockk<UserProfileViewModel>(relaxed = true)

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

  private val configurableApplication = mockk<FhirApplication>()
  private val authenticationService = mockk<AuthenticationService>()
  private val syncBroadcaster = spyk<SyncBroadcaster>()

  @Before
  fun setUp() {
    syncBroadcaster.apply {
      syncInitiator = null
      syncListeners.clear()
      registerSyncListener(syncListenerSpy)
      registerSyncInitiator(syncInitiatorSpy)
    }
    every { configurableApplication.syncBroadcaster } returns syncBroadcaster
    every { configurableApplication.authenticationService } returns authenticationService
    every { userProfileViewModel.retrieveUsername() } returns "demo"
  }

  @Test
  fun testRunSync() {
    userProfileViewModel.runSync()
    verify { syncInitiatorSpy.runSync() }
  }

  @Test
  fun testRetrieveUsernameShouldReturnDemo() {
    val username = userProfileViewModel.retrieveUsername()
    Assert.assertEquals("demo", username)
  }
}
