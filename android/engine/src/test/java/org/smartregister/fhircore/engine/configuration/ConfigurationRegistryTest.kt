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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Search
import com.google.gson.GsonBuilder
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Composition.SectionComponent
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.AppConfigService
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
class ConfigurationRegistryTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  @get:Rule(order = 1)
  val coroutineRule = CoroutineTestRule()
  @Inject lateinit var configRegistry: ConfigurationRegistry
  var fhirEngine: FhirEngine = mockk()
  lateinit var context: Context
  private lateinit var fhirResourceDataSource: FhirResourceDataSource

  @Before
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun setUp() {
    hiltRule.inject()
    val fhirResourceService = mockk<FhirResourceService>()
    fhirResourceDataSource = spyk(FhirResourceDataSource(fhirResourceService))
    context = ApplicationProvider.getApplicationContext()
    val sharedPreferencesHelper =
      SharedPreferencesHelper(context, GsonBuilder().setLenient().create())
    configRegistry =
      ConfigurationRegistry(
        fhirEngine,
        fhirResourceDataSource,
        sharedPreferencesHelper,
        coroutineRule.testDispatcherProvider,
        AppConfigService(context),
        Faker.json
      )
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
  fun testRetrieveConfigurationParseTemplate() {
    val appId = "idOfApp"
    configRegistry.configsJsonMap[ConfigType.Application.name] = "{\"appId\": \"${appId}\"}"
    val appConfig =
      configRegistry.retrieveConfiguration<ApplicationConfiguration>(ConfigType.Application)
    Assert.assertEquals(appId, appConfig.appId)
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testRetrieveConfigurationParseTemplateMultiConfig() {
    val appId = "idOfApp"
    val id = "register"
    configRegistry.configsJsonMap[ConfigType.Register.name] =
      "{\"appId\": \"${appId}\", \"id\": \"${id}\", \"fhirResource\": {\"baseResource\": { \"resource\": \"Patient\"}}}"
    val registerConfig =
      configRegistry.retrieveConfiguration<RegisterConfiguration>(ConfigType.Register)
    Assert.assertEquals(appId, registerConfig.appId)
    Assert.assertEquals(id, registerConfig.id)
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testRetrieveConfigurationParseTemplateMultiConfigConfigId() {
    val appId = "idOfApp"
    val id = "register"
    val configId = "idOfConfig"
    configRegistry.configsJsonMap[configId] =
      "{\"appId\": \"${appId}\", \"id\": \"${id}\", \"fhirResource\": {\"baseResource\": { \"resource\": \"Patient\"}}}"
    val registerConfig =
      configRegistry.retrieveConfiguration<RegisterConfiguration>(ConfigType.Register, configId)
    Assert.assertTrue(configRegistry.configCacheMap.containsKey(configId))
    Assert.assertEquals(appId, registerConfig.appId)
    Assert.assertEquals(id, registerConfig.id)
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testRetrieveConfigurationParseResource() {
    Assert.assertThrows("Configuration MUST be a template", IllegalArgumentException::class.java) {
      configRegistry.retrieveConfiguration<ApplicationConfiguration>(ConfigType.Sync)
    }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testFetchNonWorkflowConfigResourcesNoAppId() {
    runTest { configRegistry.fetchNonWorkflowConfigResources() }

    coVerify(inverse = true) { fhirEngine.search<Composition>(any<Search>()) }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testFetchNonWorkflowConfigResourcesAppIdExists() {
    coEvery { fhirEngine.search<Composition>(any<Search>()) } returns listOf()
    val appId = "theAppId"
    configRegistry.sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, appId)

    runTest { configRegistry.fetchNonWorkflowConfigResources() }

    coVerify(inverse = true) { fhirEngine.create(any()) }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testFetchNonWorkflowConfigResourcesHasComposition() {
    val appId = "theAppId"
    val composition = Composition().apply { identifier = Identifier().apply { value = appId } }
    coEvery { fhirEngine.search<Composition>(any<Search>()) } returns listOf(composition)
    configRegistry.sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, appId)

    runTest { configRegistry.fetchNonWorkflowConfigResources() }

    coVerify(inverse = true) { fhirEngine.create(any()) }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testFetchNonWorkflowConfigResourcesEmptyBundle() {
    val appId = "theAppId"
    val composition =
      Composition().apply {
        identifier = Identifier().apply { value = appId }
        section =
          listOf(
            Composition.SectionComponent().apply {
              focus.reference = ResourceType.Questionnaire.name
            }
          )
      }
    configRegistry.sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, appId)
    coEvery { fhirEngine.create(composition) } returns listOf(composition.id)
    coEvery { fhirEngine.search<Composition>(Search(composition.resourceType)) } returns
      listOf(composition)
    coEvery { fhirResourceDataSource.getResource(any()) } returns Bundle()

    runTest {
      configRegistry.fhirEngine.create(composition)
      configRegistry.fetchNonWorkflowConfigResources()
    }

    coVerify(inverse = true) { fhirEngine.get(any(), any()) }
    coVerify(inverse = true) { fhirEngine.update(any()) }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testFetchNonWorkflowConfigResourcesBundle() {
    val appId = "theAppId"
    val patient = Faker.buildPatient()
    val composition =
      Composition().apply {
        identifier = Identifier().apply { value = appId }
        section =
          listOf(SectionComponent().apply { focus.reference = ResourceType.Questionnaire.name })
      }
    configRegistry.sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, appId)
    coEvery { fhirEngine.create(composition) } returns listOf(composition.id)
    coEvery { fhirEngine.search<Composition>(Search(composition.resourceType)) } returns
      listOf(composition)
    coEvery { fhirResourceDataSource.getResource(any()) } returns
      Bundle().apply { entry = listOf(Bundle.BundleEntryComponent().apply { resource = patient }) }
    coEvery { fhirEngine.get(patient.resourceType, patient.logicalId) } returns patient
    coEvery { fhirEngine.update(any()) } returns Unit

    runTest {
      configRegistry.fhirEngine.create(composition)
      configRegistry.fetchNonWorkflowConfigResources()
    }

    coVerify { fhirEngine.get(patient.resourceType, patient.logicalId) }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testFetchNonWorkflowConfigResourcesBundleListResource() {
    val appId = "theAppId"
    val focusReference = ResourceType.Questionnaire.name
    val resourceKey = "resourceKey"
    val resourceId = "resourceId"
    val listResource =
      ListResource().apply {
        entry =
          listOf(
            ListResource.ListEntryComponent().apply {
              item = Reference().apply { reference = "$resourceKey/$resourceId" }
            }
          )
      }
    val bundle =
      Bundle().apply {
        entry = listOf(Bundle.BundleEntryComponent().apply { resource = listResource })
      }

    val composition =
      Composition().apply {
        identifier = Identifier().apply { value = appId }
        section = listOf(SectionComponent().apply { focus.reference = focusReference })
      }
    configRegistry.sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, appId)
    coEvery { fhirEngine.create(composition) } returns listOf(composition.id)
    coEvery { fhirEngine.search<Composition>(Search(composition.resourceType)) } returns
      listOf(composition)
    coEvery { fhirResourceDataSource.getResource("$focusReference?_id=$focusReference") } returns
      bundle
    coEvery { fhirEngine.update(any()) } returns Unit
    coEvery { fhirEngine.get(ResourceType.List, "") } returns listResource
    coEvery { fhirResourceDataSource.getResource("$resourceKey?_id=$resourceId") } returns bundle

    runTest {
      configRegistry.fhirEngine.create(composition)
      configRegistry.fetchNonWorkflowConfigResources()
    }

    coVerify { fhirEngine.get(ResourceType.List, "") }
    coVerify { fhirResourceDataSource.getResource("$resourceKey?_id=$resourceId") }
  }
  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testAddOrUpdate() {
    // when does not exist
    val patient = Faker.buildPatient()
    coEvery { fhirEngine.get(patient.resourceType, patient.logicalId) } returns patient
    coEvery { fhirEngine.update(any()) } returns Unit

    runTest {
      val previousLastUpdate = patient.meta.lastUpdated
      configRegistry.addOrUpdate(patient)
      Assert.assertNotEquals(previousLastUpdate, patient.meta.lastUpdated)
    }

    // when exists
    runTest {
      val previousLastUpdate = patient.meta.lastUpdated
      configRegistry.addOrUpdate(patient)
      Assert.assertNotEquals(previousLastUpdate, patient.meta.lastUpdated)
    }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testAddOrUpdateCatchesResourceNotFound() {
    val patient = Faker.buildPatient()
    coEvery { fhirEngine.get(patient.resourceType, patient.logicalId) } throws
      ResourceNotFoundException("", "")
    coEvery { fhirEngine.create(any()) } returns listOf()

    runTest {
      val previousLastUpdate = patient.meta.lastUpdated
      configRegistry.addOrUpdate(patient)
      Assert.assertNotEquals(previousLastUpdate, patient.meta.lastUpdated)
    }

    coVerify(inverse = true) { fhirEngine.update(any()) }
    coVerify { fhirEngine.create(patient) }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testCreate() {
    val patient = Faker.buildPatient()
    coEvery { fhirEngine.create(patient) } returns listOf(patient.id)

    runTest {
      val result = configRegistry.create(patient)
      Assert.assertEquals(listOf(patient.id), result)
    }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testLoadConfigurationsNoLoadFromAssetsAppIdNotFound() {
    val appId = "the app id"
    coEvery { fhirEngine.search<Composition>(any<Search>()) } returns listOf()
    runTest { configRegistry.loadConfigurations(appId, context) }

    Assert.assertTrue(configRegistry.configsJsonMap.isNullOrEmpty())
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testLoadConfigurationsNoLoadFromAssetsAppIdFound() {
    val appId = "the app id"
    val composition = Composition().apply { section = listOf() }
    coEvery { fhirEngine.search<Composition>(any<Search>()) } returns listOf(composition)
    runTest { configRegistry.loadConfigurations(appId, context) }

    Assert.assertTrue(configRegistry.configsJsonMap.isNullOrEmpty())
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testLoadConfigurationsNoLoadFromAssetsSectionComponent() {
    val appId = "the app id"
    val composition = Composition().apply { section = listOf(SectionComponent()) }
    coEvery { fhirEngine.search<Composition>(any<Search>()) } returns listOf(composition)
    runTest { configRegistry.loadConfigurations(appId, context) }

    Assert.assertTrue(configRegistry.configsJsonMap.isNullOrEmpty())
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testLoadConfigurationsNoLoadFromAssetsSectionComponentWithSection() {
    val appId = "the app id"
    val composition =
      Composition().apply {
        section =
          listOf(
            SectionComponent().apply {
              id = "outer"
              section = listOf(SectionComponent().apply { id = "inner" })
              focus =
                Reference().apply {
                  identifier = Identifier().apply { id = "identifierId" }
                  reference = "referenceId"
                }
            }
          )
      }
    coEvery { fhirEngine.search<Composition>(any<Search>()) } returns listOf(composition)
    runTest { configRegistry.loadConfigurations(appId, context) }

    Assert.assertTrue(configRegistry.configsJsonMap.isNullOrEmpty())
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testLoadConfigurationsNoLoadFromAssetsAppConfig() {
    val appId = "the app id"
    val referenceId = "refernceId"
    val composition =
      Composition().apply {
        section =
          listOf(
            SectionComponent().apply {
              id = "outer"
              section = listOf(SectionComponent().apply { id = "inner" })
              focus =
                Reference().apply {
                  identifier =
                    Identifier().apply {
                      id = "theFocusId"
                      value = "value"
                    }
                  reference = "Binary/$referenceId"
                }
            }
          )
      }
    coEvery { fhirEngine.search<Composition>(any<Search>()) } returns listOf(composition)
    coEvery { fhirEngine.get(ResourceType.Binary, referenceId) } returns
      Binary().apply { content = ByteArray(0) }
    runTest { configRegistry.loadConfigurations(appId, context) }

    Assert.assertFalse(configRegistry.configsJsonMap.isNullOrEmpty())
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testLoadConfigurationsNoLoadFromAssetsIconConfig() {
    val appId = "the app id"
    val referenceId = "refernceId"
    val composition =
      Composition().apply {
        section =
          listOf(
            SectionComponent().apply {
              id = "outer"
              section = listOf(SectionComponent().apply { id = "inner" })
              focus =
                Reference().apply {
                  identifier =
                    Identifier().apply {
                      id = "theFocusId"
                      value = "${ConfigurationRegistry.ICON_PREFIX}more"
                    }
                  reference = "Binary/$referenceId"
                }
            }
          )
      }
    coEvery { fhirEngine.search<Composition>(any<Search>()) } returns listOf(composition)

    runTest { configRegistry.loadConfigurations(appId, context) }

    coVerify(inverse = true) { fhirEngine.get(ResourceType.Binary, referenceId) }
    Assert.assertTrue(configRegistry.configsJsonMap.isNullOrEmpty())
  }
}
