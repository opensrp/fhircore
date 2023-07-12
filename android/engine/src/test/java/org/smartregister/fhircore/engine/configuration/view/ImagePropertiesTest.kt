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
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_LOCAL
import org.smartregister.fhircore.engine.configuration.navigation.ImageConfig

class ImagePropertiesTest {
  private val imageProperties =
    ImageProperties(
      tint = "@{taskStatusColorCodeToTint}",
      imageConfig = ImageConfig("@{taskStatusIcon}", ICON_TYPE_LOCAL),
    )

  @Test
  fun testInterpolateInImageProperties() {
    val computedValuesMap = mutableMapOf<String, String>()
    computedValuesMap["taskStatusColorCodeToTint"] = "successColor"
    computedValuesMap["taskStatusIcon"] = "ic_green_tick"

    val interpolatedImageProperties =
      imageProperties.interpolate(computedValuesMap) as ImageProperties

    Assert.assertEquals("successColor", interpolatedImageProperties.tint)
    Assert.assertEquals("ic_green_tick", interpolatedImageProperties.imageConfig?.type)
  }
}
