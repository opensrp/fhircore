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
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class RegisterFooterKtTest : RobolectricTest() {

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
                    ApplicationProvider.getApplicationContext<Application>().getString(R.string.str_page_info, 2, 5)
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
