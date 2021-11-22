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
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import javax.inject.Inject
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.shadow.FakeKeyStore
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.sync.SyncInitiator

@HiltAndroidTest
class UserProfileScreenKtTest : RobolectricTest() {

  lateinit var userProfileViewModel: UserProfileViewModel

  @Inject lateinit var syncBroadcaster: SyncBroadcaster

  @get:Rule val composeRule = createComposeRule()

  @get:Rule var hiltRule = HiltAndroidRule(this)

  @Before
  fun setUp() {
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
    syncBroadcaster.unRegisterSyncInitiator()
    syncBroadcaster.registerSyncInitiator(mockSyncInitiator)
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
