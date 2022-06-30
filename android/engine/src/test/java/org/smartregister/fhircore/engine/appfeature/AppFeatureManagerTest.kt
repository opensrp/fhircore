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
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

class AppFeatureManagerTest : RobolectricTest() {

  val context: Context = ApplicationProvider.getApplicationContext()
  lateinit var dispatcherProvider: DispatcherProvider
  lateinit var appFeatureManager: AppFeatureManager
  lateinit var configurationRegistry: ConfigurationRegistry
  lateinit var defaultRepository: DefaultRepository
  lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  lateinit var fhirResourceDataSource: FhirResourceDataSource

  @Before
  fun setUp() {
    defaultRepository = mockk()
    sharedPreferencesHelper = mockk()
    dispatcherProvider = mockk()
    fhirResourceDataSource = spyk(FhirResourceDataSource(mockk()))
    configurationRegistry =
      ConfigurationRegistry(
        context = context,
        fhirResourceDataSource = fhirResourceDataSource,
        sharedPreferencesHelper = sharedPreferencesHelper,
        dispatcherProvider = dispatcherProvider,
        repository = defaultRepository
      )
    Faker.loadTestConfigurationRegistryData(defaultRepository, configurationRegistry)
    appFeatureManager = AppFeatureManager(configurationRegistry)
  }

  @Test
  fun testActivatedFeatures_shouldReturn_empty() {
    Assert.assertEquals(0, appFeatureManager.activatedFeatures().size)
  }

  @Test
  fun testActivatedFeatures_shouldReturn_2() {
    appFeatureManager.loadAndActivateFeatures()
    Assert.assertEquals(2, appFeatureManager.activatedFeatures().size)
  }

  @Test
  fun testActiveRegisterFeatures_shouldReturn_2() {
    Assert.assertEquals(2, appFeatureManager.activeRegisterFeatures().size)
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
      1,
      appFeatureManager.appFeatureSettings(AppFeature.HouseholdManagement).size
    )
  }

  @Test
  fun testAppFeatureSettings_shouldReturn_empty() {
    appFeatureManager.loadAndActivateFeatures()
    Assert.assertEquals(0, appFeatureManager.appFeatureSettings(AppFeature.PatientManagement).size)
  }

  @Test
  fun testAppFeatureSettings_withStringName_shouldReturn_1() {
    appFeatureManager.loadAndActivateFeatures()
    Assert.assertEquals(1, appFeatureManager.appFeatureSettings("HouseholdManagement").size)
  }

  @Test
  fun testAppFeatureSettings_withStringName_shouldReturn_empty() {
    appFeatureManager.loadAndActivateFeatures()
    Assert.assertEquals(0, appFeatureManager.appFeatureSettings("PatientManagement").size)
  }

  @Test
  fun testAppFeatureHasSetting_shouldReturn_true() {
    appFeatureManager.loadAndActivateFeatures()
    Assert.assertEquals(true, appFeatureManager.appFeatureHasSetting("deactivateMembers"))
  }
}
