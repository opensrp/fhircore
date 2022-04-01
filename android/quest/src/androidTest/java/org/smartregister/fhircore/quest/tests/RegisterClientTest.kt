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

class RegisterClientTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule(LoginActivity::class.java)

    @Before
    fun successfulLogin() {
        composeTestRule.onNodeWithText("Enter username").performTextInput("demo")
        composeTestRule.onNodeWithText("Enter password").performTextInput("Amani123")
        composeTestRule.onNodeWithText("LOGIN").performClick()
    }

    @Test
    fun registerClientTest(){
        composeTestRule.onNodeWithText("REGISTER NEW CLIENT").performClick()
        composeTestRule.onNodeWithText("Surname").performTextInput("Test")
        composeTestRule.onNodeWithText("First Name").performTextInput("Another")
        composeTestRule.onNodeWithText("National ID Number")
            .performTextInput("89764532")
        composeTestRule.onNodeWithText("Male").performClick()
        composeTestRule.onNodeWithText("Age").performClick()
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.onNodeWithText("SAVE").performClick()
    }
    @Test
    fun registerClientWithMissingFields() {
        composeTestRule.onNodeWithText("REGISTER NEW CLIENT").performClick()
        composeTestRule.onNodeWithText("First Name").performTextInput("Another")
        composeTestRule.onNodeWithText("National ID Number")
            .performTextInput("89764532")
        composeTestRule.onNodeWithText("Male").performClick()
        composeTestRule.onNodeWithText("Age").performClick()
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.onNodeWithText("Validation Failed").assertExists()
    }

    @After
    fun logout() {

    }
}