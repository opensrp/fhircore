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

package org.smartregister.fhircore.engine.configuration

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.decodeJson

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

  @Test
  fun `test the configuration retrival with the Binary Resource response`() {
    configRegistry.configsJsonMap["application"] =
      "{\n" +
        "    \"resourceType\": \"Binary\",\n" +
        "    \"id\": \"ee550614-5a60-4528-babf-b496c3224743\",\n" +
        "    \"contentType\": \"application/json\",\n" +
        "    \"data\": \"ew0KCSJhcHBJZCI6ICJjaHNzIiwNCgkiY29uZmlnVHlwZSI6ICJhcHBsaWNhdGlvbiIsDQoJInRoZW1lIjogIkRFRkFVTFQiLA0KCSJhcHBUaXRsZSI6ICJlQ0JJUyBDSFNTIiwNCgkicmVtb3RlU3luY1BhZ2VTaXplIjogMTAwLA0KCSJsYW5ndWFnZXMiOiBbDQoJCSJlbiINCgldLA0KCSJ1c2VEYXJrVGhlbWUiOiBmYWxzZSwNCgkic3luY0ludGVydmFsIjogMzAsDQoJInN5bmNTdHJhdGVneSI6IFsNCgkJIkxvY2F0aW9uIg0KCV0sDQoJImxvZ2luQ29uZmlnIjogew0KCQkic2hvd0xvZ28iOiB0cnVlLA0KCQkiZW5hYmxlUGluIjogdHJ1ZQ0KCX0sDQoJImRldmljZVRvRGV2aWNlU3luYyI6IHsNCgkJInJlc291cmNlc1RvU3luYyI6IFsNCgkJCSJHcm91cCIsDQoJCQkiUGF0aWVudCIsDQoJCQkiQ2FyZVBsYW4iLA0KCQkJIlRhc2siLA0KCQkJIkVuY291bnRlciIsDQoJCQkiT2JzZXJ2YXRpb24iLA0KCQkJIkNvbmRpdGlvbiIsDQoJCQkiUXVlc3Rpb25uYWlyZSIsDQoJCQkiUXVlc3Rpb25uYWlyZVJlc3BvbnNlIg0KCQldDQoJfQ0KfQ==\"\n" +
        "}"
    val template = configRegistry.getConfigurationTemplate("application")
    Assert.assertNotNull(template)
    val applicationConfig = template?.decodeJson<ApplicationConfiguration>()
    Assert.assertTrue(applicationConfig?.appId == "chss")
  }
}
