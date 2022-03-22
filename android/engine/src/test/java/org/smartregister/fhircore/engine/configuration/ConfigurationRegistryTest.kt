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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coVerify
import io.mockk.mockk
import javax.inject.Inject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.view.LoginViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.PinViewConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
class ConfigurationRegistryTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  val context = ApplicationProvider.getApplicationContext<Context>()

  @BindValue val secureSharedPreference: SecureSharedPreference = mockk()

  private val testAppId = "appId"

  lateinit var configurationRegistry: ConfigurationRegistry
  val defaultRepository: DefaultRepository = mockk()

  @Before
  fun setUp() {
    hiltRule.inject()

    configurationRegistry = ConfigurationRegistry(context, mockk(), defaultRepository)
  }

  @Test
  fun testLoadConfiguration() {
    Faker.loadTestConfigurationRegistryData(defaultRepository, configurationRegistry)

    Assert.assertEquals(testAppId, configurationRegistry.appId)
    Assert.assertTrue(configurationRegistry.workflowPointsMap.isNotEmpty())
    Assert.assertTrue(configurationRegistry.workflowPointsMap.containsKey("appId|application"))
  }

  @Test
  fun testRetrieveConfigurationShouldReturnLoginViewConfiguration() {
    Faker.loadTestConfigurationRegistryData(defaultRepository, configurationRegistry)

    val retrievedConfiguration =
      configurationRegistry.retrieveConfiguration<LoginViewConfiguration>(
        AppConfigClassification.LOGIN
      )

    Assert.assertTrue(configurationRegistry.workflowPointsMap.isNotEmpty())
    val configurationsMap = configurationRegistry.configurationsMap
    Assert.assertTrue(configurationsMap.isNotEmpty())
    Assert.assertTrue(configurationsMap.containsKey("appId|login"))
    Assert.assertTrue(configurationsMap["appId|login"]!! is LoginViewConfiguration)

    Assert.assertFalse(retrievedConfiguration.darkMode)
    Assert.assertFalse(retrievedConfiguration.showLogo)
    Assert.assertEquals("appId", retrievedConfiguration.appId)
    Assert.assertEquals("login", retrievedConfiguration.classification)
    Assert.assertEquals("Sample App", retrievedConfiguration.applicationName)
    Assert.assertEquals("0.0.1", retrievedConfiguration.applicationVersion)
  }

  @Test
  fun testRetrievePinConfigurationShouldReturnLoginViewConfiguration() {
    Faker.loadTestConfigurationRegistryData(defaultRepository, configurationRegistry)

    val retrievedConfiguration =
      configurationRegistry.retrieveConfiguration<PinViewConfiguration>(AppConfigClassification.PIN)

    Assert.assertTrue(configurationRegistry.workflowPointsMap.isNotEmpty())
    val configurationsMap = configurationRegistry.configurationsMap
    Assert.assertTrue(configurationsMap.isNotEmpty())
    Assert.assertTrue(configurationsMap.containsKey("appId|pin"))
    Assert.assertTrue(configurationsMap["appId|pin"]!! is PinViewConfiguration)

    Assert.assertEquals("appId", retrievedConfiguration.appId)
    Assert.assertEquals("pin", retrievedConfiguration.classification)
    Assert.assertEquals("Sample App", retrievedConfiguration.applicationName)
    Assert.assertEquals("ic_launcher", retrievedConfiguration.appLogoIconResourceFile)
    Assert.assertTrue(retrievedConfiguration.enablePin)
    Assert.assertTrue(retrievedConfiguration.showLogo)
  }

  @Test
  fun testRetrieveConfigurationWithNoEntryShouldReturnNewConfiguration() {
    configurationRegistry.appId = "testApp"

    Assert.assertTrue(configurationRegistry.workflowPointsMap.isEmpty())
    Assert.assertTrue(configurationRegistry.configurationsMap.isEmpty())

    val retrievedConfiguration =
      configurationRegistry.retrieveConfiguration<PinViewConfiguration>(AppConfigClassification.PIN)

    Assert.assertNotNull(retrievedConfiguration)
  }

  @Test
  fun testLoadConfigurationRegistry() {
    Faker.loadTestConfigurationRegistryData(defaultRepository, configurationRegistry)

    coVerify { defaultRepository.searchCompositionByIdentifier(testAppId) }
    coVerify { defaultRepository.getBinary("b_application") }
    coVerify { defaultRepository.getBinary("b_login") }
    coVerify { defaultRepository.getBinary("b_pin_view") }
    coVerify { defaultRepository.getBinary("b_patient_register") }
    coVerify { defaultRepository.getBinary("b_sync") }
  }

  @Test
  fun testIsAppIdInitialized() {
    Assert.assertFalse(configurationRegistry.isAppIdInitialized())

    Faker.loadTestConfigurationRegistryData(defaultRepository, configurationRegistry)

    Assert.assertTrue(configurationRegistry.isAppIdInitialized())
  }

  @Test
  fun testIsWorkflowPointName() {
    Faker.loadTestConfigurationRegistryData(defaultRepository, configurationRegistry)

    Assert.assertEquals("$testAppId|123", configurationRegistry.workflowPointName("123"))
    Assert.assertEquals("$testAppId|abbb", configurationRegistry.workflowPointName("abbb"))
  }
}
