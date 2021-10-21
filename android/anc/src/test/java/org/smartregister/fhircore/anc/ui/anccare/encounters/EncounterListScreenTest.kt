package org.smartregister.fhircore.anc.ui.anccare.encounters

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.hl7.fhir.r4.model.Encounter
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import java.text.SimpleDateFormat

class EncounterListScreenTest: RobolectricTest() {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun testEncounterListScreen() {
        composeRule.setContent { EncounterListScreen(dummyData()) }

        // verify top bar is displayed
        composeRule.onNodeWithText("Past encounters").assertExists()
        composeRule.onNodeWithText("Past encounters").assertIsDisplayed()

        // verify back arrow is displayed
        composeRule.onNodeWithContentDescription("Back arrow").assertExists()
        composeRule.onNodeWithContentDescription("Back arrow").assertIsDisplayed()

    }

    @Test
    fun testEncounterItem() {
        val encounterDate = SimpleDateFormat("yyyy-MM-dd").parse("2020-05-22")
        val encounterItem = org.smartregister.fhircore.anc.data.model.EncounterItem("1", Encounter.EncounterStatus.FINISHED, "Dummy", encounterDate)
        composeRule.setContent { EncounterItem(encounterItem,false) }

        // verify encounter date is displayed
        composeRule.onNodeWithText("May 22, 2020").assertExists()
        composeRule.onNodeWithText("May 22, 2020").assertIsDisplayed()

        // verify status is displayed
        composeRule.onNodeWithText(Encounter.EncounterStatus.FINISHED.name).assertExists()
        composeRule.onNodeWithText(Encounter.EncounterStatus.FINISHED.name).assertIsDisplayed()

    }

    @Test
    fun testLoadingItem() {
        composeRule.setContent { LoadingItem() }

        // verify loading item exists
        composeRule.onNodeWithTag("ProgressBarItem").assertExists()
    }
}