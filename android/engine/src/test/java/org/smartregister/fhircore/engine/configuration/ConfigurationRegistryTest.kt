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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Reference
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.view.LoginViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.PinViewConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
class ConfigurationRegistryTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  @Inject lateinit var dispatcherProvider: DispatcherProvider
  val context = ApplicationProvider.getApplicationContext<Context>()

  @BindValue val secureSharedPreference: SecureSharedPreference = mockk()

  private val testAppId = "default"
  private lateinit var fhirResourceDataSource: FhirResourceDataSource
  lateinit var configurationRegistry: ConfigurationRegistry
  val defaultRepository: DefaultRepository = mockk()

  @Before
  fun setUp() {
    hiltRule.inject()
    fhirResourceDataSource = spyk(FhirResourceDataSource(mockk()))
    configurationRegistry =
      ConfigurationRegistry(
        context,
        fhirResourceDataSource,
        sharedPreferencesHelper,
        dispatcherProvider,
        defaultRepository
      )
  }

  @Test
  fun testLoadConfiguration() {
    Faker.loadTestConfigurationRegistryData(defaultRepository, configurationRegistry)

    Assert.assertEquals(testAppId, configurationRegistry.appId)
    Assert.assertTrue(configurationRegistry.workflowPointsMap.isNotEmpty())
    Assert.assertTrue(configurationRegistry.workflowPointsMap.containsKey("default|application"))
    Assert.assertFalse(
      configurationRegistry.workflowPointsMap.containsKey("default|family-registration")
    )
  }

  @Test
  fun testRetrieveConfigurationShouldReturnLoginViewConfiguration() {
    Faker.loadTestConfigurationRegistryData(defaultRepository, configurationRegistry)

    val retrievedConfiguration =
      configurationRegistry.retrieveConfiguration<LoginViewConfiguration>(AppConfigType.LOGIN)

    Assert.assertTrue(configurationRegistry.workflowPointsMap.isNotEmpty())
    val configurationsMap = configurationRegistry.configsJsonMap
    Assert.assertTrue(configurationsMap.isNotEmpty())
    Assert.assertTrue(configurationsMap.containsKey("default|login"))
    Assert.assertTrue(configurationsMap["default|login"]!! is LoginViewConfiguration)

    Assert.assertFalse(retrievedConfiguration.darkMode)
    Assert.assertFalse(retrievedConfiguration.showLogo)
    Assert.assertEquals("default", retrievedConfiguration.appId)
    Assert.assertEquals("login", retrievedConfiguration.classification)
    Assert.assertEquals("Sample App", retrievedConfiguration.applicationName)
    Assert.assertEquals("0.0.1", retrievedConfiguration.applicationVersion)
  }

  @Test
  fun testRetrievePinConfigurationShouldReturnLoginViewConfiguration() {
    Faker.loadTestConfigurationRegistryData(defaultRepository, configurationRegistry)

    val retrievedConfiguration =
      configurationRegistry.retrieveConfiguration<PinViewConfiguration>(AppConfigType.PIN)

    Assert.assertTrue(configurationRegistry.workflowPointsMap.isNotEmpty())
    val configurationsMap = configurationRegistry.configsJsonMap
    Assert.assertTrue(configurationsMap.isNotEmpty())
    Assert.assertTrue(configurationsMap.containsKey("default|pin"))
    Assert.assertTrue(configurationsMap["default|pin"]!! is PinViewConfiguration)

    Assert.assertEquals("default", retrievedConfiguration.appId)
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
    Assert.assertTrue(configurationRegistry.configsJsonMap.isEmpty())

    val retrievedConfiguration =
      configurationRegistry.retrieveConfiguration<PinViewConfiguration>(AppConfigType.PIN)

    Assert.assertNotNull(retrievedConfiguration)
  }

  @Test
  fun testLoadConfigurationRegistry() {
    Faker.loadTestConfigurationRegistryData(defaultRepository, configurationRegistry)

    coVerify { defaultRepository.searchCompositionByIdentifier(testAppId) }
    coVerify { defaultRepository.getBinary("62938") }
    coVerify { defaultRepository.getBinary("62940") }
    coVerify { defaultRepository.getBinary("62952") }
    coVerify { defaultRepository.getBinary("87021") }
    coVerify { defaultRepository.getBinary("63003") }
    coVerify { defaultRepository.getBinary("63011") }
    coVerify { defaultRepository.getBinary("63007") }
    coVerify { defaultRepository.getBinary("56181") }
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

  @ExperimentalCoroutinesApi
  @Test
  fun testLoadConfigurationsLocally_shouldReturn_8_workflows() {
    runTest {
      Assert.assertEquals(0, configurationRegistry.workflowPointsMap.size)
      configurationRegistry.loadConfigurationsLocally("$testAppId/debug") { Assert.assertTrue(it) }
      Assert.assertEquals(9, configurationRegistry.workflowPointsMap.size)

      val workflows = configurationRegistry.workflowPointsMap
      Assert.assertTrue(workflows.containsKey("default|application"))
      Assert.assertTrue(workflows.containsKey("default|login"))
      Assert.assertTrue(workflows.containsKey("default|app_feature"))
      Assert.assertTrue(workflows.containsKey("default|patient_register"))
      Assert.assertTrue(workflows.containsKey("default|patient_task_register"))
      Assert.assertTrue(workflows.containsKey("default|pin"))
      Assert.assertTrue(workflows.containsKey("default|patient_details_view"))
      Assert.assertTrue(workflows.containsKey("default|result_details_navigation"))
      Assert.assertTrue(workflows.containsKey("default|sync"))

      Assert.assertFalse(workflows.containsKey("default|family-registration"))
    }
  }

  @ExperimentalCoroutinesApi
  @Test
  fun testLoadConfigurationsLocally_shouldReturn_empty_workflows() {
    runTest {
      Assert.assertEquals(0, configurationRegistry.workflowPointsMap.size)
      configurationRegistry.loadConfigurationsLocally("") { Assert.assertFalse(it) }
      Assert.assertEquals(0, configurationRegistry.workflowPointsMap.size)
    }
  }

  @Test
  fun testFetchNonWorkflowConfigResources() {
    coEvery { configurationRegistry.repository.searchCompositionByIdentifier(testAppId) } returns
      Composition().apply {
        addSection().apply { this.focus = Reference().apply { reference = "Questionnaire/123" } }
      }

    runBlocking {
      configurationRegistry.appId = testAppId
      configurationRegistry.fetchNonWorkflowConfigResources()
    }

    coVerify { configurationRegistry.repository.searchCompositionByIdentifier(any()) }
    coVerify { configurationRegistry.fhirResourceDataSource.loadData(any()) }
  }

  @Test
  fun testFetchNonWorkflowConfigResourcesWithNoEntry() {
    configurationRegistry.appId = "testApp"
    Assert.assertEquals(0, configurationRegistry.workflowPointsMap.size)

    coEvery { defaultRepository.searchCompositionByIdentifier(any()) } returns null

    runBlocking { configurationRegistry.fetchNonWorkflowConfigResources() }

    coVerify { defaultRepository.searchCompositionByIdentifier("testApp") }
    coVerify(inverse = true) { fhirResourceDataSource.loadData(any()) }
  }

  @Test
  fun testIsWorkflowPointReturnsTrueWithBinarySectionComponent() {
    val sectionComponent =
      Composition.SectionComponent().apply {
        this.focus = Reference().apply { reference = "Binary/123" }
      }
    Assert.assertTrue(configurationRegistry.isApplicationConfig(sectionComponent))
  }

  @Test
  fun testIsWorkflowPointReturnsFalseWithQuestionnaireSectionComponent() {
    val sectionComponent =
      Composition.SectionComponent().apply {
        this.focus = Reference().apply { reference = "Questionnaire/123" }
      }
    Assert.assertFalse(configurationRegistry.isApplicationConfig(sectionComponent))
  }
}
