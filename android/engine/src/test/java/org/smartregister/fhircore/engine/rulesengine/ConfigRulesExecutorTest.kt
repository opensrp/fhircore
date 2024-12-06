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

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import javax.inject.Inject
import org.apache.commons.jexl3.JexlEngine
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rules
import org.jeasy.rules.core.DefaultRulesEngine
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor

@HiltAndroidTest
class ConfigRulesExecutorTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var fhirPathDataExtractor: FhirPathDataExtractor

  @Inject lateinit var jexlEngine: JexlEngine

  private lateinit var configRulesExecutor: ConfigRulesExecutor
  private val rulesEngine = mockk<DefaultRulesEngine>()

  @Before
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun setUp() {
    hiltRule.inject()
    configRulesExecutor = spyk(ConfigRulesExecutor(fhirPathDataExtractor, jexlEngine))
  }

  @Test
  fun testFireRulesReturnCorrectMapValue() {
    val ruleConfig =
      RuleConfig(
        name = "underFive",
        description = "Children Under 5 years",
        actions =
          listOf("data.put('underFive', dateService.addOrSubtractYearFromCurrentDate(5,'-'))"),
      )
    val ruleConfigs = listOf(ruleConfig)

    ReflectionHelpers.setField(configRulesExecutor, "rulesEngine", rulesEngine)
    every { rulesEngine.fire(any(), any()) } just runs
    val rules = configRulesExecutor.generateRules(ruleConfigs)
    configRulesExecutor.computeConfigRules(rules, null)
    val factsSlot = slot<Facts>()
    val rulesSlot = slot<Rules>()
    verify { rulesEngine.fire(capture(rulesSlot), capture(factsSlot)) }
    val capturedRule = rulesSlot.captured.first()
    Assert.assertEquals(ruleConfig.name, capturedRule.name)
    Assert.assertEquals(ruleConfig.description, capturedRule.description)
  }
}
