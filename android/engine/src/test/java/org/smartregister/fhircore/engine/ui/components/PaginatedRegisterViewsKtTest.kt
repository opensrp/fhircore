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
import androidx.paging.LoadState
import io.mockk.spyk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.ui.components.register.NoResults
import org.smartregister.fhircore.engine.ui.components.register.RegisterFooter
import org.smartregister.fhircore.engine.ui.components.register.RegisterHeader
import org.smartregister.fhircore.engine.ui.components.register.SEARCH_FOOTER_NEXT_BUTTON_TAG
import org.smartregister.fhircore.engine.ui.components.register.SEARCH_FOOTER_PAGINATION_TAG
import org.smartregister.fhircore.engine.ui.components.register.SEARCH_FOOTER_PREVIOUS_BUTTON_TAG
import org.smartregister.fhircore.engine.ui.components.register.SEARCH_FOOTER_TAG
import org.smartregister.fhircore.engine.ui.components.register.SEARCH_HEADER_TEXT_TAG

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
      }
    )

  @Test
  fun testSearchHeaderComponent() {
    composeRule.setContent { RegisterHeader(resultCount = 20) }
    composeRule.onNodeWithTag(SEARCH_HEADER_TEXT_TAG).assertExists()
    composeRule.onNodeWithTag(SEARCH_HEADER_TEXT_TAG).assertIsDisplayed()
    composeRule.onNodeWithTag(SEARCH_HEADER_TEXT_TAG).assertTextEquals("20 RESULT(S)")
  }

  @Test
  fun testSearchFooterWithZeroAsResultCount() {
    composeRule.setContent {
      RegisterFooter(
        resultCount = 0,
        currentPage = 1,
        pagesCount = 1,
        previousButtonClickListener = { listenerObjectSpy.onPreviousButtonClick() },
        nextButtonClickListener = { listenerObjectSpy.onNextButtonClick() }
      )
    }
    composeRule.onNodeWithTag(SEARCH_FOOTER_TAG).assertDoesNotExist()
  }

  @Test
  fun testSearchFooterWithTenAsResultCount() {
    composeRule.setContent {
      RegisterFooter(
        resultCount = 50,
        currentPage = 1,
        pagesCount = 3,
        previousButtonClickListener = { listenerObjectSpy.onPreviousButtonClick() },
        nextButtonClickListener = { listenerObjectSpy.onNextButtonClick() }
      )
    }
    composeRule.onNodeWithTag(SEARCH_FOOTER_TAG).assertExists()
    composeRule.onNodeWithTag(SEARCH_FOOTER_TAG).assertIsDisplayed()

    // Previous button not displayed
    composeRule.onNodeWithTag(SEARCH_FOOTER_PREVIOUS_BUTTON_TAG).assertDoesNotExist()

    // Pagination text is "Page 1 of 3"
    composeRule.onNodeWithTag(SEARCH_FOOTER_PAGINATION_TAG).assertExists()
    composeRule.onNodeWithTag(SEARCH_FOOTER_PAGINATION_TAG).assertIsDisplayed()
    composeRule.onNodeWithTag(SEARCH_FOOTER_PAGINATION_TAG).assertTextEquals("Page 1 of 3")

    // Next button is displayed
    composeRule.onNodeWithTag(SEARCH_FOOTER_NEXT_BUTTON_TAG).assertExists()

    // Clicking next button should call 'onNextButtonClick' method of 'listenerObjectSpy'
    composeRule.onNodeWithTag(SEARCH_FOOTER_NEXT_BUTTON_TAG).performClick()
    verify { listenerObjectSpy.onNextButtonClick() }
  }

  @Test
  fun testSearchFooterWithTenAsResultsSplitInThreePages() {
    composeRule.setContent {
      RegisterFooter(
        resultCount = 50,
        currentPage = 3,
        pagesCount = 3,
        previousButtonClickListener = { listenerObjectSpy.onPreviousButtonClick() },
        nextButtonClickListener = { listenerObjectSpy.onNextButtonClick() }
      )
    }
    composeRule.onNodeWithTag(SEARCH_FOOTER_TAG).assertExists()
    composeRule.onNodeWithTag(SEARCH_FOOTER_TAG).assertIsDisplayed()

    // Previous button is displayed
    composeRule.onNodeWithTag(SEARCH_FOOTER_PREVIOUS_BUTTON_TAG).assertExists()

    // Clicking previous button should call 'onPreviousButtonClick' method of 'listenerObjectSpy'
    composeRule.onNodeWithTag(SEARCH_FOOTER_PREVIOUS_BUTTON_TAG).performClick()
    verify { listenerObjectSpy.onPreviousButtonClick() }

    // Pagination text is "Page 3 of 3"
    composeRule.onNodeWithTag(SEARCH_FOOTER_PAGINATION_TAG).assertExists()
    composeRule.onNodeWithTag(SEARCH_FOOTER_PAGINATION_TAG).assertIsDisplayed()
    composeRule.onNodeWithTag(SEARCH_FOOTER_PAGINATION_TAG).assertTextEquals("Page 3 of 3")

    // Next button is not displayed
    composeRule.onNodeWithTag(SEARCH_FOOTER_NEXT_BUTTON_TAG).assertDoesNotExist()
  }

  @Test
  fun testSearchFooterWithResultsFittingOnePage() {
    composeRule.setContent {
      RegisterFooter(
        resultCount = 20,
        currentPage = 1,
        pagesCount = 1,
        previousButtonClickListener = { listenerObjectSpy.onPreviousButtonClick() },
        nextButtonClickListener = { listenerObjectSpy.onNextButtonClick() }
      )
    }
    composeRule.onNodeWithTag(SEARCH_FOOTER_TAG).assertExists()
    composeRule.onNodeWithTag(SEARCH_FOOTER_TAG).assertIsDisplayed()

    // Previous button is not displayed
    composeRule.onNodeWithTag(SEARCH_FOOTER_PREVIOUS_BUTTON_TAG).assertDoesNotExist()

    // Pagination text is "Page 1 of 1"
    composeRule.onNodeWithTag(SEARCH_FOOTER_PAGINATION_TAG).assertExists()
    composeRule.onNodeWithTag(SEARCH_FOOTER_PAGINATION_TAG).assertIsDisplayed()
    composeRule.onNodeWithTag(SEARCH_FOOTER_PAGINATION_TAG).assertTextEquals("Page 1 of 1")

    // Next button is not displayed
    composeRule.onNodeWithTag(SEARCH_FOOTER_NEXT_BUTTON_TAG).assertDoesNotExist()
  }

  @Test
  fun testNoResultsComponent() {
    composeRule.setContent { NoResults() }
    composeRule.onNodeWithText("No results", useUnmergedTree = true).assertExists()
    composeRule.onNodeWithText("No results", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun testPaginatedRegisterShouldShowNoResultsView() {
    composeRule.setContent {
      PaginatedRegister(
        loadState = LoadState.NotLoading(false),
        showResultsCount = true,
        resultCount = 0,
        body = { RegisterBody() },
        currentPage = 1,
        pagesCount = 1,
        previousButtonClickListener = { listenerObjectSpy.onPreviousButtonClick() },
        nextButtonClickListener = { listenerObjectSpy.onNextButtonClick() },
        showHeader = true,
        showFooter = true
      )
    }

    // No results is displayed
    composeRule.onNodeWithText("No results").assertExists()
    composeRule.onNodeWithText("No results").assertIsDisplayed()

    // Register body not displayed
    composeRule.onNodeWithTag(registerBodyTag).assertDoesNotExist()

    // Pagination is not displayed
    composeRule.onNodeWithTag(SEARCH_FOOTER_PREVIOUS_BUTTON_TAG).assertDoesNotExist()
    composeRule.onNodeWithTag(SEARCH_FOOTER_PAGINATION_TAG).assertDoesNotExist()
    composeRule.onNodeWithTag(SEARCH_FOOTER_NEXT_BUTTON_TAG).assertDoesNotExist()
  }

  @Test
  fun testPaginatedRegisterShouldDisplayProgressDialog() {
    composeRule.setContent {
      PaginatedRegister(
        loadState = LoadState.Loading,
        showResultsCount = false,
        resultCount = 0,
        body = { RegisterBody() },
        currentPage = 1,
        pagesCount = 1,
        previousButtonClickListener = { listenerObjectSpy.onPreviousButtonClick() },
        nextButtonClickListener = { listenerObjectSpy.onNextButtonClick() },
        showHeader = true,
        showFooter = true
      )
    }

    // CircularProgressBar is displayed
    composeRule.onNodeWithTag(CIRCULAR_PROGRESS_BAR).assertExists()
    composeRule.onNodeWithTag(CIRCULAR_PROGRESS_BAR).assertIsDisplayed()

    // No results is not displayed
    composeRule.onNodeWithText("No results").assertDoesNotExist()

    // Register body not displayed
    composeRule.onNodeWithTag(registerBodyTag).assertDoesNotExist()

    // Pagination is not displayed
    composeRule.onNodeWithTag(SEARCH_FOOTER_PREVIOUS_BUTTON_TAG).assertDoesNotExist()
    composeRule.onNodeWithTag(SEARCH_FOOTER_PAGINATION_TAG).assertDoesNotExist()
    composeRule.onNodeWithTag(SEARCH_FOOTER_NEXT_BUTTON_TAG).assertDoesNotExist()
  }

  @Test
  fun testPaginatedRegisterShouldDisplayResultsBodyWithFooter() {
    composeRule.setContent {
      PaginatedRegister(
        loadState = LoadState.NotLoading(true),
        showResultsCount = false,
        resultCount = 52,
        body = { RegisterBody() },
        currentPage = 2,
        pagesCount = 3,
        previousButtonClickListener = { listenerObjectSpy.onPreviousButtonClick() },
        nextButtonClickListener = { listenerObjectSpy.onNextButtonClick() },
        showHeader = true,
        showFooter = true
      )
    }

    // CircularProgressBar is not displayed
    composeRule.onNodeWithTag(CIRCULAR_PROGRESS_BAR).assertDoesNotExist()

    // No results is not displayed
    composeRule.onNodeWithText("No results").assertDoesNotExist()

    // Register body is displayed
    composeRule.onNodeWithTag(registerBodyTag).assertExists()
    composeRule.onNodeWithTag(registerBodyTag).assertIsDisplayed()

    // Pagination is displayed with text Page 2 of 3
    composeRule.onNodeWithTag(SEARCH_FOOTER_PREVIOUS_BUTTON_TAG).assertExists()
    composeRule.onNodeWithTag(SEARCH_FOOTER_PREVIOUS_BUTTON_TAG).assertIsDisplayed()
    composeRule.onNodeWithTag(SEARCH_FOOTER_PAGINATION_TAG).assertExists()
    composeRule.onNodeWithTag(SEARCH_FOOTER_PAGINATION_TAG).assertTextEquals("Page 2 of 3")
    composeRule.onNodeWithTag(SEARCH_FOOTER_NEXT_BUTTON_TAG).assertExists()
    composeRule.onNodeWithTag(SEARCH_FOOTER_NEXT_BUTTON_TAG).assertIsDisplayed()
  }

  @Test
  fun testPaginatedRegisterShouldDisplayResultsBodyWithNoFooter() {
    composeRule.setContent {
      PaginatedRegister(
        loadState = LoadState.NotLoading(true),
        showResultsCount = true,
        resultCount = 52,
        body = { RegisterBody() },
        currentPage = 2,
        pagesCount = 3,
        previousButtonClickListener = { listenerObjectSpy.onPreviousButtonClick() },
        nextButtonClickListener = { listenerObjectSpy.onNextButtonClick() },
        showHeader = true,
        showFooter = false
      )
    }

    // CircularProgressBar is not displayed
    composeRule.onNodeWithTag(CIRCULAR_PROGRESS_BAR).assertDoesNotExist()

    // No results is not displayed
    composeRule.onNodeWithText("No results").assertDoesNotExist()

    // Register body is displayed
    composeRule.onNodeWithTag(registerBodyTag).assertExists()
    composeRule.onNodeWithTag(registerBodyTag).assertIsDisplayed()

    // Pagination is not displayed
    composeRule.onNodeWithTag(SEARCH_FOOTER_PREVIOUS_BUTTON_TAG).assertDoesNotExist()
    composeRule.onNodeWithTag(SEARCH_FOOTER_PAGINATION_TAG).assertDoesNotExist()
    composeRule.onNodeWithTag(SEARCH_FOOTER_NEXT_BUTTON_TAG).assertDoesNotExist()
  }

  @Test
  fun testPaginatedRegisterShouldDisplayResultsBodyWithFooterAbsolute() {
    composeRule.setContent {
      PaginatedRegister(
        loadState = LoadState.NotLoading(true),
        showResultsCount = false,
        resultCount = 52,
        body = { RegisterBody() },
        currentPage = 2,
        pagesCount = 3,
        previousButtonClickListener = { listenerObjectSpy.onPreviousButtonClick() },
        nextButtonClickListener = { listenerObjectSpy.onNextButtonClick() },
        showHeader = true,
        showFooter = true
      )
    }

    // CircularProgressBar is not displayed
    composeRule.onNodeWithTag(CIRCULAR_PROGRESS_BAR).assertDoesNotExist()

    // No results is not displayed
    composeRule.onNodeWithText("No results").assertDoesNotExist()

    // Register body is displayed
    composeRule.onNodeWithTag(registerBodyTag).assertExists()
    composeRule.onNodeWithTag(registerBodyTag).assertIsDisplayed()

    // Pagination is displayed
    composeRule.onNodeWithTag(SEARCH_FOOTER_PREVIOUS_BUTTON_TAG).assertExists()
    composeRule.onNodeWithTag(SEARCH_FOOTER_PAGINATION_TAG).assertExists()
    composeRule.onNodeWithTag(SEARCH_FOOTER_NEXT_BUTTON_TAG).assertExists()
  }

  @Test
  fun testPaginatedRegisterShouldDisplayResultsBodyWithNoFooterAbsolute() {
    composeRule.setContent {
      PaginatedRegister(
        loadState = LoadState.NotLoading(true),
        showResultsCount = false,
        resultCount = 52,
        body = { RegisterBody() },
        currentPage = 2,
        pagesCount = 3,
        previousButtonClickListener = { listenerObjectSpy.onPreviousButtonClick() },
        nextButtonClickListener = { listenerObjectSpy.onNextButtonClick() },
        showHeader = true,
        showFooter = false
      )
    }

    // CircularProgressBar is not displayed
    composeRule.onNodeWithTag(CIRCULAR_PROGRESS_BAR).assertDoesNotExist()

    // No results is not displayed
    composeRule.onNodeWithText("No results").assertDoesNotExist()

    // Register body is displayed
    composeRule.onNodeWithTag(registerBodyTag).assertExists()
    composeRule.onNodeWithTag(registerBodyTag).assertIsDisplayed()

    // Pagination is not displayed
    composeRule.onNodeWithTag(SEARCH_FOOTER_PREVIOUS_BUTTON_TAG).assertDoesNotExist()
    composeRule.onNodeWithTag(SEARCH_FOOTER_PAGINATION_TAG).assertDoesNotExist()
    composeRule.onNodeWithTag(SEARCH_FOOTER_NEXT_BUTTON_TAG).assertDoesNotExist()
  }

  @Composable
  private fun RegisterBody() {
    Text(text = "Nothing in particular", modifier = Modifier.testTag(registerBodyTag))
  }
}
