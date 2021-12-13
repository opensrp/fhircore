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
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import java.text.SimpleDateFormat
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Period
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

@HiltAndroidTest
class FamilyDetailScreenTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val composeRule = createComposeRule()

  @Before
  fun setUp() {
    hiltRule.inject()

    val viewModel =
      spyk(
        FamilyDetailViewModel(
          mockk {
            coEvery { fetchDemographics(any()) } returns
              Patient().apply {
                addName().apply {
                  family = "John"
                  addGiven("Doe")
                }
              }

            coEvery { fetchFamilyMembers(any()) } returns
              listOf(
                FamilyMemberItem("Jane", "1", "18", "Male", pregnant = true, houseHoldHead = true)
              )

            coEvery { fetchFamilyCarePlans(any()) } returns
              listOf(CarePlan().apply { id = "CarePlan/1" })

            coEvery { fetchEncounters("") } returns
              listOf(
                Encounter().apply {
                  class_ = Coding("", "", "Encounter 1")
                  period =
                    Period().apply { start = SimpleDateFormat("yyyy-MM-dd").parse("2020-05-22") }
                }
              )
          }
        )
      )

    viewModel.fetchDemographics("")
    viewModel.fetchFamilyMembers("")
    viewModel.fetchCarePlans("")
    viewModel.fetchEncounters("")

    composeRule.setContent { FamilyDetailScreen(viewModel) }
  }

  @Test
  fun testSurfaceComponent() {

    // Top bar is displayed
    composeRule.onNodeWithText("All Families").assertExists().assertIsDisplayed()

    // Family name given is displayed
    composeRule.onNodeWithText("John Doe").assertExists().assertIsDisplayed()
  }

  @Test
  fun testMemberHeadingComponent() {
    composeRule.onNodeWithText("Members".uppercase()).assertExists()
    composeRule.onNodeWithText("Members".uppercase()).assertIsDisplayed()
  }

  @Test
  fun testMembersList() {

    // Member name is displayed
    composeRule.onNodeWithText("Jane").assertExists()
    composeRule.onNodeWithText("Jane").assertIsDisplayed()

    // Forward arrow image is displayed
    composeRule.onNodeWithContentDescription("Forward arrow").assertExists()
    composeRule.onNodeWithContentDescription("Forward arrow").assertIsDisplayed()
  }

  @Test
  fun testMembersListWithPregnantHeadOfHouseHold() {

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
    // Encounter heading is displayed
    composeRule.onNodeWithText("Encounters".uppercase()).assertExists()

    // See All encounters buttons is displayed
    composeRule.onAllNodesWithText("See All".uppercase())[1].assertExists()
  }

  @Test
  fun testEncounterList() {

    // encounter text is displayed
    composeRule.onNodeWithText("Encounter 1").assertExists()

    // encounter date is displayed
    composeRule.onNodeWithText("22-May-2020").assertExists()
  }

  @Test
  fun testHouseHoldTaskHeading() {
    composeRule.onNodeWithText("Household Tasks".uppercase()).assertExists()
    composeRule.onNodeWithText("Household Tasks".uppercase()).assertIsDisplayed()
  }

  @Test
  fun testMonthlyVisitHeading() {
    composeRule.onNodeWithText("+Monthly Visit".uppercase()).assertExists()
  }

  @Test
  fun testUpcomingServiceHeader() {

    // Upcoming services header displayed
    composeRule.onNodeWithText("Upcoming Services".uppercase()).assertExists()

    // See all upcoming services button displayed
    composeRule.onAllNodesWithText("See All".uppercase())[0].assertExists()
  }
}
