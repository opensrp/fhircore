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
    Assert.assertEquals("HouseHoldManagement", featureConfig.feature)
  }

  @Test
  fun testGetActive_shouldReturn_true() {
    Assert.assertEquals(true, featureConfig.active)
  }

  @Test
  fun testGetSettings_shouldReturn_1() {
    Assert.assertEquals(1, featureConfig.settings.size)
  }

  @Test
  fun testGetSettings_deactivateMembers_shouldContain_deactivateMembers() {
    Assert.assertEquals(true, featureConfig.settings.containsKey("deactivateMembers"))
  }

  @Test
  fun testGetTarget_shouldReturn_CHW() {
    Assert.assertEquals("CHW", featureConfig.target.toStr())
  }

  @Test
  fun testGetHealthModule_shouldReturn_FAMILY() {
    Assert.assertEquals("FAMILY", featureConfig.healthModule.toStr())
  }

  @Test
  fun testGetUseCases_shouldReturn_4() {
    Assert.assertNotNull(featureConfig.useCases)
    Assert.assertEquals(4, featureConfig.useCases!!.size)
  }

  @Test
  fun testGetUseCases_shouldContain_allUseCases() {
    Assert.assertNotNull(featureConfig.useCases)
    Assert.assertEquals(true, featureConfig.useCases!!.contains("HOUSEHOLD_REGISTRATION"))
    Assert.assertEquals(true, featureConfig.useCases!!.contains("REMOVE_HOUSEHOLD"))
    Assert.assertEquals(true, featureConfig.useCases!!.contains("HOUSEHOLD_VISITS"))
    Assert.assertEquals(true, featureConfig.useCases!!.contains("REMOVE_HOUSEHOLD_MEMBER"))
  }
}
