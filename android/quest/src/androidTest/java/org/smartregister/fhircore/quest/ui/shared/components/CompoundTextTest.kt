package org.smartregister.fhircore.quest.ui.shared.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.TextFontWeight
import org.smartregister.fhircore.engine.domain.model.ResourceData

class CompoundTextTest {

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
              resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
              navController = navController
      )
    }
    composeRule.onNodeWithText("Full Name, Age").assertExists().assertIsDisplayed()
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
              resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
              navController = navController
      )
    }
    composeRule.onNodeWithText("Sex").assertExists().assertIsDisplayed()
  }

  @Test
  fun testFullNameCompoundTextWithSecondaryTextIsRenderedCorrectly() {
    composeRule.setContent {
      GenerateView(
              properties =
              CompoundTextProperties(primaryText = "Full Name, Sex, Age", primaryTextColor = "#000000"),
              resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
              navController = navController
      )
    }
    composeRule.onNodeWithText("Full Name, Sex, Age").assertExists().assertIsDisplayed()
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
              resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
              navController = navController
      )
    }
    composeRule.onNodeWithText("Last visited").assertExists().assertIsDisplayed()
    composeRule.onNodeWithText("Yesterday").assertExists().assertIsDisplayed()
  }
}