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

package org.smartregister.fhircore.engine.ui.components

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.spyk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.ui.components.register.NoResults
import org.smartregister.fhircore.engine.ui.components.register.RegisterHeader
import org.smartregister.fhircore.engine.ui.components.register.SEARCH_HEADER_TEXT_TAG
import org.smartregister.fhircore.quest.ui.components.RegisterFooter
import org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_NEXT_BUTTON_TAG
import org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_PAGINATION_TAG
import org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_PREVIOUS_BUTTON_TAG
import org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_TAG

class PaginatedRegisterViewsKtTest : RobolectricTest() {

  private val registerBodyTag = "registerBodyTag"

  @get:Rule val composeRule = createComposeRule()

  private val listenerObjectSpy =
    spyk(
      object {
        fun onPreviousButtonClick() {
          // Imitate previous button click action by doing nothing
        }

        fun onNextButtonClick() {
          // Imitate next button click action by doing nothing
        }
      },
    )

  @Test
  fun testSearchHeaderComponent() {
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent { RegisterHeader(resultCount = 20) }
    composeRule.onNodeWithTag(SEARCH_HEADER_TEXT_TAG).assertExists()
    composeRule.onNodeWithTag(SEARCH_HEADER_TEXT_TAG).assertIsDisplayed()
    composeRule.onNodeWithTag(SEARCH_HEADER_TEXT_TAG).assertTextEquals("20 RESULT(S)")
  }

  @Test
  fun testSearchFooterWithTenAsResultCount() {
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      org.smartregister.fhircore.quest.ui.components.RegisterFooter(
        currentPageStateFlow = 1,
        pagesCountStateFlow = 3,
        previousButtonClickListener = { listenerObjectSpy.onPreviousButtonClick() },
        nextButtonClickListener = { listenerObjectSpy.onNextButtonClick() },
      )
    }
    composeRule
      .onNodeWithTag(org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_TAG)
      .assertExists()
    composeRule
      .onNodeWithTag(org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_TAG)
      .assertIsDisplayed()

    // Previous button not displayed
    composeRule
      .onNodeWithTag(
        org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_PREVIOUS_BUTTON_TAG,
      )
      .assertDoesNotExist()

    // Pagination text is "Page 1 of 3"
    composeRule
      .onNodeWithTag(org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_PAGINATION_TAG)
      .assertExists()
    composeRule
      .onNodeWithTag(org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_PAGINATION_TAG)
      .assertIsDisplayed()
    composeRule
      .onNodeWithTag(org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_PAGINATION_TAG)
      .assertTextEquals("Page 1 of 3")

    // Next button is displayed
    composeRule
      .onNodeWithTag(org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_NEXT_BUTTON_TAG)
      .assertExists()

    // Clicking next button should call 'onNextButtonClick' method of 'listenerObjectSpy'
    composeRule
      .onNodeWithTag(org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_NEXT_BUTTON_TAG)
      .performClick()
    verify { listenerObjectSpy.onNextButtonClick() }
  }

  @Test
  fun testSearchFooterWithTenAsResultsSplitInThreePages() {
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      org.smartregister.fhircore.quest.ui.components.RegisterFooter(
        currentPageStateFlow = 3,
        pagesCountStateFlow = 3,
        previousButtonClickListener = { listenerObjectSpy.onPreviousButtonClick() },
        nextButtonClickListener = { listenerObjectSpy.onNextButtonClick() },
      )
    }
    composeRule
      .onNodeWithTag(org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_TAG)
      .assertExists()
    composeRule
      .onNodeWithTag(org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_TAG)
      .assertIsDisplayed()

    // Previous button is displayed
    composeRule
      .onNodeWithTag(
        org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_PREVIOUS_BUTTON_TAG,
      )
      .assertExists()

    // Clicking previous button should call 'onPreviousButtonClick' method of 'listenerObjectSpy'
    composeRule
      .onNodeWithTag(
        org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_PREVIOUS_BUTTON_TAG,
      )
      .performClick()
    verify { listenerObjectSpy.onPreviousButtonClick() }

    // Pagination text is "Page 3 of 3"
    composeRule
      .onNodeWithTag(org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_PAGINATION_TAG)
      .assertExists()
    composeRule
      .onNodeWithTag(org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_PAGINATION_TAG)
      .assertIsDisplayed()
    composeRule
      .onNodeWithTag(org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_PAGINATION_TAG)
      .assertTextEquals("Page 3 of 3")

    // Next button is not displayed
    composeRule
      .onNodeWithTag(org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_NEXT_BUTTON_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun testSearchFooterWithResultsFittingOnePage() {
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      org.smartregister.fhircore.quest.ui.components.RegisterFooter(
        currentPageStateFlow = 1,
        pagesCountStateFlow = 1,
        previousButtonClickListener = { listenerObjectSpy.onPreviousButtonClick() },
        nextButtonClickListener = { listenerObjectSpy.onNextButtonClick() },
      )
    }
    composeRule
      .onNodeWithTag(org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_TAG)
      .assertExists()
    composeRule
      .onNodeWithTag(org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_TAG)
      .assertIsDisplayed()

    // Previous button is not displayed
    composeRule
      .onNodeWithTag(
        org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_PREVIOUS_BUTTON_TAG,
      )
      .assertDoesNotExist()

    // Pagination text is "Page 1 of 1"
    composeRule
      .onNodeWithTag(org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_PAGINATION_TAG)
      .assertExists()
    composeRule
      .onNodeWithTag(org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_PAGINATION_TAG)
      .assertIsDisplayed()
    composeRule
      .onNodeWithTag(org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_PAGINATION_TAG)
      .assertTextEquals("Page 1 of 1")

    // Next button is not displayed
    composeRule
      .onNodeWithTag(org.smartregister.fhircore.quest.ui.components.SEARCH_FOOTER_NEXT_BUTTON_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun testNoResultsComponent() {
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent { NoResults() }
    composeRule.onNodeWithText("No results", useUnmergedTree = true).assertExists()
    composeRule.onNodeWithText("No results", useUnmergedTree = true).assertIsDisplayed()
  }

  @Composable
  private fun RegisterBody() {
    Text(text = "Nothing in particular", modifier = Modifier.testTag(registerBodyTag))
  }
}
