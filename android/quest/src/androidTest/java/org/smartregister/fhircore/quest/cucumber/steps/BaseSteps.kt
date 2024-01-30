package org.smartregister.fhircore.quest.cucumber.steps

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import org.smartregister.fhircore.quest.cucumber.ComposeRuleHolder
import javax.inject.Inject

abstract class BaseSteps : SemanticsNodeInteractionsProvider {

  @Inject
  lateinit var composeRuleHolder: ComposeRuleHolder

  override fun onAllNodes(
    matcher: SemanticsMatcher,
    useUnmergedTree: Boolean
  ): SemanticsNodeInteractionCollection {
    return composeRuleHolder.composeRule.onAllNodes(matcher, useUnmergedTree)
  }

  override fun onNode(
    matcher: SemanticsMatcher,
    useUnmergedTree: Boolean
  ): SemanticsNodeInteraction {
    return composeRuleHolder.composeRule.onNode(matcher, useUnmergedTree)
  }
}