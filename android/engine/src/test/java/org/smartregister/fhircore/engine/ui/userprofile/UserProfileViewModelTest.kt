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

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import junit.framework.Assert.assertTrue
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers.setField
import org.smartregister.fhircore.engine.auth.AuthenticationService
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.sync.SyncInitiator

class UserProfileViewModelTest : RobolectricTest() {

  private lateinit var userProfileViewModel: UserProfileViewModel
  private lateinit var appConfig: ConfigurableApplication

  @Before
  fun setUp() {
    appConfig = mockk()
    userProfileViewModel = spyk(UserProfileViewModel(ApplicationProvider.getApplicationContext()))
    setField(userProfileViewModel, "configurableApplication", appConfig)
  }

  @Test
  fun testRunSync() {
    val mockSyncInitiator = mockk<SyncInitiator> { every { runSync() } returns Unit }

    every { appConfig.syncBroadcaster } returns
      mockk { every { syncInitiator } returns mockSyncInitiator }

    userProfileViewModel.runSync()
    verify { mockSyncInitiator.runSync() }
  }

  @Test
  fun testRetrieveUsernameShouldReturnDemo() {

    every { appConfig.secureSharedPreference } returns
      mockk { every { retrieveSessionUsername() } returns "demo" }
    val username = userProfileViewModel.retrieveUsername()
    Assert.assertEquals("demo", username)
  }

  @Test
  fun testLogoutUserShouldCallAuthLogoutService() {

    val authService = mockk<AuthenticationService> { every { logout() } returns Unit }
    every { appConfig.authenticationService } returns authService

    userProfileViewModel.logoutUser()
    verify(exactly = 1) { authService.logout() }
    shadowOf(Looper.getMainLooper()).idle()
    assertTrue(userProfileViewModel.onLogout.value!!)
  }
}
