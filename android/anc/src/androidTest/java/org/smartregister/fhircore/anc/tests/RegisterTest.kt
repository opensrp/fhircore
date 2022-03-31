package org.smartregister.fhircore.anc.tests

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.engine.ui.login.LoginActivity

class RegisterTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule(LoginActivity::class.java)

    @Test
    fun successfulLogin() {
        composeTestRule.onNodeWithText("Enter username").performTextInput("demo")
        composeTestRule.onNodeWithText("Enter password").performTextInput("Amani123")
        composeTestRule.onNodeWithText("LOGIN").performClick()
    }

    @Test
    fun registerFamily()
    {
        onView(withId(R.id.text_input_edit_text)).perform(typeText("Test"))

    }
}