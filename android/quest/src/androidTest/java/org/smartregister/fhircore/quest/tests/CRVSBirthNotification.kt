package org.smartregister.fhircore.quest.tests

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.ui.login.LoginActivity

class CRVSBirthNotification {
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
    fun crvsBirthNotiForm(){
        Thread.sleep(5000)
        composeTestRule.onNodeWithText("234 234").performClick()
        composeTestRule.onNodeWithText("CRVS BIRTH NOTIFICATION").performClick()
        Thread.sleep(5000)
        composeTestRule.onNodeWithText("Other").performClick()
        composeTestRule.onNodeWithText("Number of children born").performTextInput("5")
        composeTestRule.onNodeWithText("Type of attendance at delivery").performTextInput("2")
        composeTestRule.onNodeWithText("Mode of delivery").performTextInput("Forceps")
        composeTestRule.onNodeWithText("Number of children born").performTextInput("5")
        composeTestRule.onNodeWithText("Baby birth weight").performTextInput("12")
        composeTestRule.onNodeWithText("Mother's Name").performTextInput("abc")
        composeTestRule.onNodeWithText("Mother's National ID number").performTextInput("4557664867")
        composeTestRule.onNodeWithText("Mother's Marital Status").performTextInput("Mariied")
        composeTestRule.onNodeWithText("Mother's Residence").performTextInput("ghiuse ggdki")
        composeTestRule.onNodeWithText("Mother's Birth Place").performTextInput("xyz")
        composeTestRule.onNodeWithText("Father's Name").performTextInput("abc")
        composeTestRule.onNodeWithText("Father's National ID number").performTextInput("4557664867")
        composeTestRule.onNodeWithText("Father's Marital Status").performTextInput("Married")
        composeTestRule.onNodeWithText("Father's Residence").performTextInput("ghiuse ggdki")
        composeTestRule.onNodeWithText("Father's Birth Place").performTextInput("xyz")
        composeTestRule.onNodeWithText("Mother's Name").performTextInput("abc")
        composeTestRule.onNodeWithText("Mother's National ID number").performTextInput("4557664867")
        composeTestRule.onNodeWithText("Mother's Marital Status").performTextInput("Married")
        composeTestRule.onNodeWithText("Mother's Residence").performTextInput("ghiuse ggdki")
        composeTestRule.onNodeWithText("Yes").performClick()
        composeTestRule.onNodeWithText("SAVE").performClick()
    }
}