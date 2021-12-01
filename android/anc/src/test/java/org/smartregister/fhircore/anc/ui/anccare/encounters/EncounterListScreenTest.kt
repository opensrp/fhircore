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

package org.smartregister.fhircore.anc.ui.anccare.encounters

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import io.mockk.mockk
import java.text.SimpleDateFormat
import org.hl7.fhir.r4.model.Encounter
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.data.EncounterRepository
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

class EncounterListScreenTest : RobolectricTest() {

  @get:Rule val composeRule = createComposeRule()

  @Ignore("Fix tracked on https://github.com/opensrp/fhircore/issues/760")
  @Test
  fun testEncounterListScreen() {
    composeRule.setContent { EncounterListScreen(EncounterListViewModel(mockk())) }

    // verify top bar is displayed
    composeRule.onNodeWithText("Past encounters").assertExists()
    composeRule.onNodeWithText("Past encounters").assertIsDisplayed()

    // verify back arrow is displayed
    composeRule.onNodeWithContentDescription("Back arrow").assertExists()
    composeRule.onNodeWithContentDescription("Back arrow").assertIsDisplayed()
  }

  @Test
  fun testEncounterItem() {
    val encounterDate = SimpleDateFormat("yyyy-MM-dd").parse("2020-05-22")
    val encounterItem =
      org.smartregister.fhircore.anc.data.model.EncounterItem(
        "1",
        Encounter.EncounterStatus.FINISHED,
        "Dummy",
        encounterDate
      )
    composeRule.setContent { EncounterItemRow(encounterItem, false) }

    // verify encounter date is displayed
    composeRule.onNodeWithText("22-May-2020").assertExists()
    composeRule.onNodeWithText("22-May-2020").assertIsDisplayed()

    // verify status is displayed
    composeRule.onNodeWithText(Encounter.EncounterStatus.FINISHED.name).assertExists()
    composeRule.onNodeWithText(Encounter.EncounterStatus.FINISHED.name).assertIsDisplayed()
  }

  @Test
  fun testLoadingItem() {
    composeRule.setContent { LoadingItem() }

    // verify loading item exists
    composeRule.onNodeWithTag("ProgressBarItem").assertExists()
  }
}
