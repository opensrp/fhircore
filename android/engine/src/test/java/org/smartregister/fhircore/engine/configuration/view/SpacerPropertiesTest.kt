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

package org.smartregister.fhircore.engine.configuration.view

import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.domain.model.ViewType

class SpacerPropertiesTest {

  private val spacerProperties =
    SpacerProperties(
      viewType = ViewType.SPACER,
      weight = 0f,
      backgroundColor = "#FFFFFF",
      padding = 0,
      borderRadius = 2,
      alignment = ViewAlignment.NONE,
      fillMaxWidth = true,
      fillMaxHeight = true,
      clickable = "true",
      height = 4f,
      width = 10f
    )

  @Test
  fun testSpacerProperties() {
    Assert.assertEquals(ViewType.SPACER, spacerProperties.viewType)
    Assert.assertEquals(0f, spacerProperties.weight)
    Assert.assertEquals("#FFFFFF", spacerProperties.backgroundColor)
    Assert.assertEquals(0, spacerProperties.padding)
    Assert.assertEquals(2, spacerProperties.borderRadius)
    Assert.assertEquals(ViewAlignment.NONE, spacerProperties.alignment)
    Assert.assertEquals(true, spacerProperties.fillMaxWidth)
    Assert.assertEquals(true, spacerProperties.fillMaxHeight)
    Assert.assertEquals("true", spacerProperties.clickable)
    Assert.assertEquals(4f, spacerProperties.height)
    Assert.assertEquals(10f, spacerProperties.width)
  }
}
