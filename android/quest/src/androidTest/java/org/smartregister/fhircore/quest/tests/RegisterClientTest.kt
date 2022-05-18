package org.smartregister.fhircore.quest.tests

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.*
import org.smartregister.fhircore.engine.ui.login.LoginActivity

class RegisterClientTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule(LoginActivity::class.java)

    @Before
    fun successfulLogin() {
        Thread.sleep(5000)
        composeTestRule.onNodeWithText("Enter username").performTextInput("ecbis")
        composeTestRule.onNodeWithText("Enter password").performTextInput("Amani123")
        composeTestRule.onNodeWithText("LOGIN").performClick()
        Thread.sleep(10000)
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
        composeTestRule.onNodeWithText("Age (years) *").performTextInput("30")
        composeTestRule.onNodeWithText("Phone Number")
            .performTextInput("0723456789")
        composeTestRule.onNodeWithText("City").performTextInput("Bulawayo")
        composeTestRule.onNodeWithText("Country").performTextInput("Congo")
        composeTestRule.onNodeWithText("Yes").performClick()
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