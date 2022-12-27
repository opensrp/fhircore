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

class RowPropertiesTest {

  private val viewPropertiesList = emptyList<ViewProperties>()

  private val rowProperties =
    RowProperties(
      viewType = ViewType.ROW,
      weight = 0f,
      backgroundColor = "#F5F5F5",
      padding = 0,
      borderRadius = 0,
      alignment = ViewAlignment.NONE,
      fillMaxWidth = false,
      fillMaxHeight = false,
      clickable = "true",
      spacedBy = 8,
      arrangement = RowArrangement.CENTER,
      wrapContent = true,
      children = viewPropertiesList
    )

  @Test
  fun testRowProperties() {
    Assert.assertEquals(ViewType.ROW, rowProperties.viewType)
    Assert.assertEquals(0f, rowProperties.weight)
    Assert.assertEquals("#F5F5F5", rowProperties.backgroundColor)
    Assert.assertEquals(0, rowProperties.padding)
    Assert.assertEquals(0, rowProperties.borderRadius)
    Assert.assertEquals(ViewAlignment.NONE, rowProperties.alignment)
    Assert.assertEquals(false, rowProperties.fillMaxWidth)
    Assert.assertEquals(false, rowProperties.fillMaxHeight)
    Assert.assertEquals("true", rowProperties.clickable)
    Assert.assertEquals(RowArrangement.CENTER, rowProperties.arrangement)
    Assert.assertEquals(true, rowProperties.wrapContent)
    Assert.assertEquals(viewPropertiesList, rowProperties.children)
  }
}
