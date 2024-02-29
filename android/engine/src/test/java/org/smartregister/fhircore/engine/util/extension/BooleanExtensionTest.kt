/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.util.extension

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

class BooleanExtensionTest {

  @Test
  fun testAppIdExistsAndIsNotNullIsTrueWhenAppIdEndsWithDebugSuffix() {
    val mockSharedPreferencesHelper = mockk<SharedPreferencesHelper>()

    val mockAppId = "mockAppId${ConfigurationRegistry.DEBUG_SUFFIX}"

    every { (mockSharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null)) } returns
      mockAppId

    val result = appIdExistsAndIsNotNull(mockSharedPreferencesHelper)

    Assert.assertTrue(result)
  }

  @Test
  fun testAppIdExistsAndIsNotNullIsFalseWhenAppIdIsMissingDebugSuffix() {
    val mockSharedPreferencesHelper = mockk<SharedPreferencesHelper>()

    val mockAppId = "mockAppId$ConfigurationRegistry"

    every { (mockSharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null)) } returns
      mockAppId

    val result = appIdExistsAndIsNotNull(mockSharedPreferencesHelper)

    Assert.assertFalse(result)
  }
}
