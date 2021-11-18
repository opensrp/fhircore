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
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

class FamilyDetailScreenTest : RobolectricTest() {

  @get:Rule val composeRule = createComposeRule()

  private val listenerObjectSpy =
    spyk(
      object {

        fun onSeeAllUpcomingServiceClick() {

          // imitate see all upcoming services click action by doing nothing
        }
        fun onAddMemberItemClick() {

          // imitate add member click action by doing nothing
        }
        fun onMemberItemClick(memberItem: FamilyMemberItem) {

          // imitate member item click action by doing nothing
        }
      }
    )

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

  @Test
  fun testMembersListWithPregnantHeadOfHouseHold() {
    val familyMember = FamilyMemberItem("Jane", "1", "18", "Female", true, true)
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
}
