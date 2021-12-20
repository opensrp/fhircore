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

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.text.SimpleDateFormat
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Period
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

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
  fun testSurfaceComponent() {
    val viewModel =
      FamilyDetailViewModel(
        mockk {
          coEvery { fetchDemographics(any()) } returns
            Patient().apply {
              addName().apply {
                family = "John"
                addGiven("Doe")
              }
            }
        }
      )
    viewModel.fetchDemographics("")

    composeRule.setContent { FamilyDetailScreen(viewModel) }

    // Top bar is displayed
    composeRule.onNodeWithText("All Families").assertExists()
    composeRule.onNodeWithText("All Families").assertIsDisplayed()

    // Family name given is displayed
    composeRule.onNodeWithText("John Doe").assertExists()
    composeRule.onNodeWithText("John Doe").assertIsDisplayed()
  }

  @Test
  fun testMemberHeadingComponent() {
    composeRule.setContent { MemberHeading { listenerObjectSpy.onAddMemberItemClick() } }
    composeRule.onNodeWithText("Members".uppercase()).assertExists()
    composeRule.onNodeWithText("Members".uppercase()).assertIsDisplayed()
  }

  @Test
  fun testMembersList() {
    val familyMember = FamilyMemberItem("James", "1", "18", "Male", false, false)
    val familyMembers = listOf(familyMember)

    composeRule.setContent {
      MembersList(familyMembers) { listenerObjectSpy.onMemberItemClick(familyMember) }
    }

    // Member name is displayed
    composeRule.onNodeWithText("James").assertExists()
    composeRule.onNodeWithText("James").assertIsDisplayed()

    // Forward arrow image is displayed
    composeRule.onNodeWithContentDescription("Forward arrow").assertExists()
    composeRule.onNodeWithContentDescription("Forward arrow").assertIsDisplayed()
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

  @Test
  fun testToolbarHasBackArrowActionButton() {
    val application = ApplicationProvider.getApplicationContext<Application>()

    composeRule.setContent { FamilyDetailScreen(FamilyDetailViewModel(mockk())) }
    composeRule
      .onNodeWithTag(TOOLBAR_TITLE)
      .assertTextEquals(application.getString(R.string.all_families))
    composeRule.onNodeWithContentDescription("Back arrow").assertHasClickAction()
  }

  @Test
  fun testToolbarHasRemoveFamilyMenuItemInOverflowMenu() {
    val application = ApplicationProvider.getApplicationContext<Application>()

    composeRule.setContent { FamilyDetailScreen(FamilyDetailViewModel(mockk())) }
    composeRule.onNodeWithTag(TOOLBAR_MENU_BUTTON).assertHasClickAction().performClick()
    composeRule.onNodeWithTag(TOOLBAR_MENU).assertIsDisplayed()
    composeRule
      .onNodeWithTag(TOOLBAR_MENU)
      .onChildAt(0)
      .assertTextEquals(application.getString(R.string.remove_family))
      .assertHasClickAction()
  }

  @Test
  fun testToolbarMenuButtonShouldToggleMenuItemsList() {

    composeRule.setContent { FamilyDetailScreen(FamilyDetailViewModel(mockk())) }

    composeRule.onNodeWithTag(TOOLBAR_MENU).assertDoesNotExist()
    composeRule.onNodeWithTag(TOOLBAR_MENU_BUTTON).performClick()

    composeRule
      .onNodeWithTag(TOOLBAR_MENU)
      .assertExists()
      .assertIsDisplayed()
      .onChildren()
      .assertCountEquals(1)

    composeRule.onNodeWithTag(TOOLBAR_MENU_BUTTON).performClick()
    composeRule.onNodeWithTag(TOOLBAR_MENU).assertDoesNotExist()
  }

  @Test
  fun testToolbarRemoveFamilyMenuItemShouldCallRemoveFamilyMenuItemClickedListener() {

    val familyDetailViewModel = FamilyDetailViewModel(mockk(relaxed = true))
    Assert.assertFalse(familyDetailViewModel.isRemoveFamilyMenuItemClicked.value!!)

    composeRule.setContent { FamilyDetailScreen(familyDetailViewModel) }

    composeRule.onNodeWithTag(TOOLBAR_MENU_BUTTON).performClick()
    composeRule.onNodeWithTag(TOOLBAR_MENU).onChildAt(0).performClick()

    Assert.assertNotNull(familyDetailViewModel.isRemoveFamilyMenuItemClicked.value)
    Assert.assertTrue(familyDetailViewModel.isRemoveFamilyMenuItemClicked.value!!)
  }

  private fun dummyEncounter(text: String, periodStartDate: String): Encounter {
    return Encounter().apply {
      class_ = Coding("", "", text)
      period = Period().apply { start = SimpleDateFormat("yyyy-MM-dd").parse(periodStartDate) }
    }
  }
}
