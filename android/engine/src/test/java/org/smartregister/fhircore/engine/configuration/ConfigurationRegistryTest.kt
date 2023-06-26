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
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.search
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Reference
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.view.LoginViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.PinViewConfiguration
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.robolectric.RobolectricTest.Companion.readFile
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class ConfigurationRegistryTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  @Inject lateinit var dispatcherProvider: DispatcherProvider
  val context = ApplicationProvider.getApplicationContext<Context>()
  @get:Rule(order = 1) val coroutineRule = CoroutineTestRule()
  @BindValue val secureSharedPreference: SecureSharedPreference = mockk()
  private val testAppId = "default"
  private val application: Context = ApplicationProvider.getApplicationContext()
  private lateinit var fhirResourceDataSource: FhirResourceDataSource
  lateinit var configurationRegistry: ConfigurationRegistry
  var fhirEngine: FhirEngine = mockk()

  @Before
  fun setUp() {
    hiltRule.inject()
    fhirResourceDataSource = spyk(FhirResourceDataSource(mockk()))
    configurationRegistry =
      ConfigurationRegistry(
        context,
        fhirEngine,
        fhirResourceDataSource,
        sharedPreferencesHelper,
        coroutineRule.testDispatcherProvider,
      )
    Assert.assertNotNull(configurationRegistry)
    Faker.loadTestConfigurationRegistryData(fhirEngine, configurationRegistry)
  }

  @Test
  fun testLoadConfiguration() {
    Assert.assertEquals(testAppId, configurationRegistry.appId)
    Assert.assertTrue(configurationRegistry.workflowPointsMap.isNotEmpty())
    Assert.assertTrue(configurationRegistry.workflowPointsMap.containsKey("default|application"))
    Assert.assertFalse(
      configurationRegistry.workflowPointsMap.containsKey("default|family-registration")
    )
  }

  @Test
  fun testRetrieveConfigurationShouldReturnLoginViewConfiguration() {
    val retrievedConfiguration =
      configurationRegistry.retrieveConfiguration<LoginViewConfiguration>(
        AppConfigClassification.LOGIN
      )

    Assert.assertTrue(configurationRegistry.workflowPointsMap.isNotEmpty())
    val configurationsMap = configurationRegistry.configurationsMap
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
    val retrievedConfiguration =
      configurationRegistry.retrieveConfiguration<PinViewConfiguration>(AppConfigClassification.PIN)

    Assert.assertTrue(configurationRegistry.workflowPointsMap.isNotEmpty())
    val configurationsMap = configurationRegistry.configurationsMap
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
    Assert.assertTrue(configurationRegistry.configurationsMap.isEmpty())

    val retrievedConfiguration =
      configurationRegistry.retrieveConfiguration<PinViewConfiguration>(AppConfigClassification.PIN)

    Assert.assertNotNull(retrievedConfiguration)
  }

  @Test
  fun testLoadConfigurationRegistry() {
    runTest { configurationRegistry.fetchNonWorkflowConfigResources() }

    coVerify { fhirEngine.search<Composition>(any<Search>()) }
  }

  @Test
  fun testIsAppIdInitialized() {
    Assert.assertFalse(configurationRegistry.isAppIdInitialized())
    Assert.assertTrue(configurationRegistry.isAppIdInitialized())
  }

  @Test
  fun testIsWorkflowPointName() {
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
  fun testFetchNonWorkflowConfigResources() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    coEvery { configurationRegistry.searchCompositionByIdentifier(testAppId) } returns
      Composition().apply {
        addSection().apply { this.focus = Reference().apply { reference = "Questionnaire/123" } }
      }
    coEvery { configurationRegistry.fhirResourceDataSource.loadData(any()) } returns Bundle()

    configurationRegistry.appId = testAppId
    configurationRegistry.fetchNonWorkflowConfigResources(dispatcher)

    //    coVerify { configurationRegistry.repository.searchCompositionByIdentifier(any()) }
    advanceUntilIdle()
    coVerify {
      configurationRegistry.fhirResourceDataSource.loadData(
        withArg { Assert.assertTrue(it.startsWith("Questionnaire", ignoreCase = true)) }
      )
    }
  }

  @Test
  fun testFetchNonWorkflowConfigResourcesWithNoEntry() {
    configurationRegistry.appId = "testApp"
    Assert.assertEquals(0, configurationRegistry.workflowPointsMap.size)

    coEvery { configurationRegistry.searchCompositionByIdentifier(any()) } returns null

    runBlocking { configurationRegistry.fetchNonWorkflowConfigResources() }

    //    coVerify { defaultRepository.searchCompositionByIdentifier("testApp") }
    coVerify(inverse = true) { fhirResourceDataSource.loadData(any()) }
  }

  @Test
  fun testIsWorkflowPointReturnsTrueWithBinarySectionComponent() {
    val sectionComponent =
      Composition.SectionComponent().apply {
        this.focus = Reference().apply { reference = "Binary/123" }
      }
    Assert.assertTrue(configurationRegistry.isWorkflowPoint(sectionComponent))
  }

  @Test
  fun testIsWorkflowPointReturnsFalseWithQuestionnaireSectionComponent() {
    val sectionComponent =
      Composition.SectionComponent().apply {
        this.focus = Reference().apply { reference = "Questionnaire/123" }
      }
    Assert.assertFalse(configurationRegistry.isWorkflowPoint(sectionComponent))
  }
}
