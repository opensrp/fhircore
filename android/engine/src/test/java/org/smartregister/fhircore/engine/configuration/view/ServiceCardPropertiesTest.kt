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

class ServiceCardPropertiesTest {

  private val compoundTextPropertiesList = emptyList<CompoundTextProperties>()

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
      actions = emptyList()
    )
  private val actionsList = emptyList<ActionConfig>()
  private val serviceCardProperties =
    ServiceCardProperties(
      viewType = ViewType.SERVICE_CARD,
      weight = 0f,
      backgroundColor = "#FFFFFF",
      padding = 0,
      borderRadius = 2,
      alignment = ViewAlignment.NONE,
      fillMaxWidth = true,
      fillMaxHeight = false,
      clickable = "true",
      details = compoundTextPropertiesList,
      showVerticalDivider = true,
      serviceMemberIcons = "serviceIcons",
      serviceButton = buttonProperties,
      actions = actionsList
    )

  @Test
  fun testServiceCardProperties() {
    Assert.assertEquals(ViewType.SERVICE_CARD, serviceCardProperties.viewType)
    Assert.assertEquals(0f, serviceCardProperties.weight)
    Assert.assertEquals("#FFFFFF", serviceCardProperties.backgroundColor)
    Assert.assertEquals(0, serviceCardProperties.padding)
    Assert.assertEquals(2, serviceCardProperties.borderRadius)
    Assert.assertEquals(ViewAlignment.NONE, serviceCardProperties.alignment)
    Assert.assertEquals(true, serviceCardProperties.fillMaxWidth)
    Assert.assertEquals(false, serviceCardProperties.fillMaxHeight)
    Assert.assertEquals("true", serviceCardProperties.clickable)
    Assert.assertEquals(compoundTextPropertiesList, serviceCardProperties.details)
    Assert.assertEquals(true, serviceCardProperties.showVerticalDivider)
    Assert.assertEquals("serviceIcons", serviceCardProperties.serviceMemberIcons)
    Assert.assertEquals(buttonProperties, serviceCardProperties.serviceButton)
    Assert.assertEquals(actionsList, serviceCardProperties.actions)
  }
}
