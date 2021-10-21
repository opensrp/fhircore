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

package org.smartregister.fhircore.anc.ui.family.details

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.spyk
import io.mockk.verify
import java.text.SimpleDateFormat
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Period
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

class FamilyDetailScreenKtTest : RobolectricTest() {

  @get:Rule val composeRule = createComposeRule()

  private val listenerObjectSpy =
    spyk(
      object {
        fun onSeeAllEncounterClick() {
          // imitate see all encounter click action by doing nothing
        }
        fun onEncounterItemClick(item: Encounter) {
          // imitate encounter item click action by doing nothing
        }
        fun onMemberItemClick(memberItem: FamilyMemberItem) {
          // imitate member item click action by doing nothing
        }
        fun onAddMemberItemClick() {
          // imitate add member click action by doing nothing
        }
      }
    )

  @Test
  fun testSurfaceComponent() {
    composeRule.setContent { FamilyDetailScreen(getDummyDataProvider()) }

    // Top bar is displayed
    composeRule.onNodeWithText("All Families").assertExists()
    composeRule.onNodeWithText("All Families").assertIsDisplayed()

    // Family name is displayed
    composeRule.onNodeWithText("Doe").assertExists()
    composeRule.onNodeWithText("Doe").assertIsDisplayed()

    // Given name is displayed
    composeRule.onNodeWithText("John").assertExists()
    composeRule.onNodeWithText("John").assertIsDisplayed()
  }

  @Test
  fun testMemberHeadingComponent() {
    composeRule.setContent { MemberHeading() }
    composeRule.onNodeWithText("Members".uppercase()).assertExists()
    composeRule.onNodeWithText("Members".uppercase()).assertIsDisplayed()
  }

  @Test
  fun testMembersList() {
    val familyMember = FamilyMemberItem("James", "1", "18", "Male", false)
    val familyMembers = listOf(familyMember)

    composeRule.setContent {
      MembersList(familyMembers, { listenerObjectSpy.onAddMemberItemClick() }) {
        listenerObjectSpy.onMemberItemClick(familyMember)
      }
    }

    // Member name is displayed
    composeRule.onNodeWithText("James").assertExists()
    composeRule.onNodeWithText("James").assertIsDisplayed()

    // Forward arrow image is displayed
    composeRule.onNodeWithContentDescription("").assertExists()
    composeRule.onNodeWithContentDescription("").assertIsDisplayed()

    // Add member button is displayed
    composeRule.onNodeWithText("Add Member".uppercase()).assertExists()
    composeRule.onNodeWithText("Add Member".uppercase()).assertIsDisplayed()

    // clicking add member button should call 'onAddMemberItemClick' method of 'listenerObjectSpy'
    composeRule.onNodeWithText("Add Member".uppercase()).performClick()
    verify { listenerObjectSpy.onAddMemberItemClick() }
  }

  @Test
  fun testEncounterHeaderComponent() {
    composeRule.setContent { EncounterHeader { listenerObjectSpy.onSeeAllEncounterClick() } }
    // Encounter heading is displayed
    composeRule.onNodeWithText("Encounters".uppercase()).assertExists()
    composeRule.onNodeWithText("Encounters".uppercase()).assertIsDisplayed()

    // See All encounters buttons is displayed
    composeRule.onNodeWithText("See All".uppercase()).assertExists()
    composeRule.onNodeWithText("See All".uppercase()).assertIsDisplayed()

    // clicking see all button should call 'onSeeAllEncounterClick' method of 'listenerObjectSpy'
    composeRule.onNodeWithText("See All".uppercase()).performClick()
    verify { listenerObjectSpy.onSeeAllEncounterClick() }
  }

  @Test
  fun testEncounterList() {
    val item = dummyEncounter("Encounter 1", "2020-05-22")
    val encounters = listOf(item)
    composeRule.setContent {
      EncounterList(encounters) { listenerObjectSpy.onEncounterItemClick(item) }
    }

    // encounter text is displayed
    composeRule.onNodeWithText("Encounter 1").assertExists()
    composeRule.onNodeWithText("Encounter 1").assertIsDisplayed()

    // encounter date is displayed
    composeRule.onNodeWithText("May 22, 2020").assertExists()
    composeRule.onNodeWithText("May 22, 2020").assertIsDisplayed()
  }

  private fun dummyEncounter(text: String, periodStartDate: String): Encounter {
    return Encounter().apply {
      class_ = Coding("", "", text)
      period = Period().apply { start = SimpleDateFormat("yyyy-MM-dd").parse(periodStartDate) }
    }
  }
}
