package org.smartregister.fhircore.quest.cucumber.steps

import android.content.Intent
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Before
import org.junit.Rule
import org.smartregister.fhircore.quest.cucumber.ActivityScenarioHolder
import org.smartregister.fhircore.quest.cucumber.ComposeRuleHolder
import org.smartregister.fhircore.quest.ui.appsetting.APP_ID_TEXT_INPUT_TAG
import org.smartregister.fhircore.quest.ui.appsetting.AppSettingActivity


class LoginSteps(
  val activityScenarioHolder: ActivityScenarioHolder,
  val composeRuleHolder: ComposeRuleHolder
) : SemanticsNodeInteractionsProvider by composeRuleHolder.composeRule {

  @get:Rule
  val hiltRule = HiltAndroidRule(this)

  @Before
  fun setUp() {
    hiltRule.inject()
  }

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
    composeRuleHolder.composeRule.setContent {
      val text = onNode(hasTestTag(APP_ID_TEXT_INPUT_TAG)).assertExists()
      text.performTextInput("notice-f")
    }
  }

  @Then("^I click on load configuration button")
  fun clickOnLoadConfigurationButton() {
  }
}
