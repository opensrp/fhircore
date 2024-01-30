package org.smartregister.fhircore.quest.cucumber

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import io.cucumber.junit.WithJunitRule
import org.junit.Rule
import javax.inject.Singleton

@WithJunitRule
@Singleton
class ComposeRuleHolder {
  @get:Rule(order = 1)
  val composeRule = createEmptyComposeRule()
}
