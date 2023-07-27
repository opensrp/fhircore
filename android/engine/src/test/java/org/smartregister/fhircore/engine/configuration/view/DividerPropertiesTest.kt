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

package org.smartregister.fhircore.engine.configuration.view

import org.junit.Assert
import org.junit.Test

class DividerPropertiesTest {
  private val imageProperties =
    DividerProperties(
      backgroundColor = "@{backgroundColor}",
      visible = "@{visible}",
    )

  @Test
  fun testInterpolateInImageProperties() {
    val computedValuesMap = mutableMapOf<String, String>()
    computedValuesMap["backgroundColor"] = "#000000"
    computedValuesMap["visible"] = "false"

    val interpolatedDividerProperties =
      imageProperties.interpolate(computedValuesMap) as DividerProperties

    Assert.assertEquals("#000000", interpolatedDividerProperties.backgroundColor)
    Assert.assertEquals("false", interpolatedDividerProperties.visible)
  }
}
