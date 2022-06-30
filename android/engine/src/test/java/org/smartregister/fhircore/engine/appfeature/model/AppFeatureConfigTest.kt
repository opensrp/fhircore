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

package org.smartregister.fhircore.engine.appfeature.model

import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class AppFeatureConfigTest {

  lateinit var appFeatureConfig: AppFeatureConfig

  @Before
  fun setUp() {
    appFeatureConfig =
      AppFeatureConfig(
        appId = "quest",
        classification = "app_feature",
        appFeatures = listOf(mockk(), mockk(), mockk())
      )
  }

  @Test
  fun testGetAppId_shouldReturn_test() {
    Assert.assertEquals("quest", appFeatureConfig.appId)
  }

  @Test
  fun testGetClassification_shouldReturn_test_configuration() {
    Assert.assertEquals("app_feature", appFeatureConfig.classification)
  }

  @Test
  fun testGetAppFeatures_shouldReturn_3() {
    Assert.assertEquals(3, appFeatureConfig.appFeatures.size)
  }
}
