package org.smartregister.fhircore.quest.cucumber

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule


class ComposeRuleHolder{

  @get:Rule(order = 1)
  val composeRule = createComposeRule()
}

/*


@Singleton
class CustomComposableRuleHolder @Inject constructor() {
  @get:Rule(order = 2)
  val composeRule = createComposeRule()
}

 */
