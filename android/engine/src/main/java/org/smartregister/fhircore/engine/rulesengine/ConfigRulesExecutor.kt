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

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis
import org.apache.commons.jexl3.JexlEngine
import org.hl7.fhir.r4.model.Resource
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rules
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.rulesengine.services.DateService
import org.smartregister.fhircore.engine.util.extension.generateRules
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import timber.log.Timber

/**
 * This file executes rules to be used by configs.
 *
 * NOTE: that the [Facts] object is not thread safe, each thread should have its own set of data to
 * work on. When used in a multi-threaded environment it may exhibit unexpected behavior and return
 * incorrect results when rules are fired. Use the [ConfigRulesExecutor] in the same coroutine
 * context of the caller.
 */
@Singleton
class ConfigRulesExecutor
@Inject
constructor(
  val fhirPathDataExtractor: FhirPathDataExtractor,
  val jexlEngine: JexlEngine,
) : RulesListener() {

  /** Compute configuration level [Rules] */
  fun computeConfigRules(rules: Rules, baseResource: Resource?): Map<String, Any> =
    fireRules(
      rules = rules,
      baseResource = baseResource,
    )

  private fun fireRules(rules: Rules, baseResource: Resource? = null): Map<String, Any> {
    val facts =
      Facts().apply {
        put(FHIR_PATH, fhirPathDataExtractor)
        put(DATA, mutableMapOf<String, Any>())
        put(DATE_SERVICE, DateService)
        if (baseResource != null) {
          put(baseResource.resourceType.name, baseResource)
        }
      }
    if (BuildConfig.DEBUG) {
      val timeToFireRules = measureTimeMillis { rulesEngine.fire(rules, facts) }
      Timber.d("Rule executed in $timeToFireRules millisecond(s)")
    } else {
      rulesEngine.fire(rules, facts)
    }
    return facts.get(DATA) as Map<String, Any>
  }

  fun generateRules(ruleConfigs: List<RuleConfig>): Rules = ruleConfigs.generateRules(jexlEngine)

  companion object {
    private const val DATE_SERVICE = "dateService"
  }
}
