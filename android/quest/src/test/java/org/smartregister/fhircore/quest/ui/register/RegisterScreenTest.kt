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

package org.smartregister.fhircore.quest.ui.register

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.register.NoResultsConfig
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class RegisterScreenTest : RobolectricTest() {
  @get:Rule val composeTestRule = createComposeRule()

  @get:Rule val composeRegisterTestRule = createComposeRule()

  val searchText = mutableStateOf("Search text")
  val currentPage: MutableState<Int> = mutableStateOf(0)
  private val noResults = NoResultsConfig()

  @Before
  fun setUp() {
    composeTestRule.setContent {
      NoRegisterDataView(modifier = Modifier, noResults = noResults, onClick = mockk())
    }
  }

  @Test
  fun testNoRegisterDataViewDisplaysNoTestTag() {
    composeTestRule
      .onNodeWithTag(NO_REGISTER_VIEW_COLUMN_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testCountAllNodeNoRegisterDataViewDisplaysNoTestTag() {
    composeTestRule
      .onAllNodesWithTag(NO_REGISTER_VIEW_COLUMN_TEST_TAG, useUnmergedTree = true)
      .assertCountEquals(1)
  }
}
