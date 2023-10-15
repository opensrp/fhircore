package org.smartregister.fhircore.quest.cucumber.objects

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.closeSoftKeyboard
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.ui.appsetting.AppSettingActivity
import org.smartregister.fhircore.quest.ui.login.LoginActivity

class Login {
    @get:Rule val composeTestAppScreen = createAndroidComposeRule(AppSettingActivity::class.java)
    @get:Rule val composeTestRule = createAndroidComposeRule(LoginActivity::class.java)

    @Test
    fun enterApplicationIdField(applicationID: String) {
        composeTestAppScreen.onNodeWithText("Enter Application ID").performTextInput(applicationID)
    }
    fun selectUsernameField() {
        composeTestAppScreen.onNodeWithText("Enter username")
    }
    fun selectPasswordField() {
        composeTestAppScreen.onNodeWithText("Enter password")
    }
    fun enterUsername(username: String) {
        composeTestRule.onNodeWithText("Enter username").performTextInput(username)
    }
    fun enterPassword(password: String) {
        composeTestRule.onNodeWithText("Enter password").performTextInput(password)
    }
    fun clickLoginButton() {
        composeTestAppScreen.onNodeWithText("LOGIN").performClick()
    }
    fun closeKeyboard() {
        closeSoftKeyboard()
        Thread.sleep(1000)
    }
    fun clickLoadConfigurationsButton() {
        composeTestAppScreen.onNodeWithText("LOAD CONFIGURATIONS").performClick()
    }
    fun isSuccessfulLogin() {
        composeTestRule.onNodeWithText("Enter username").performTextInput("demo")
        composeTestRule.onNodeWithText("Enter password").performTextInput("Amani123")
        composeTestRule.onNodeWithText("LOGIN").performClick()
    }

}