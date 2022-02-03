package org.smartregister.fhircore.anc.cucumber.espresso

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.ui.login.LoginActivity

class LaunchActivityTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule(LoginActivity::class.java)
    
    @Test
    fun successfulLogin() {
        composeTestRule.onNodeWithText("Enter username").performTextInput("demo")
        composeTestRule.onNodeWithText("Enter password").performTextInput("Amani123")
        composeTestRule.onNodeWithText("LOGIN").performClick()
    }
}