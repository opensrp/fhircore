package org.smartregister.fhircore.engine.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class SeparatorTest: RobolectricTest() {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun testSeparatorWithDefaultValuesIsDisplayed() {
        composeRule.setContent { Separator() }
        composeRule.onNodeWithText("-").assertExists()
    }

    @Test
    fun testSeparatorWithCustomValuesIsDisplayed() {
        val separator = "|"
        composeRule.setContent { Separator(separator = separator) }
        composeRule.onNodeWithText(separator).assertExists()
    }
}