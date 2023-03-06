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

package org.smartregister.fhircore.engine.configuration.app

import org.junit.Assert
import org.junit.Before
import org.junit.Test

class LoginConfigTest() {
  private lateinit var loginConfig: LoginConfig

  @Before
  fun setUp() {
    loginConfig = LoginConfig()
  }

  @Test
  fun testGetShowLogo() {
    Assert.assertTrue(loginConfig.showLogo)
    loginConfig = LoginConfig(showLogo = false, enablePin = true, pinLength = 8)
    Assert.assertFalse(loginConfig.showLogo)
  }

  @Test
  fun testGetEnablePin() {
    Assert.assertEquals(false, loginConfig.enablePin)
    loginConfig = LoginConfig(showLogo = false, enablePin = true, pinLength = 8)
    Assert.assertEquals(true, loginConfig.enablePin)
  }

  @Test
  fun testGetPinLength() {
    Assert.assertEquals(4, loginConfig.pinLength)
    loginConfig = LoginConfig(showLogo = false, enablePin = true, pinLength = 8)
    Assert.assertEquals(8, loginConfig.pinLength)
  }
}
