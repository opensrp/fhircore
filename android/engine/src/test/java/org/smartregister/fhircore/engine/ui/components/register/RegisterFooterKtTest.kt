package org.smartregister.fhircore.engine.ui.components.register

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class RegisterFooterKtTest : RobolectricTest() {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun testRegisterFooterView() {
    composeRule.setContent {
      RegisterFooter(
        resultCount = 10,
        currentPage = 1,
        pagesCount = 5,
        previousButtonClickListener = {},
        nextButtonClickListener = {}
      )
    }

    
  }
}
