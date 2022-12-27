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

package org.smartregister.fhircore.engine.configuration.register

import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.domain.model.RuleConfig

class RegisterContentConfigTest {
  private val rulesList = listOf(RuleConfig(name = "test rule", actions = listOf("actions")))

  private val registerContentConfig =
    RegisterContentConfig("separator", "display", rulesList, visible = false, computedRules = null)

  @Test
  fun testRegisterContentConfig() {
    Assert.assertEquals("separator", registerContentConfig.separator)
    Assert.assertEquals("display", registerContentConfig.display)
    Assert.assertEquals(rulesList, registerContentConfig.rules)
    Assert.assertEquals(false, registerContentConfig.visible)
    Assert.assertEquals(null, registerContentConfig.computedRules)
  }
}
