/*
 * Copyright 2021 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.engine.rulesengine

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import javax.inject.Inject
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rule
import org.jeasy.rules.api.RuleListener
import org.jeasy.rules.api.Rules
import org.jeasy.rules.core.DefaultRulesEngine
import org.jeasy.rules.core.RuleBuilder
import org.mvel2.CompileException
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.navigation.NavigationActionRuleConfig
import timber.log.Timber

class RulesFactory @Inject constructor(val configurationRegistry: ConfigurationRegistry) :
  RuleListener {

  private var facts: Facts = Facts()
  var allRules: Rules? = null
  private var rulesEngine: DefaultRulesEngine = DefaultRulesEngine()
  private var executableRulesList: HashSet<Rule> = hashSetOf()
  private val fhirContext: FhirContext = FhirContext.forCached(FhirVersionEnum.R4)
  private val fhirPathEngine: FHIRPathEngine =
    FHIRPathEngine(HapiWorkerContext(fhirContext, fhirContext.validationSupport))

  init {
    rulesEngine.registerRuleListener(this)
    loadFacts()
  }

  override fun beforeEvaluate(rule: Rule?, facts: Facts?): Boolean = true

  override fun onSuccess(rule: Rule?, facts: Facts?) = Timber.d("%s executed successfully", rule)

  override fun onFailure(rule: Rule?, facts: Facts?, exception: Exception?) =
    when (exception) {
      is CompileException -> Timber.e(exception.cause)
      else -> Timber.e(exception)
    }

  override fun beforeExecute(rule: Rule?, facts: Facts?) = Unit

  override fun afterEvaluate(rule: Rule?, facts: Facts?, evaluationResult: Boolean) = Unit

  fun updateFactsAndExecuteRules() {
    fireRules()
  }

  fun readRulesFromFile() {
    // TODO Implement this
  }

  fun loadFacts() {
    configurationRegistry.configsJsonMap.forEach { entry -> facts.put(entry.key, entry.value) }

    facts.put("fhirPathEngine", fhirPathEngine)
  }

  fun fireRule(navActionRuleConfig: NavigationActionRuleConfig) {
    val rule =
      RuleBuilder()
        .name(navActionRuleConfig.name)
        .description(navActionRuleConfig.condition)
        .then { navActionRuleConfig.action }
        .build()

    rulesEngine.fire(Rules(rule), facts)
  }

  private fun fireRules() {
    rulesEngine.fire(Rules(executableRulesList), facts)
  }
}
