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

package org.smartregister.fhircore.engine.util.extension

import java.net.URL
import java.util.Locale
import org.apache.commons.jexl3.JexlEngine
import org.apache.commons.jexl3.JexlException
import org.jeasy.rules.api.Rules
import org.jeasy.rules.jexl.JexlRule
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.rulesengine.RulesListener
import timber.log.Timber

fun ConfigurationRegistry.fetchLanguages() =
  this.retrieveConfiguration<ApplicationConfiguration>(ConfigType.Application)
    .run { this.languages }
    .map { Language(it, Locale.forLanguageTag(it).displayName) }

fun URL.getSubDomain() = this.host.substringBeforeLast('.').substringBeforeLast('.')

@Synchronized
fun List<RuleConfig>.generateRules(jexlEngine: JexlEngine): Rules =
  Rules(
    this.asSequence()
      .map { ruleConfig ->
        val customRule: JexlRule =
          JexlRule(jexlEngine)
            .name(ruleConfig.name)
            .description(ruleConfig.description)
            .priority(ruleConfig.priority)
            .`when`(ruleConfig.condition.ifEmpty { RulesListener.TRUE })

        for (action in ruleConfig.actions) {
          try {
            customRule.then(action)
          } catch (jexlException: JexlException) {
            Timber.e(jexlException)
            continue // Skip action when an error occurs to avoid app force close
          }
        }
        customRule
      }
      .toSet(),
  )
