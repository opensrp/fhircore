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

import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.mockk
import io.mockk.spyk
import javax.inject.Inject
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.view.LoginViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.PinViewConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
class ConfigurationRegistryTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @Inject lateinit var accountAuthenticator: AccountAuthenticator

  @BindValue val repository: DefaultRepository = mockk()

  private val context = ApplicationProvider.getApplicationContext<HiltTestApplication>()

  private val testAppId = "appId"

  private lateinit var configurationRegistry: ConfigurationRegistry

  @Before
  fun setUp() {
    hiltRule.inject()
    configurationRegistry =
      spyk(
        ConfigurationRegistry(
          context = context,
          sharedPreferencesHelper = sharedPreferencesHelper,
          repository = repository
        )
      )
  }

  @Test
  fun testLoadConfiguration() {
    // appId should be provided in assets/configurations/application_configurations.json
    configurationRegistry.loadAppConfigurations(
      appId = testAppId,
      accountAuthenticator = accountAuthenticator
    ) {}
    Assert.assertEquals(testAppId, configurationRegistry.appId)
    Assert.assertTrue(configurationRegistry.configurationsMap.isNotEmpty())
    Assert.assertTrue(configurationRegistry.configurationsMap.containsKey("appId|application"))
  }

  @Test
  fun testRetrieveConfigurationShouldReturnLoginViewConfiguration() {
    configurationRegistry.loadAppConfigurations(testAppId, accountAuthenticator) {}
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

  @Test(expected = UninitializedPropertyAccessException::class)
  fun testRetrieveConfigurationShouldThrowAnExceptionWhenAppIdNotProvided() {
    // AppId not initialized; throw UninitializedPropertyAccessException
    configurationRegistry.retrieveConfiguration<LoginViewConfiguration>(
      AppConfigClassification.LOGIN
    )
  }

  @Test(expected = NoSuchElementException::class)
  fun testRetrieveConfigurationShouldThrowAnExceptionWhenAppIdProvided() {
    configurationRegistry.appId = testAppId
    // WorkflowPoint not initialized; throw NoSuchElementException
    configurationRegistry.retrieveConfiguration<LoginViewConfiguration>(
      AppConfigClassification.LOGIN
    )
  }

  @Test
  fun testRetrievePinConfigurationShouldReturnLoginViewConfiguration() {
    configurationRegistry.loadAppConfigurations(testAppId, accountAuthenticator) {}
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

  @Test(expected = UninitializedPropertyAccessException::class)
  fun testRetrievePinConfigurationShouldThrowAnExceptionWhenAppIdNotProvided() {
    // AppId not initialized; throw UninitializedPropertyAccessException
    configurationRegistry.retrieveConfiguration<LoginViewConfiguration>(AppConfigClassification.PIN)
  }

  @Test(expected = NoSuchElementException::class)
  fun testRetrievePinConfigurationShouldThrowAnExceptionWhenAppIdProvided() {
    configurationRegistry.appId = testAppId
    // WorkflowPoint not initialized; throw NoSuchElementException
    configurationRegistry.retrieveConfiguration<PinViewConfiguration>(AppConfigClassification.PIN)
  }
}
