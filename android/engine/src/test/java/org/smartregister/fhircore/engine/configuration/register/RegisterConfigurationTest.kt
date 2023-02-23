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

package org.smartregister.fhircore.engine.configuration.register

import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig

class RegisterConfigurationTest {

  private val fhirResourceConfig =
    FhirResourceConfig(ResourceConfig("id", "resource"), relatedResources = emptyList())
  private val registerConfiguration =
    RegisterConfiguration(
      appId = "1234",
      configType = "configType",
      id = "2424",
      registerTitle = "title",
      fhirResource = fhirResourceConfig
    )

  @Test
  fun testRegisterConfiguration() {

    Assert.assertEquals("1234", registerConfiguration.appId)
    Assert.assertEquals("configType", registerConfiguration.configType)
    Assert.assertEquals("2424", registerConfiguration.id)
    Assert.assertEquals("title", registerConfiguration.registerTitle)
    Assert.assertEquals("id", registerConfiguration.fhirResource.baseResource.id)
    Assert.assertEquals("resource", registerConfiguration.fhirResource.baseResource.resource)
  }
}
