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
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ViewType

class CompoundTextPropertiesTest {

  private val primaryTextActions = emptyList<ActionConfig>()
  private val secondaryTextActions = emptyList<ActionConfig>()
  private val compoundTextProperties =
    CompoundTextProperties(
      viewType = ViewType.COMPOUND_TEXT,
      weight = 0f,
      backgroundColor = "#F2F4F7",
      padding = 0,
      borderRadius = 2,
      alignment = ViewAlignment.NONE,
      fillMaxWidth = false,
      fillMaxHeight = false,
      clickable = "false",
      primaryText = "primaryText",
      primaryTextColor = "#F2F4F7",
      secondaryText = "secondaryText",
      secondaryTextColor = "#F2F4F7",
      separator = null,
      fontSize = 16.0f,
      primaryTextBackgroundColor = "#F2F4F7",
      secondaryTextBackgroundColor = "#F2F4F7",
      primaryTextFontWeight = TextFontWeight.NORMAL,
      secondaryTextFontWeight = TextFontWeight.NORMAL,
      primaryTextActions = primaryTextActions,
      secondaryTextActions = secondaryTextActions
    )

  @Test
  fun testCompoundTextProperties() {
    Assert.assertEquals(ViewType.COMPOUND_TEXT, compoundTextProperties.viewType)
    Assert.assertEquals(0f, compoundTextProperties.weight)
    Assert.assertEquals("#F2F4F7", compoundTextProperties.backgroundColor)
    Assert.assertEquals(0, compoundTextProperties.padding)
    Assert.assertEquals(2, compoundTextProperties.borderRadius)
    Assert.assertEquals(ViewAlignment.NONE, compoundTextProperties.alignment)
    Assert.assertEquals(false, compoundTextProperties.fillMaxWidth)
    Assert.assertEquals(false, compoundTextProperties.fillMaxHeight)
    Assert.assertEquals("false", compoundTextProperties.clickable)
    Assert.assertEquals("primaryText", compoundTextProperties.primaryText)
    Assert.assertEquals("#F2F4F7", compoundTextProperties.primaryTextColor)
    Assert.assertEquals("secondaryText", compoundTextProperties.secondaryText)
    Assert.assertEquals("#F2F4F7", compoundTextProperties.secondaryTextColor)
    Assert.assertEquals(null, compoundTextProperties.separator)
    Assert.assertEquals(16.0f, compoundTextProperties.fontSize)
    Assert.assertEquals("#F2F4F7", compoundTextProperties.primaryTextBackgroundColor)
    Assert.assertEquals("#F2F4F7", compoundTextProperties.secondaryTextBackgroundColor)
    Assert.assertEquals(TextFontWeight.NORMAL, compoundTextProperties.primaryTextFontWeight)
    Assert.assertEquals(TextFontWeight.NORMAL, compoundTextProperties.secondaryTextFontWeight)
    Assert.assertEquals(primaryTextActions, compoundTextProperties.primaryTextActions)
    Assert.assertEquals(secondaryTextActions, compoundTextProperties.secondaryTextActions)
  }
}
