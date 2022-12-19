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

class NavigationBottomSheetRegisterTest {

  @Test
  fun testAuthConfiguration() {
    val navigationBottomSheetRegisterConfig =
      NavigationBottomSheetRegisterConfig(
        visible = false,
        display = "some test display",
        menuIconConfig = MenuIconConfig(type = "ic_menu")
      )

    Assert.assertEquals(false, navigationBottomSheetRegisterConfig.visible)
    Assert.assertEquals("some test display", navigationBottomSheetRegisterConfig.display)
    Assert.assertEquals("ic_menu", navigationBottomSheetRegisterConfig.menuIconConfig?.type)
  }
}
