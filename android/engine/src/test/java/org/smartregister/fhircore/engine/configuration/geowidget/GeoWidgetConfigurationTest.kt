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

package org.smartregister.fhircore.engine.configuration.geowidget

import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig

class GeoWidgetConfigurationTest {
  @Test
  fun testAuthConfiguration() {
    val geoWidgetConfiguration =
      GeoWidgetConfiguration(
        appId = "demo_app",
        configType = ConfigType.GeoWidget.name,
        id = "test_id",
        profileId = "profile_test_id",
        registrationQuestionnaire = QuestionnaireConfig(id = "1090"),
        resourceConfig = FhirResourceConfig(baseResource = ResourceConfig(resource = "Patient"))
      )

    Assert.assertEquals("demo_app", geoWidgetConfiguration.appId)
    Assert.assertEquals("geoWidget", geoWidgetConfiguration.configType)
    Assert.assertEquals("test_id", geoWidgetConfiguration.id)
    Assert.assertEquals("profile_test_id", geoWidgetConfiguration.profileId)
    Assert.assertEquals("1090", geoWidgetConfiguration.registrationQuestionnaire.id)
  }

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
