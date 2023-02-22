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

package org.smartregister.fhircore.engine.configuration.profile

import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.domain.model.ExtractedResource
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.OverflowMenuItemConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.domain.model.RuleConfig

class ProfileConfigurationTest {

  private val fhirResourceConfig =
    FhirResourceConfig(baseResource = ResourceConfig(resource = "Patient"))

  private val extractedResource =
    ExtractedResource(
      resourceType = "Patient",
      fhirPathExpression = "extractedResourceFhirPathExpression"
    )

  private val managingEntityConfig =
    ManagingEntityConfig(
      infoFhirPathExpression = "infoFhirPathExpression test",
      fhirPathResource = extractedResource
    )

  private val rulesList = listOf(RuleConfig(name = "test rule", actions = listOf("actions")))
  private val overflowMenuItemConfigList = listOf(OverflowMenuItemConfig(visible = "false"))
  private val navigationMenuConfigList =
    listOf(
      NavigationMenuConfig(
        id = "1234",
        visible = false,
        enabled = "true",
        display = "sample display",
        showCount = false,
      )
    )

  @Test
  fun testAuthConfiguration() {
    val profileConfiguration =
      ProfileConfiguration(
        appId = "sample fhir path expression",
        configType = ConfigType.Profile.name,
        id = "12345",
        fhirResource = fhirResourceConfig,
        managingEntity = managingEntityConfig,
        profileParams = listOf("param1"),
        rules = rulesList,
        fabActions = navigationMenuConfigList,
        overFlowMenuItems = overflowMenuItemConfigList
      )

    Assert.assertEquals("sample fhir path expression", profileConfiguration.appId)
    Assert.assertEquals("profile", profileConfiguration.configType)
    Assert.assertEquals("12345", profileConfiguration.id)
    Assert.assertEquals(fhirResourceConfig, profileConfiguration.fhirResource)
    Assert.assertEquals(managingEntityConfig, profileConfiguration.managingEntity)
    Assert.assertEquals("param1", profileConfiguration.profileParams.first())
    Assert.assertEquals(rulesList, profileConfiguration.rules)
    Assert.assertEquals(navigationMenuConfigList, profileConfiguration.fabActions)
    Assert.assertEquals(overflowMenuItemConfigList, profileConfiguration.overFlowMenuItems)
  }
}
