package org.smartregister.fhircore.quest.tests

import android.service.autofill.Validators.not
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.ui.login.LoginActivity
import org.smartregister.fhircore.quest.R
import java.util.regex.Pattern.matches

class SettingTest {
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
    fun languageChange(){
        Thread.sleep(90000)
        onView(withContentDescription("Settings")).perform(click())
        composeTestRule.onNodeWithText("Language").performClick()
        composeTestRule.onNodeWithText("Swahili").performClick()
        Thread.sleep(5000)
       // onView(withId(R.id.register_filter_textview)).check(matches(v))
    }
}