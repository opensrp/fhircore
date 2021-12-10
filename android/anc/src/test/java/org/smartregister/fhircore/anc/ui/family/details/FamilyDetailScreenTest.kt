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
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.text.SimpleDateFormat
import java.util.Date
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Period
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.makeItReadable
import org.smartregister.fhircore.engine.util.extension.plusYears

class FamilyDetailScreenTest : RobolectricTest() {

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
        fun onSeeAllUpcomingServiceClick() {

          // imitate see all upcoming services click action by doing nothing
        }
      }
    )

  @Test
  @Ignore("Fix")
  fun testSurfaceComponent() {
    composeRule.setContent { FamilyDetailScreen(FamilyDetailViewModel(mockk())) }

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
    composeRule.setContent { MemberHeading(listenerObjectSpy::onAddMemberItemClick, {}) }
    composeRule.onNodeWithText("Members".uppercase()).assertExists()
    composeRule.onNodeWithText("Members".uppercase()).assertIsDisplayed()
  }

  @Test
  fun testMembersList() {
    val familyMember =
      FamilyMemberItem("James", "1", Date().plusYears(-18), "Male", false, false, Date(), 2, 4)
    val familyMembers = listOf(familyMember)

    composeRule.setContent {
      MembersList(familyMembers) { listenerObjectSpy.onMemberItemClick(familyMember) }
    }

    // Member name is displayed
    composeRule.onNodeWithText("James").assertExists()
    composeRule.onNodeWithText("James").assertIsDisplayed()

    composeRule.onNodeWithText("Deceased(" + Date().makeItReadable() + ")").assertIsDisplayed()

    // Forward arrow image is displayed
    composeRule.onNodeWithContentDescription("Forward arrow").assertExists()
    composeRule.onNodeWithContentDescription("Forward arrow").assertIsDisplayed()
  }

  @Test
  fun testMembersListWithPregnantHeadOfHouseHold() {
    val familyMember =
      FamilyMemberItem("Jane", "1", Date().plusYears(-18), "Female", true, true, null, 1, 2)
    val familyMembers = listOf(familyMember)

    composeRule.setContent {
      MembersList(familyMembers) { listenerObjectSpy.onMemberItemClick(familyMember) }
    }

    // Member name is displayed
    composeRule.onNodeWithText("Jane").assertExists()
    composeRule.onNodeWithText("Jane").assertIsDisplayed()

    // Head of household label displayed
    composeRule.onNodeWithText("Head of household").assertExists()
    composeRule.onNodeWithText("Head of household").assertIsDisplayed()

    // Pregnant lady image is displayed
    composeRule.onNodeWithContentDescription("Pregnant woman").assertExists()
    composeRule.onNodeWithContentDescription("Pregnant woman").assertIsDisplayed()

    // ANC visit due text is displayed
    composeRule.onNodeWithText("ANC visit due").assertExists()
    composeRule.onNodeWithText("ANC visit due").assertIsDisplayed()

    // Forward arrow image is displayed
    composeRule.onNodeWithContentDescription("Forward arrow").assertExists()
    composeRule.onNodeWithContentDescription("Forward arrow").assertIsDisplayed()
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
    composeRule.onNodeWithText("22-May-2020").assertExists()
    composeRule.onNodeWithText("22-May-2020").assertIsDisplayed()
  }

  @Test
  fun testHouseHoldTaskHeading() {
    composeRule.setContent { HouseHoldTaskHeading() }
    composeRule.onNodeWithText("Household Tasks".uppercase()).assertExists()
    composeRule.onNodeWithText("Household Tasks".uppercase()).assertIsDisplayed()
  }

  @Test
  fun testMonthlyVisitHeading() {
    composeRule.setContent { MonthlyVisitHeading() }
    composeRule.onNodeWithText("+Monthly Visit".uppercase()).assertExists()
    composeRule.onNodeWithText("+Monthly Visit".uppercase()).assertIsDisplayed()
  }

  @Test
  fun testUpcomingServiceHeader() {
    composeRule.setContent {
      UpcomingServiceHeader { listenerObjectSpy.onSeeAllUpcomingServiceClick() }
    }

    // Upcoming services header displayed
    composeRule.onNodeWithText("Upcoming Services".uppercase()).assertExists()
    composeRule.onNodeWithText("Upcoming Services".uppercase()).assertIsDisplayed()

    // See all upcoming services button displayed
    composeRule.onNodeWithText("See All".uppercase()).assertExists()
    composeRule.onNodeWithText("See All".uppercase()).assertIsDisplayed()

    // clicking see all button should call 'onSeeAllUpcomingServiceClick' method of
    // 'listenerObjectSpy'
    composeRule.onNodeWithText("See All".uppercase()).performClick()
    verify { listenerObjectSpy.onSeeAllUpcomingServiceClick() }
  }

  private fun dummyEncounter(text: String, periodStartDate: String): Encounter {
    return Encounter().apply {
      class_ = Coding("", "", text)
      period = Period().apply { start = SimpleDateFormat("yyyy-MM-dd").parse(periodStartDate) }
    }
  }
}
