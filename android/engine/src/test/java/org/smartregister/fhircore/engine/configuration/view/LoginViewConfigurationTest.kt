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

class LoginViewConfigurationTest {

  @Test
  fun testLoginViewConfiguration() {
    val loginViewConfiguration =
      LoginViewConfiguration("anc", "classification", "app", "1.0.0", 2, true, true)
    Assert.assertEquals("anc", loginViewConfiguration.appId)
    Assert.assertEquals("classification", loginViewConfiguration.classification)
    Assert.assertEquals("1.0.0", loginViewConfiguration.applicationVersion)
    Assert.assertEquals(2, loginViewConfiguration.applicationVersionCode)
    Assert.assertTrue(loginViewConfiguration.darkMode)
    Assert.assertTrue(loginViewConfiguration.showLogo)
  }

  @Test
  fun testLoginViewConfigurationOf() {
    val loginViewConfigurationOf =
      loginViewConfigurationOf("anc", "classification", "app", "1.0.0", 2, true, true)
    Assert.assertEquals("anc", loginViewConfigurationOf.appId)
    Assert.assertEquals("classification", loginViewConfigurationOf.classification)
    Assert.assertEquals("1.0.0", loginViewConfigurationOf.applicationVersion)
    Assert.assertEquals(2, loginViewConfigurationOf.applicationVersionCode)
    Assert.assertTrue(loginViewConfigurationOf.darkMode)
    Assert.assertTrue(loginViewConfigurationOf.showLogo)
  }
}
