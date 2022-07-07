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

import com.google.android.fhir.datacapture.enablement.fhirPathEngine
import javax.inject.Inject
import javax.inject.Singleton
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rule
import org.jeasy.rules.api.RuleListener
import org.jeasy.rules.api.Rules
import org.jeasy.rules.core.DefaultRulesEngine
import org.jeasy.rules.core.RuleBuilder
import org.mvel2.CompileException
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import timber.log.Timber

@Singleton
class RulesFactory @Inject constructor(val configurationRegistry: ConfigurationRegistry) :
  RuleListener {

  private var facts: Facts = Facts()
  private var rulesEngine: DefaultRulesEngine = DefaultRulesEngine()
  private val fhirPathDataExtractor = FhirPathDataExtractor

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

  fun loadFacts() {
    configurationRegistry.configsJsonMap.forEach { entry -> facts.put(entry.key, entry.value) }

    facts.put("fhirPathEngine", fhirPathEngine)
  }

  fun fireRule(ruleConfig: RuleConfig) {
    val rule =
      RuleBuilder()
        .name(ruleConfig.name)
        .description(ruleConfig.condition)
        // .then { ruleConfig.action } // TODO check on this
        .build()

    rulesEngine.fire(Rules(rule), facts)
  }
}
