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
import org.smartregister.fhircore.engine.domain.model.ViewType

class ColumnPropertiesTest {

  private val viewPropertiesList = emptyList<ViewProperties>()
  private val columnProperties =
    ColumnProperties(
      viewType = ViewType.COLUMN,
      weight = 0f,
      backgroundColor = "#F2F4F7",
      clickable = "false",
      padding = 0,
      borderRadius = 0,
      alignment = ViewAlignment.NONE,
      fillMaxWidth = false,
      fillMaxHeight = false,
      spacedBy = 8,
      wrapContent = false,
      arrangement = ColumnArrangement.SPACE_BETWEEN,
      children = viewPropertiesList
    )

  @Test
  fun testColumnProperties() {
    Assert.assertEquals(ViewType.COLUMN, columnProperties.viewType)
    Assert.assertEquals(0f, columnProperties.weight)
    Assert.assertEquals("#F2F4F7", columnProperties.backgroundColor)
    Assert.assertEquals("false", columnProperties.clickable)
    Assert.assertEquals(0, columnProperties.borderRadius)
    Assert.assertEquals(ViewAlignment.NONE, columnProperties.alignment)
    Assert.assertEquals(false, columnProperties.fillMaxWidth)
    Assert.assertEquals(false, columnProperties.fillMaxHeight)
    Assert.assertEquals(8, columnProperties.spacedBy)
    Assert.assertEquals(false, columnProperties.wrapContent)
    Assert.assertEquals(ColumnArrangement.SPACE_BETWEEN, columnProperties.arrangement)
    Assert.assertEquals(viewPropertiesList, columnProperties.children)
  }
}
