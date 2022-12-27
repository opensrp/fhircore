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

class CardViewPropertiesTest {

  private val viewPropertiesList = emptyList<ViewProperties>()
  private val cardViewProperties =
    CardViewProperties(
      viewType = ViewType.CARD,
      weight = 0f,
      backgroundColor = "#F2F4F7",
      padding = 0,
      borderRadius = 2,
      alignment = ViewAlignment.NONE,
      fillMaxWidth = false,
      fillMaxHeight = false,
      clickable = "true",
      content = viewPropertiesList,
      elevation = 5,
      cornerSize = 6,
      header = null,
      headerBackgroundColor = "#F2F4F7",
      viewAllAction = false
    )

  @Test
  fun testCardViewProperties() {
    Assert.assertEquals(ViewType.CARD, cardViewProperties.viewType)
    Assert.assertEquals(0f, cardViewProperties.weight)
    Assert.assertEquals("#F2F4F7", cardViewProperties.backgroundColor)
    Assert.assertEquals(0, cardViewProperties.padding)
    Assert.assertEquals(2, cardViewProperties.borderRadius)
    Assert.assertEquals(ViewAlignment.NONE, cardViewProperties.alignment)
    Assert.assertEquals(false, cardViewProperties.fillMaxWidth)
    Assert.assertEquals(false, cardViewProperties.fillMaxHeight)
    Assert.assertEquals("true", cardViewProperties.clickable)
    Assert.assertEquals(viewPropertiesList, cardViewProperties.content)
    Assert.assertEquals(5, cardViewProperties.elevation)
    Assert.assertEquals(6, cardViewProperties.cornerSize)
    Assert.assertEquals(null, cardViewProperties.header)
    Assert.assertEquals("#F2F4F7", cardViewProperties.headerBackgroundColor)
    Assert.assertEquals(false, cardViewProperties.viewAllAction)
  }
}
