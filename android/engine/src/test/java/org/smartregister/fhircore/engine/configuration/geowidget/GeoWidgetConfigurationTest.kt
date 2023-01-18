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

package org.smartregister.fhircore.engine.configuration.geowidget.org.smartregister.fhircore.engine.configuration.geowidget

import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.geowidget.GeoWidgetConfiguration
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig

class GeoWidgetConfigurationTest {
  @Test
  fun testGeoWidgetConfiguration() {
    val appId = "testAppId"
    val id = "testId"
    val profileId = "testProfileId"
    val registrationQuestionnaire = QuestionnaireConfig("testQuestionnaireId")
    val resourceConfig = FhirResourceConfig(baseResource = ResourceConfig("name", "resource"))
    val geoWidgetConfiguration =
      GeoWidgetConfiguration(
        appId,
        id = id,
        profileId = profileId,
        registrationQuestionnaire = registrationQuestionnaire,
        resourceConfig = resourceConfig
      )
    Assert.assertEquals(appId, geoWidgetConfiguration.appId)
    Assert.assertEquals("geoWidget", geoWidgetConfiguration.configType)
    Assert.assertEquals(id, geoWidgetConfiguration.id)
    Assert.assertEquals(profileId, geoWidgetConfiguration.profileId)
    Assert.assertEquals(registrationQuestionnaire, geoWidgetConfiguration.registrationQuestionnaire)
    Assert.assertEquals(resourceConfig, geoWidgetConfiguration.resourceConfig)
  }
}
