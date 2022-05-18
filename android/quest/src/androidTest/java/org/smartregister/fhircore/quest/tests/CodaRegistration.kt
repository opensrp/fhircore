package org.smartregister.fhircore.quest.tests

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.ui.login.LoginActivity

class CodaRegistration {
    @get:Rule
    val composeTestRule = createAndroidComposeRule(LoginActivity::class.java)

    @Before
    fun successfulLogin() {
        composeTestRule.onNodeWithText("Enter username").performTextInput("ecbis")
        composeTestRule.onNodeWithText("Enter password").performTextInput("Amani123")
        composeTestRule.onNodeWithText("LOGIN").performClick()
        Thread.sleep(10000)
    }

    @Test
    fun registerCodaTest(){
        Thread.sleep(5000)
        composeTestRule.onNodeWithText("").performClick()
        composeTestRule.onNodeWithText("CODA ReGISTRATION").performClick()
        composeTestRule.onNodeWithText("First Name").performTextInput("Another")
        composeTestRule.onNodeWithText("Last Name").performTextInput("Test")
        composeTestRule.onNodeWithText("Father's Name").performTextInput("Only")
        composeTestRule.onNodeWithText("Date of Birth")
            .performTextInput("03/12/2021")
        composeTestRule.onNodeWithText("Age (in months)").performTextInput("6")
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.onNodeWithText("SAVE").performClick()
        Thread.sleep(10000)
    }
    @Test
    fun registerCodaWithMissingFields() {
        composeTestRule.onNodeWithText("").performClick()
        composeTestRule.onNodeWithText("CODA ReGISTRATION").performClick()
        composeTestRule.onNodeWithText("Last Name").performTextInput("Test")
        composeTestRule.onNodeWithText("Father's Name").performTextInput("Only")
        composeTestRule.onNodeWithText("Date of Birth")
            .performTextInput("03/12/2021")
        composeTestRule.onNodeWithText("Age (in months)").performTextInput("6")
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.onNodeWithText("Validation Failed").assertExists()
    }

    @After
    fun logout() {

    }
}