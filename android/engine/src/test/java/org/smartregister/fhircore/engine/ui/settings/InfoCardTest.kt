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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.domain.util.DataLoadState
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class InfoCardTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val coroutineRule = CoroutineTestRule()

  @get:Rule(order = 2) val composeRule = createComposeRule()

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @Test
  fun infoCard_ShowsLoading_WhenStateIsLoading() {
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent { InfoCard(profileData = MutableLiveData(DataLoadState.Loading)) }
    composeRule.onNodeWithTag("ProgressBarItem").assertExists()
  }

  @Test
  fun infoCard_ShowsError_WhenStateIsError() {
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      InfoCard(profileData = MutableLiveData(DataLoadState.Error(Exception("Test Error"))))
    }
    composeRule.onNodeWithText("Something went wrong while fetching data..").assertExists()
  }

  @Test
  fun infoCard_ShowsContent_WhenStateIsSuccess() {
    val mockData =
      ProfileData(userName = "Test User", isUserValid = true, practitionerDetails = null)
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      InfoCard(profileData = MutableLiveData(DataLoadState.Success(mockData)))
    }
    composeRule.onNodeWithText("Test User", ignoreCase = true).assertExists()
  }

  @Test
  fun fieldCard_TogglesContent_WhenClicked() {
    val fieldData = FieldData("1", "Test Value")
    composeRule.setContent { FieldCard(fieldData = fieldData) }

    composeRule.onNodeWithText("Test Value").performClick()
    composeRule.onNodeWithText("1").assertExists()
  }
}
