package org.smartregister.fhircore.quest.tests

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.ui.login.LoginActivity

class CRVSClientRegistration {

    @get:Rule
    val composeTestRule = createAndroidComposeRule(LoginActivity::class.java)

    @Before
    fun successfulLogin() {
        Thread.sleep(5000)
        composeTestRule.onNodeWithText("Enter username").performTextInput("demo")
        composeTestRule.onNodeWithText("Enter password").performTextInput("Amani123")
        composeTestRule.onNodeWithText("LOGIN").performClick()
        Thread.sleep(10000)
    }

    @Test
    fun crvsClientRegistration(){
        Thread.sleep(5000)
        composeTestRule.onNodeWithText("234 234").performClick()
        composeTestRule.onNodeWithText("CRVS CLIENT REGISTRATION").performClick()
        Thread.sleep(5000)
        composeTestRule.onNodeWithText("First Name").performTextInput("Test")
        composeTestRule.onNodeWithText("Middle Name").performTextInput("Test")
        composeTestRule.onNodeWithText("Surname").performTextInput("Test")
        composeTestRule.onNodeWithText("Female").performClick()
        composeTestRule.onNodeWithText("SAVE").performClick()

    }
}