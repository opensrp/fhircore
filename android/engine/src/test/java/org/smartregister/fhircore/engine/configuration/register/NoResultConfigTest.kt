/*
 * Copyright 2021-2023 Ona Systems, Inc
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
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig

class NoResultConfigTest {

  @Test
  fun testNoResultConfig() {
    val noResultsConfig =
      NoResultsConfig(
        title = "title",
        message = "message",
        actionButton =
          NavigationMenuConfig(
            id = "1234",
            visible = false,
            enabled = "true",
            display = "sample display",
            showCount = false,
          )
      )
    Assert.assertEquals("title", noResultsConfig.title)
    Assert.assertEquals("message", noResultsConfig.message)
    Assert.assertEquals("1234", noResultsConfig.actionButton?.id)
    Assert.assertEquals(false, noResultsConfig.actionButton?.visible)
    Assert.assertEquals("true", noResultsConfig.actionButton?.enabled)
    Assert.assertEquals("sample display", noResultsConfig.actionButton?.display)
    Assert.assertEquals(false, noResultsConfig.actionButton?.showCount)
  }
}
