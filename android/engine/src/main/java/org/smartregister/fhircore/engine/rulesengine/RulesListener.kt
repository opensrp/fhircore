/*
 * Copyright 2021-2024 Ona Systems, Inc
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

import org.apache.commons.jexl3.JexlException
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rule
import org.jeasy.rules.api.RuleListener
import org.jeasy.rules.core.DefaultRulesEngine
import org.smartregister.fhircore.engine.BuildConfig
import timber.log.Timber

abstract class RulesListener : RuleListener {
  protected val rulesEngine: DefaultRulesEngine =
    DefaultRulesEngine().also { it.registerRuleListener(this) }

  override fun beforeEvaluate(rule: Rule, facts: Facts): Boolean = true

  override fun onSuccess(rule: Rule, facts: Facts) {
    if (BuildConfig.DEBUG) {
      val computedValuesMap = facts.get(DATA) as Map<String, Any>
      Timber.d("Rule executed: %s -> %s", rule, computedValuesMap[rule.name])
    }
  }

  override fun onFailure(rule: Rule, facts: Facts, exception: Exception) =
    if (exception is JexlException) {
      when (exception) {
        // Just display error message for undefined variable; expected for missing facts
        is JexlException.Variable ->
          log(
            exception,
            "${exception.localizedMessage}, consider checking for null before usage: e.g ${exception.variable} != null",
          )
        else -> log(exception, "Failed to execute rule : ${rule.name} ")
      }
    } else {
      log(exception)
    }

  override fun onEvaluationError(rule: Rule, facts: Facts, exception: java.lang.Exception) {
    log(exception, "Evaluation error")
  }

  override fun afterEvaluate(rule: Rule, facts: Facts, evaluationResult: Boolean) = Unit

  fun log(exception: java.lang.Exception, message: String? = null) = Timber.e(exception, message)

  fun Map<String, List<*>>.addToFacts(facts: Facts) = this.forEach { facts.put(it.key, it.value) }

  companion object {
    const val TRUE = "true"
    const val DATA = "data"
    const val FHIR_PATH = "fhirPath"
  }
}
