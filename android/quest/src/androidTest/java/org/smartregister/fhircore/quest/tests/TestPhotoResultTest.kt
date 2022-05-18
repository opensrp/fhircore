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

class TestPhotoResultTest {
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
    fun enterG6PDNumber(){
        Thread.sleep(5000)
        composeTestRule.onNodeWithText("REGISTER NEW CLIENT").performClick()
        composeTestRule.onNodeWithText("G6PD Test Photo Result").performClick()
        composeTestRule.onNodeWithText("Number").performClick()
        composeTestRule.onNodeWithText("G6PD").performTextInput("24")
        composeTestRule.onNodeWithText("Hemoglobin (Hb)").performTextInput("24")
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.onNodeWithText("SAVE").performClick()
        Thread.sleep(10000)
    }
    @Test
    fun enterG6PDError() {
        Thread.sleep(5000)
        composeTestRule.onNodeWithText("REGISTER NEW CLIENT").performClick()
        composeTestRule.onNodeWithText("G6PD Test Photo Result").performClick()
        composeTestRule.onNodeWithText("Error").performClick()
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.onNodeWithText("SAVE").performClick()
        Thread.sleep(10000)
    }

    @Test
    fun enterG6PDNA() {
        Thread.sleep(5000)
        composeTestRule.onNodeWithText("REGISTER NEW CLIENT").performClick()
        composeTestRule.onNodeWithText("G6PD Test Photo Result").performClick()
        composeTestRule.onNodeWithText("N/A").performClick()
        composeTestRule.onNodeWithText("G6PD").performTextInput("24")
        composeTestRule.onNodeWithText("Hemoglobin (Hb)").performTextInput("24")
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.onNodeWithText("SAVE").performClick()
        Thread.sleep(10000)
    }

    @After
    fun logout() {

    }
}