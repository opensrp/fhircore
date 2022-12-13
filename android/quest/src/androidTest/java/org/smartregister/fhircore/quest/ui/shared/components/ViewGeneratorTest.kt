package org.smartregister.fhircore.quest.ui.shared.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavController
import io.mockk.mockk
import org.hl7.fhir.r4.model.Patient
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.TextFontWeight
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData

class ViewGeneratorTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val navController = mockk<NavController>(relaxed = true, relaxUnitFun = true)
    private val viewProperties = ViewProperties


    @Test
    fun testGenerateViewRendersActionableButtonWhenViewTypeIsButton() {
        composeRule.setContent {
            GenerateView(
                properties = ButtonProperties(
                    status = "DUE",
                    text = "Button Text",
                    actions =
                    listOf(
                        ActionConfig(
                            trigger = ActionTrigger.ON_CLICK,
                            workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
                            questionnaire = QuestionnaireConfig(id = "23", title = "Add Family"),
                        )
                    )
                ),
                resourceData = ResourceData(Patient()),
                navController = navController
            )
        }
        composeRule
            .onNodeWithText("Button Text", useUnmergedTree = true)
            .assertExists()
            .assertIsDisplayed()
            .performClick()
    }

    @Test
    fun testCompoundTextWithSecondaryTextIsRenderedCorrectly() {
        composeRule.setContent {
            GenerateView(
                properties =
                CompoundTextProperties(
                    primaryText = "Full Name, Sex, Age",
                    primaryTextColor = "#000000"),
                resourceData = ResourceData(Patient()),
                navController = navController
            )
        }
        composeRule
            .onNodeWithText("Full Name, Sex, Age")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun testLastVisitedCompoundTextWithSecondaryTextIsRenderedCorrectly() {
        composeRule.setContent {
            GenerateView(
                properties =
                CompoundTextProperties(
                    primaryText = "Last visited",
                    primaryTextColor = "#5A5A5A",
                    secondaryText = "Yesterday",
                    secondaryTextColor = "#FFFFFF",
                    separator = ".",
                    secondaryTextBackgroundColor = "#FFA500"
                ),
                resourceData = ResourceData(Patient()),
                navController = navController
            )
        }
        composeRule
            .onNodeWithText("Last visited")
            .assertExists()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Yesterday")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun testFullNameCompoundTextNoSecondaryTextIsRenderedCorrectly() {
        composeRule.setContent {
            GenerateView(
                properties =
                CompoundTextProperties(
                    primaryText = "Full Name, Age",
                    primaryTextColor = "#000000",
                    primaryTextFontWeight = TextFontWeight.SEMI_BOLD
                ),
                resourceData = ResourceData(Patient()),
                navController = navController
            )
        }
        composeRule
            .onNodeWithText("Full Name, Age")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun testSexCompoundTextNoSecondaryTextIsRenderedCorrectly() {
        composeRule.setContent {
            GenerateView(
                properties =
                CompoundTextProperties(
                    primaryText = "Sex",
                    primaryTextColor = "#5A5A5A",
                ),
                resourceData = ResourceData(Patient()),
                navController = navController
            )
        }
        composeRule
            .onNodeWithText("Sex")
            .assertExists()
            .assertIsDisplayed()
    }


    @Test
    fun testGenerateViewRendersServiceCardCorrectly() {
        composeRule.setContent {
            GenerateView(
                properties =
                CompoundTextProperties(
                    primaryText = "Sex",
                    primaryTextColor = "#5A5A5A",
                ),
                resourceData = ResourceData(Patient()),
                navController = navController
            )
        }
        composeRule
            .onNodeWithText("Sex")
            .assertExists()
            .assertIsDisplayed()
    }

}