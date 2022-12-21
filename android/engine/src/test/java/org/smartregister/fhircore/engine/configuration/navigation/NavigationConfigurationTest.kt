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
import org.smartregister.fhircore.engine.configuration.ConfigType

class NavigationConfigurationTest {

  @Test
  fun testAuthConfiguration() {
    val navigationConfiguration =
      NavigationConfiguration(
        appId = "app-demo",
        configType = ConfigType.Navigation.name,
        menuActionButton = NavigationMenuConfig(id = "test", display = "sample display"),
        staticMenu = listOf(NavigationMenuConfig(id = "test-2", display = "sample display 2")),
        clientRegisters = listOf(NavigationMenuConfig(id = "test-3", display = "sample display 3")),
        bottomSheetRegisters = NavigationBottomSheetRegisterConfig(display = "sample display 4"),
      )

    Assert.assertEquals("app-demo", navigationConfiguration.appId)
    Assert.assertEquals("navigation", navigationConfiguration.configType)
    Assert.assertEquals("test", navigationConfiguration.menuActionButton?.id)
    Assert.assertEquals("test-2", navigationConfiguration.staticMenu.first().id)
    Assert.assertEquals("test-3", navigationConfiguration.clientRegisters.first().id)
    Assert.assertEquals("sample display 4", navigationConfiguration.bottomSheetRegisters?.display)
  }
}
