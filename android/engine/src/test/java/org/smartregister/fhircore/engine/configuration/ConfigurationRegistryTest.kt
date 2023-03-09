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

package org.smartregister.fhircore.engine.configuration

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
class ConfigurationRegistryTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @Inject lateinit var configRegistry: ConfigurationRegistry

  @Before
  fun setUp() {
    hiltRule.inject()
    Assert.assertNotNull(configRegistry)
  }

  @Test
  fun testRetrieveResourceBundleConfigurationReturnsNull() {
    configRegistry.configsJsonMap["stringsEn"] = "name.title=Mr.\n" + "gender.male=Male"
    val resource = configRegistry.retrieveResourceBundleConfiguration("nonexistent")
    Assert.assertNull(resource)
  }

  @Test
  fun testRetrieveResourceBundleConfigurationMissingVariantReturnsBaseResourceBundle() {
    configRegistry.configsJsonMap["strings"] = "name.title=Mr.\n" + "gender.male=Male"
    val resource = configRegistry.retrieveResourceBundleConfiguration("strings_en")
    Assert.assertNotNull(resource)
    Assert.assertEquals("Mr.", resource?.getString("name.title"))
    Assert.assertEquals("Male", resource?.getString("gender.male"))
  }

  @Test
  fun testRetrieveResourceBundleConfigurationReturnsCorrectBundle() {
    configRegistry.configsJsonMap["stringsSw"] = "name.title=Bwana.\n" + "gender.male=Kijana"
    val resource = configRegistry.retrieveResourceBundleConfiguration("strings_sw")
    Assert.assertNotNull(resource)
    Assert.assertEquals("Bwana.", resource?.getString("name.title"))
    Assert.assertEquals("Kijana", resource?.getString("gender.male"))
  }

  @Test
  fun testRetrieveResourceBundleConfigurationWithLocaleVariantReturnsCorrectBundle() {
    configRegistry.configsJsonMap["stringsSw"] = "name.title=Bwana.\n" + "gender.male=Kijana"
    val resource = configRegistry.retrieveResourceBundleConfiguration("strings_sw_KE")
    Assert.assertNotNull(resource)
    Assert.assertEquals("Bwana.", resource?.getString("name.title"))
    Assert.assertEquals("Kijana", resource?.getString("gender.male"))
  }
}
