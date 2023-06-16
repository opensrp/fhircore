package org.smartregister.lint

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 14-06-2023. */
import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId

internal val CUSTOM_RULE_SET_ID = "opensrp-lint"

class CustomRuleSetProvider : RuleSetProviderV3(RuleSetId(CUSTOM_RULE_SET_ID)) {
  override fun getRuleProviders(): Set<RuleProvider> {
    return setOf(
      RuleProvider { FhirEngineCreateUpdateMethodCallRule() },
    )
  }
}
