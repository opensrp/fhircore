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

package org.smartregister.fhircore.engine.ui.settings

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsScreenKtTest : RobolectricTest() {

  @get:Rule(order = 1) val coroutineRule = CoroutineTestRule()
  @get:Rule(order = 2) val composeRule = createComposeRule()

  private val appContext = ApplicationProvider.getApplicationContext<Context>()
  private val settingsViewModel = mockk<SettingsViewModel>()
  private val devMenuViewModel = mockk<DevViewModel>()

  @Test
  fun testSettingsScreenShowsUserProfileRows() = runTest {
    coEvery { devMenuViewModel.getResourcesToReport() } returns emptyMap()
    coEvery { settingsViewModel.data } returns MutableLiveData()
    composeRule.setContent {
      SettingsScreen(settingsViewModel = settingsViewModel, devViewModel = devMenuViewModel)
    }

    composeRule.onNodeWithText(appContext.getString(R.string.re_fetch_practitioner)).assertExists()
    composeRule.onNodeWithText(appContext.getString(R.string.sync)).assertExists()
    composeRule.onNodeWithText(appContext.getString(R.string.dev_menu)).assertExists()
    composeRule.onNodeWithText(appContext.getString(R.string.logout)).assertExists()
  }
}
