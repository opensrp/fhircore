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

package org.smartregister.fhircore.engine.configuration.navigation

import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.domain.model.ActionConfig

class NavigationMenuConfigTest {

  private val actionConfigList = listOf(ActionConfig(trigger = ActionTrigger.ON_CLICK))
  private val menuIconConfig = MenuIconConfig(type = "icon")

  @Test
  fun testAuthConfiguration() {
    val navigationMenuConfig =
      NavigationMenuConfig(
        id = "1234",
        visible = false,
        enabled = "true",
        menuIconConfig = menuIconConfig,
        display = "sample display",
        showCount = false,
        actions = actionConfigList,
      )

    Assert.assertEquals("1234", navigationMenuConfig.id)
    Assert.assertEquals(false, navigationMenuConfig.visible)
    Assert.assertEquals("true", navigationMenuConfig.enabled)
    Assert.assertEquals(menuIconConfig, navigationMenuConfig.menuIconConfig)
    Assert.assertEquals("sample display", navigationMenuConfig.display)
    Assert.assertEquals(false, navigationMenuConfig.showCount)
    Assert.assertEquals(actionConfigList, navigationMenuConfig.actions)
  }
}
