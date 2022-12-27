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
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ViewType

class ButtonPropertiesTest {

  private val actionsList = emptyList<ActionConfig>()
  private val buttonProperties =
    ButtonProperties(
      viewType = ViewType.BUTTON,
      weight = 0f,
      backgroundColor = "#F2F4F7",
      padding = 0,
      borderRadius = 2,
      alignment = ViewAlignment.NONE,
      fillMaxWidth = true,
      fillMaxHeight = false,
      clickable = "false",
      visible = "true",
      enabled = "true",
      text = "button",
      status = "buttonStatus",
      smallSized = false,
      fontSize = 10.0f,
      actions = actionsList
    )

  @Test
  fun testButtonProperties() {
    Assert.assertEquals(ViewType.BUTTON, buttonProperties.viewType)
    Assert.assertEquals(0f, buttonProperties.weight)
    Assert.assertEquals("#F2F4F7", buttonProperties.backgroundColor)
    Assert.assertEquals(0, buttonProperties.padding)
    Assert.assertEquals(2, buttonProperties.borderRadius)
    Assert.assertEquals(ViewAlignment.NONE, buttonProperties.alignment)
    Assert.assertEquals(true, buttonProperties.fillMaxWidth)
    Assert.assertEquals(false, buttonProperties.fillMaxHeight)
    Assert.assertEquals("false", buttonProperties.clickable)
    Assert.assertEquals("true", buttonProperties.visible)
    Assert.assertEquals("true", buttonProperties.enabled)
    Assert.assertEquals("button", buttonProperties.text)
    Assert.assertEquals("buttonStatus", buttonProperties.status)
    Assert.assertEquals(false, buttonProperties.smallSized)
    Assert.assertEquals(10.0f, buttonProperties.fontSize)
    Assert.assertEquals(actionsList, buttonProperties.actions)
  }
}
