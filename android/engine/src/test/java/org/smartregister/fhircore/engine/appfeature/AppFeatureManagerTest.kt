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

package org.smartregister.fhircore.engine.appfeature

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

class AppFeatureManagerTest : RobolectricTest() {

  val context: Context = ApplicationProvider.getApplicationContext()
  lateinit var appFeatureManager: AppFeatureManager
  lateinit var configurationRegistry: ConfigurationRegistry
  lateinit var defaultRepository: DefaultRepository
  lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @Before
  fun setUp() {
    defaultRepository = mockk()
    sharedPreferencesHelper = mockk()
    configurationRegistry =
      ConfigurationRegistry(
        context = context,
        sharedPreferencesHelper = sharedPreferencesHelper,
        repository = defaultRepository
      )
    Faker.loadTestConfigurationRegistryData(defaultRepository, configurationRegistry)
    appFeatureManager = AppFeatureManager(configurationRegistry)
  }

  @Test
  fun testActivatedFeatures_shouldReturn_empty() {
    Assert.assertEquals(appFeatureManager.activatedFeatures().size, 0)
  }

  @Test
  fun testActivatedFeatures_shouldReturn_2() {
    appFeatureManager.loadAndActivateFeatures()
    Assert.assertEquals(appFeatureManager.activatedFeatures().size, 2)
  }

  @Test
  fun testActiveRegisterFeatures_shouldReturn_2() {
    Assert.assertEquals(appFeatureManager.activeRegisterFeatures().size, 2)
  }

  @Test
  fun testIsFeatureActive_shouldReturn_true() {
    appFeatureManager.loadAndActivateFeatures()
    Assert.assertEquals(true, appFeatureManager.isFeatureActive(AppFeature.HouseholdManagement))
  }

  @Test
  fun testIsFeatureActive_shouldReturn_false() {
    appFeatureManager.loadAndActivateFeatures()
    Assert.assertEquals(false, appFeatureManager.isFeatureActive(AppFeature.InAppReporting))
  }

  @Test
  fun testAppFeatureSettings_shouldReturn_1() {
    appFeatureManager.loadAndActivateFeatures()
    Assert.assertEquals(
      appFeatureManager.appFeatureSettings(AppFeature.HouseholdManagement).size,
      1
    )
  }

  @Test
  fun testAppFeatureSettings_shouldReturn_empty() {
    appFeatureManager.loadAndActivateFeatures()
    Assert.assertEquals(appFeatureManager.appFeatureSettings(AppFeature.PatientManagement).size, 0)
  }

  @Test
  fun testAppFeatureSettings_withStringName_shouldReturn_1() {
    appFeatureManager.loadAndActivateFeatures()
    Assert.assertEquals(
      appFeatureManager.appFeatureSettings(AppFeature.HouseholdManagement).size,
      1
    )
  }

  @Test
  fun testAppFeatureSettings_withStringName_shouldReturn_empty() {
    appFeatureManager.loadAndActivateFeatures()
    Assert.assertEquals(appFeatureManager.appFeatureSettings(AppFeature.PatientManagement).size, 0)
  }

  @Test
  fun testAppFeatureHasSetting_shouldReturn_true() {
    appFeatureManager.loadAndActivateFeatures()
    Assert.assertEquals(appFeatureManager.appFeatureHasSetting("deactivateMembers"), true)
  }
}
