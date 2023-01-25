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

package org.smartregister.fhircore.engine.ui.components.register

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.R

class RegisterFooterKtTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun testRegisterFooterView() {
    composeRule.setContent {
      RegisterFooter(
        resultCount = 10,
        currentPage = 2,
        pagesCount = 5,
        previousButtonClickListener = {},
        nextButtonClickListener = {}
      )
    }

    composeRule.onNodeWithTag(SEARCH_FOOTER_TAG).assertExists()
    composeRule.onNodeWithTag(SEARCH_FOOTER_TAG).assertIsDisplayed()

    // Since {currentPage} > 1
    composeRule.onNodeWithTag(SEARCH_FOOTER_PREVIOUS_BUTTON_TAG).assertExists()
    composeRule.onNodeWithTag(SEARCH_FOOTER_PREVIOUS_BUTTON_TAG).assertIsDisplayed()

    composeRule
      .onNodeWithTag(SEARCH_FOOTER_PREVIOUS_BUTTON_TAG)
      .assertTextEquals(
        ApplicationProvider.getApplicationContext<Application>().getString(R.string.str_previous)
      )

    composeRule.onNodeWithTag(SEARCH_FOOTER_PAGINATION_TAG).assertExists()
    composeRule.onNodeWithTag(SEARCH_FOOTER_PAGINATION_TAG).assertIsDisplayed()

    composeRule
      .onNodeWithTag(SEARCH_FOOTER_PAGINATION_TAG)
      .assertTextEquals(
        ApplicationProvider.getApplicationContext<Application>()
          .getString(R.string.str_page_info, 2, 5)
      )

    composeRule.onNodeWithTag(SEARCH_FOOTER_NEXT_BUTTON_TAG).assertExists()
    composeRule.onNodeWithTag(SEARCH_FOOTER_NEXT_BUTTON_TAG).assertIsDisplayed()

    composeRule
      .onNodeWithTag(SEARCH_FOOTER_NEXT_BUTTON_TAG)
      .assertTextEquals(
        ApplicationProvider.getApplicationContext<Application>().getString(R.string.str_next)
      )
  }
}
