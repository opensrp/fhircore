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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.shadow.FakeKeyStore
import org.smartregister.fhircore.engine.sync.SyncInitiator

class UserProfileScreenKtTest : RobolectricTest() {

  private lateinit var userProfileViewModel: UserProfileViewModel

  @get:Rule val composeRule = createComposeRule()

  @Before
  fun setUp() {
    userProfileViewModel =
      spyk(objToCopy = UserProfileViewModel(ApplicationProvider.getApplicationContext()))
    composeRule.setContent { UserProfileScreen(userProfileViewModel = userProfileViewModel) }
  }

  @Test
  fun testUserProfileShouldDisplayCorrectContent() {
    composeRule.onNodeWithText("Demo").assertExists()
    composeRule.onNodeWithText("Sync").assertExists()
    composeRule.onNodeWithText("Log out").assertExists()
  }

  @Test
  fun testSyncRowClickShouldInitiateSync() {
    val mockSyncInitiator = mockk<SyncInitiator> { every { runSync() } returns Unit }

    every { userProfileViewModel.configurableApplication } returns
      mockk {
        every { syncBroadcaster } returns
          mockk { every { syncInitiator } returns mockSyncInitiator }
      }

    composeRule.onNodeWithText("Sync").performClick()
    verify { mockSyncInitiator.runSync() }
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun setupMocks() {
      FakeKeyStore.setup
    }
  }
}
