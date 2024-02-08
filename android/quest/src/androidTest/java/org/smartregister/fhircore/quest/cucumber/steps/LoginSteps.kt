package org.smartregister.fhircore.quest.cucumber.steps

import android.content.Intent
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidTest
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.smartregister.fhircore.quest.cucumber.ActivityScenarioHolder
import org.smartregister.fhircore.quest.cucumber.ComposeRuleHolder
import org.smartregister.fhircore.quest.ui.appsetting.APP_ID_TEXT_INPUT_TAG
import org.smartregister.fhircore.quest.ui.appsetting.AppSettingActivity

@HiltAndroidTest
class LoginSteps(
  val activityScenarioHolder: ActivityScenarioHolder,
  val composeRuleHolder: ComposeRuleHolder
) : SemanticsNodeInteractionsProvider by composeRuleHolder.composeRule {

  @Given("I initialize App")
  fun initializeApp() {
    val intent =
      Intent(
        InstrumentationRegistry.getInstrumentation().targetContext,
        AppSettingActivity::class.java
      )
    activityScenarioHolder.launch(intent)
  }

  @When("^I enter application ID")
  fun iEnterApplicationId() {
    val text = "notice-f"
    onNodeWithTag(APP_ID_TEXT_INPUT_TAG).performTextInput(text)
    onNodeWithTag(APP_ID_TEXT_INPUT_TAG).assert(hasText(text))

    //editTest.performTextInput("Text")
  }

  @Then("^I click on load configuration button")
  fun clickOnLoadConfigurationButton() {
  }
}
