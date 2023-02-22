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
import org.smartregister.fhircore.engine.configuration.register.RegisterCardConfig
import org.smartregister.fhircore.engine.domain.model.ExtractedResource
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.domain.model.ViewType

class ListPropertiesTest {

  private val relatedResources =
    listOf(ExtractedResource(resourceType = "typeResource", fhirPathExpression = "fhirPath"))

  private val registerCardConfig =
    RegisterCardConfig(rules = emptyList<RuleConfig>(), views = emptyList<ViewProperties>())

  private val listProperties =
    ListProperties(
      viewType = ViewType.LIST,
      weight = 0f,
      backgroundColor = "#FFFFFF",
      padding = 0,
      borderRadius = 2,
      alignment = ViewAlignment.NONE,
      fillMaxWidth = false,
      fillMaxHeight = false,
      clickable = "false",
      id = "listid",
      baseResource = "resource",
      relatedResources = relatedResources,
      registerCard = registerCardConfig,
      showDivider = true
    )

  @Test
  fun testListProperties() {
    Assert.assertEquals(ViewType.LIST, listProperties.viewType)
    Assert.assertEquals(0f, listProperties.weight)
    Assert.assertEquals("#FFFFFF", listProperties.backgroundColor)
    Assert.assertEquals(0, listProperties.padding)
    Assert.assertEquals(2, listProperties.borderRadius)
    Assert.assertEquals(ViewAlignment.NONE, listProperties.alignment)
    Assert.assertEquals(false, listProperties.fillMaxWidth)
    Assert.assertEquals(false, listProperties.fillMaxHeight)
    Assert.assertEquals("false", listProperties.clickable)
    Assert.assertEquals("resource", listProperties.baseResource)
    Assert.assertEquals(relatedResources, listProperties.relatedResources)
    Assert.assertEquals(registerCardConfig, listProperties.registerCard)
    Assert.assertEquals(true, listProperties.showDivider)
  }
}
