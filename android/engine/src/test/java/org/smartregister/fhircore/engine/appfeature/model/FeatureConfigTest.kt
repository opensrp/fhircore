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

import io.mockk.InternalPlatformDsl.toStr
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class FeatureConfigTest {

  lateinit var featureConfig: FeatureConfig

  @Before
  fun setUp() {
    featureConfig =
      FeatureConfig(
        feature = "HouseHoldManagement",
        active = true,
        settings = mapOf("deactivateMembers" to "true"),
        target = AppTarget.CHW,
        healthModule = HealthModule.FAMILY,
        useCases =
          listOf(
            "HOUSEHOLD_REGISTRATION",
            "REMOVE_HOUSEHOLD",
            "HOUSEHOLD_VISITS",
            "REMOVE_HOUSEHOLD_MEMBER"
          )
      )
  }

  @Test
  fun testGetFeature_shouldReturn_HouseHoldManagement() {
    Assert.assertEquals(featureConfig.feature, "HouseHoldManagement")
  }

  @Test
  fun testGetActive_shouldReturn_true() {
    Assert.assertEquals(featureConfig.active, true)
  }

  @Test
  fun testGetSettings_shouldReturn_1() {
    Assert.assertEquals(featureConfig.settings.size, 1)
  }

  @Test
  fun testGetSettings_deactivateMembers_shouldContain_deactivateMembers() {
    Assert.assertEquals(featureConfig.settings.containsKey("deactivateMembers"), true)
  }

  @Test
  fun testGetTarget_shouldReturn_CHW() {
    Assert.assertEquals(featureConfig.target.toStr(), "CHW")
  }

  @Test
  fun testGetHealthModule_shouldReturn_FAMILY() {
    Assert.assertEquals(featureConfig.healthModule.toStr(), "FAMILY")
  }

  @Test
  fun testGetUseCases_shouldReturn_4() {
    Assert.assertNotNull(featureConfig.useCases)
    Assert.assertEquals(featureConfig.useCases!!.size, 4)
  }

  @Test
  fun testGetUseCases_shouldContain_allUseCases() {
    Assert.assertNotNull(featureConfig.useCases)
    Assert.assertEquals(featureConfig.useCases!!.contains("HOUSEHOLD_REGISTRATION"), true)
    Assert.assertEquals(featureConfig.useCases!!.contains("REMOVE_HOUSEHOLD"), true)
    Assert.assertEquals(featureConfig.useCases!!.contains("HOUSEHOLD_VISITS"), true)
    Assert.assertEquals(featureConfig.useCases!!.contains("REMOVE_HOUSEHOLD_MEMBER"), true)
  }
}
