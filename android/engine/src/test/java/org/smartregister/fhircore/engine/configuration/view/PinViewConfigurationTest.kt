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

class PinViewConfigurationTest {

  @Test
  fun testLoginViewConfiguration() {
    val pinViewConfiguration =
      PinViewConfiguration(
        appId = "anc",
        classification = "classification",
        applicationName = "app",
        appLogoIconResourceFile = "ic_launcher",
        showLogo = true,
        enablePin = true
      )
    Assert.assertEquals("anc", pinViewConfiguration.appId)
    Assert.assertEquals("classification", pinViewConfiguration.classification)
    Assert.assertEquals("app", pinViewConfiguration.applicationName)
    Assert.assertEquals("ic_launcher", pinViewConfiguration.appLogoIconResourceFile)
    Assert.assertTrue(pinViewConfiguration.showLogo)
    Assert.assertTrue(pinViewConfiguration.enablePin)
  }

  @Test
  fun testLoginViewConfigurationOf() {
    val pinConfiguration =
      pinViewConfigurationOf(
        appId = "anc",
        classification = "classification",
        applicationName = "app",
        appLogoIconResourceFile = "ic_launcher",
        showLogo = true,
        enablePin = false
      )
    Assert.assertEquals("anc", pinConfiguration.appId)
    Assert.assertEquals("classification", pinConfiguration.classification)
    Assert.assertEquals("app", pinConfiguration.applicationName)
    Assert.assertEquals("ic_launcher", pinConfiguration.appLogoIconResourceFile)
    Assert.assertTrue(pinConfiguration.showLogo)
    Assert.assertFalse(pinConfiguration.enablePin)
  }
}
