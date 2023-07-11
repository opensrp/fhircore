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
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Search
import com.google.gson.GsonBuilder
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Composition.SectionComponent
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.AppConfigService
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry.Companion.MANIFEST_PROCESSOR_BATCH_SIZE
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
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
  private val fhirResourceService = mockk<FhirResourceService>()

  @Before
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun setUp() {
    hiltRule.inject()
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
        Faker.json,
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
          listOf(SectionComponent().apply { focus.reference = ResourceType.Questionnaire.name })
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

  // TODO TO DO Remove after migration to PROXY for all projects: Duplication for Backward
  // Compatibility for NON-PROXY version
  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testFetchNonWorkflowConfigResourcesBundleListResourceProxyBackwardCompatible() {
    val appId = "theAppId"
    val focusReference = ResourceType.Questionnaire.name
    val resourceKey = "resourceKey"
    val resourceId = "resourceId"
    val testListId = "test-list-id"
    val listResource =
      ListResource().apply {
        id = "test-list-id"
        entry =
          listOf(
            ListResource.ListEntryComponent().apply {
              item = Reference().apply { reference = "$resourceKey/$resourceId" }
            },
          )
      }
    val bundle =
      Bundle().apply {
        entry = listOf(Bundle.BundleEntryComponent().apply { resource = listResource })
      }

    val composition =
      Composition().apply {
        identifier = Identifier().apply { value = appId }
        section =
          listOf(
            SectionComponent().apply { focus.reference = "${ResourceType.List.name}/$testListId" },
          )
      }
    configRegistry.sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, appId)
    coEvery { fhirEngine.create(composition) } returns listOf(composition.id)
    coEvery { fhirEngine.search<Composition>(Search(composition.resourceType)) } returns
      listOf(composition)
    coEvery { fhirResourceDataSource.getResource("$focusReference?_id=$focusReference") } returns
      bundle
    coEvery { fhirEngine.update(any()) } returns Unit
    coEvery { fhirEngine.get(ResourceType.List, testListId) } returns listResource
    coEvery { fhirResourceDataSource.getResource("$resourceKey?_id=$resourceId") } returns bundle
    coEvery { fhirResourceService.getResource("List?_id=$testListId") } returns bundle

    runTest {
      configRegistry.fhirEngine.create(composition)
      configRegistry.setNonProxy(true)
      configRegistry.fetchNonWorkflowConfigResources()
    }

    coVerify { fhirEngine.get(ResourceType.List, testListId) }
    coVerify { fhirResourceDataSource.getResource("$resourceKey?_id=$resourceId") }
    coEvery { fhirResourceDataSource.getResource("$focusReference?_id=$focusReference") }
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
            },
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
    coEvery { fhirEngine.createRemote(any()) } just runs

    runTest {
      val previousLastUpdate = patient.meta.lastUpdated
      configRegistry.addOrUpdate(patient)
      Assert.assertNotEquals(previousLastUpdate, patient.meta.lastUpdated)
    }

    coVerify(inverse = true) { fhirEngine.update(any()) }
    coVerify { fhirEngine.createRemote(patient) }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testCreate() {
    val patient = Faker.buildPatient()
    coEvery { fhirEngine.createRemote(patient) } just runs

    runTest {
      configRegistry.create(patient)
      coVerify { fhirEngine.createRemote(patient) }
    }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testLoadConfigurationsNoLoadFromAssetsAppIdNotFound() {
    val appId = "the app id"
    coEvery { fhirEngine.search<Composition>(any<Search>()) } returns listOf()
    runTest { configRegistry.loadConfigurations(appId, context) }

    Assert.assertTrue(configRegistry.configsJsonMap.isEmpty())
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testLoadConfigurationsNoLoadFromAssetsAppIdFound() {
    val appId = "the app id"
    val composition = Composition().apply { section = listOf() }
    coEvery { fhirEngine.search<Composition>(any<Search>()) } returns listOf(composition)
    runTest { configRegistry.loadConfigurations(appId, context) }

    Assert.assertTrue(configRegistry.configsJsonMap.isEmpty())
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testLoadConfigurationsNoLoadFromAssetsSectionComponent() {
    val appId = "the app id"
    val composition = Composition().apply { section = listOf(SectionComponent()) }
    coEvery { fhirEngine.search<Composition>(any<Search>()) } returns listOf(composition)
    runTest { configRegistry.loadConfigurations(appId, context) }

    Assert.assertTrue(configRegistry.configsJsonMap.isEmpty())
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
            },
          )
      }
    coEvery { fhirEngine.search<Composition>(any<Search>()) } returns listOf(composition)
    runTest { configRegistry.loadConfigurations(appId, context) }

    Assert.assertTrue(configRegistry.configsJsonMap.isEmpty())
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testLoadConfigurationsNoLoadFromAssetsAppConfig() {
    val appId = "the app id"
    val referenceId = "referenceId"
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
            },
          )
      }
    coEvery { fhirEngine.search<Composition>(any<Search>()) } returns listOf(composition)
    coEvery { fhirEngine.get(ResourceType.Binary, referenceId) } returns
      Binary().apply { content = ByteArray(0) }
    runTest { configRegistry.loadConfigurations(appId, context) }

    Assert.assertFalse(configRegistry.configsJsonMap.isEmpty())
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testLoadConfigurationsNoLoadFromAssetsIconConfig() {
    val appId = "the app id"
    val referenceId = "referenceId"
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
            },
          )
      }
    coEvery { fhirEngine.search<Composition>(any<Search>()) } returns listOf(composition)

    runTest { configRegistry.loadConfigurations(appId, context) }

    coVerify(inverse = true) { fhirEngine.get(ResourceType.Binary, referenceId) }
    Assert.assertTrue(configRegistry.configsJsonMap.isEmpty())
  }

  @Test
  fun testRetrieveConfigurationUpdatesTheConfigCacheMap() {
    configRegistry.configsJsonMap[ConfigType.Application.name] =
      """{"appId": "thisApp", "configType": "application"}"""

    // First time reading the configCacheMap not yet populated
    Assert.assertFalse(configRegistry.configCacheMap.containsKey(ConfigType.Application.name))
    val applicationConfiguration =
      configRegistry.retrieveConfiguration<ApplicationConfiguration>(
        configType = ConfigType.Application,
      )

    Assert.assertNotNull(applicationConfiguration)
    Assert.assertEquals("thisApp", applicationConfiguration.appId)
    Assert.assertNotNull(ConfigType.Application.name, applicationConfiguration.configType)
    // Config cache map now contains application config
    Assert.assertTrue(configRegistry.configCacheMap.containsKey(ConfigType.Application.name))

    val anotherApplicationConfig =
      configRegistry.retrieveConfiguration<ApplicationConfiguration>(
        configType = ConfigType.Application,
      )
    Assert.assertTrue(configRegistry.configCacheMap.containsKey(ConfigType.Application.name))
    Assert.assertNotNull(anotherApplicationConfig)
    Assert.assertEquals("thisApp", anotherApplicationConfig.appId)
    Assert.assertNotNull(ConfigType.Application.name, anotherApplicationConfig.configType)
  }

  @Test
  fun testRetrieveConfigurationWithParamsCachesTheSecondTime() {
    configRegistry.configsJsonMap[ConfigType.Application.name] =
      """{"appId": "thisApp", "configType": "application"}"""
    val paramsList =
      arrayListOf(
        ActionParameter(
          key = "paramsName",
          paramType = ActionParameterType.PARAMDATA,
          value = "testing1",
          dataType = Enumerations.DataType.STRING,
          linkId = null,
        ),
        ActionParameter(
          key = "paramName2",
          paramType = ActionParameterType.PARAMDATA,
          value = "testing2",
          dataType = Enumerations.DataType.STRING,
          linkId = null,
        ),
        ActionParameter(
          key = "paramName3",
          paramType = ActionParameterType.PREPOPULATE,
          value = "testing3",
          dataType = Enumerations.DataType.STRING,
          linkId = null,
        ),
      )
    val paramsMap =
      paramsList
        .asSequence()
        .filter { it.paramType == ActionParameterType.PARAMDATA && !it.value.isNullOrEmpty() }
        .associate { it.key to it.value }

    // First time reading the configCacheMap not yet populated
    Assert.assertFalse(configRegistry.configCacheMap.containsKey(ConfigType.Application.name))
    val applicationConfiguration =
      configRegistry.retrieveConfiguration<ApplicationConfiguration>(
        configType = ConfigType.Application,
        paramsMap = paramsMap,
      )

    Assert.assertNotNull(applicationConfiguration)
    Assert.assertEquals("thisApp", applicationConfiguration.appId)
    Assert.assertNotNull(ConfigType.Application.name, applicationConfiguration.configType)
    // Config cache map now contains application config

    val anotherApplicationConfig =
      configRegistry.retrieveConfiguration<ApplicationConfiguration>(
        configType = ConfigType.Application,
      )
    Assert.assertNotNull(anotherApplicationConfig)
    Assert.assertTrue(configRegistry.configCacheMap.containsKey(ConfigType.Application.name))
  }

  @Test
  fun testFetchNonWorkflowConfigResourcesProcessesManifestEntriesInChunks() {
    val appId = "theAppId"
    val compositionSections = mutableListOf<SectionComponent>()

    for (i in 1..MANIFEST_PROCESSOR_BATCH_SIZE + 1) { // We need more than the MAX batch size
      compositionSections.add(
        SectionComponent().apply { focus.reference = "${ResourceType.StructureMap.name}/id-$i" },
      )
    }

    Assert.assertEquals(31, compositionSections.size)

    val composition =
      Composition().apply {
        identifier = Identifier().apply { value = appId }
        section = compositionSections
      }
    configRegistry.sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, appId)

    coEvery { fhirEngine.search<Composition>(Search(composition.resourceType)) } returns
      listOf(composition)
    coEvery { fhirResourceDataSource.getResource(any()) } returns Bundle()

    runTest { configRegistry.fetchNonWorkflowConfigResources() }

    val requestPathArgumentSlot = mutableListOf<String>()

    coVerify(exactly = 2) { fhirResourceDataSource.getResource(capture(requestPathArgumentSlot)) }

    Assert.assertEquals(2, requestPathArgumentSlot.size)
    Assert.assertEquals(
      "StructureMap?_id=id-1,id-2,id-3,id-4,id-5,id-6,id-7,id-8,id-9,id-10,id-11,id-12,id-13,id-14,id-15,id-16,id-17,id-18,id-19,id-20,id-21,id-22,id-23,id-24,id-25,id-26,id-27,id-28,id-29,id-30",
      requestPathArgumentSlot.first(),
    )
    Assert.assertEquals("StructureMap?_id=id-31", requestPathArgumentSlot.last())
  }

  @Test
  fun testFetchNonWorkflowConfigListResourcesPersistsActualListEntryResources() {
    val appId = "theAppId"
    val compositionSections = mutableListOf<SectionComponent>()
    compositionSections.add(
      SectionComponent().apply { focus.reference = "${ResourceType.List.name}/46464" },
    )

    val iParser: IParser = FhirContext.forR4Cached().newJsonParser()
    val listJson =
      context.assets.open("sample_commodities_list_bundle.json").bufferedReader().use {
        it.readText()
      }
    val listResource = iParser.parseResource(listJson) as Bundle

    val composition =
      Composition().apply {
        identifier = Identifier().apply { value = appId }
        section = compositionSections
      }
    configRegistry.sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, appId)

    coEvery { fhirEngine.search<Composition>(Search(composition.resourceType)) } returns
      listOf(composition)

    coEvery {
      fhirResourceDataSource.getResourceWithGatewayModeHeader("list-entries", "List/46464")
    } returns Bundle().apply { entry = listOf(BundleEntryComponent().setResource(listResource)) }

    coEvery { fhirEngine.get(any(), any()) } throws
      ResourceNotFoundException(ResourceType.Group.name, "some-id")

    coEvery { fhirEngine.createRemote(any()) } just runs

    runTest { configRegistry.fetchNonWorkflowConfigResources() }

    val requestPathArgumentSlot = mutableListOf<Group>()

    coVerify(exactly = 2) { fhirEngine.createRemote(capture(requestPathArgumentSlot)) }

    Assert.assertEquals(2, requestPathArgumentSlot.size)

    Assert.assertEquals("Group/1000001", requestPathArgumentSlot.first().id)
    Assert.assertEquals(ResourceType.Group, requestPathArgumentSlot.first().resourceType)

    Assert.assertEquals("Group/2000001", requestPathArgumentSlot.last().id)
    Assert.assertEquals(ResourceType.Group, requestPathArgumentSlot.last().resourceType)
  }

  @Test
  fun testFetchNonWorkflowConfigListResourcesNestedBundlePersistsActualListEntryResources() {
    val appId = "theAppId"
    val compositionSections = mutableListOf<SectionComponent>()
    compositionSections.add(
      SectionComponent().apply { focus.reference = "${ResourceType.List.name}/46464" },
    )

    val iParser: IParser = FhirContext.forR4Cached().newJsonParser()
    val listJson =
      context.assets.open("sample_commodities_list_bundle.json").bufferedReader().use {
        it.readText()
      }
    val listResource = iParser.parseResource(listJson) as Bundle

    val composition =
      Composition().apply {
        identifier = Identifier().apply { value = appId }
        section = compositionSections
      }
    configRegistry.sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, appId)

    coEvery { fhirEngine.search<Composition>(Search(composition.resourceType)) } returns
      listOf(composition)

    coEvery {
      fhirResourceDataSource.getResourceWithGatewayModeHeader("list-entries", "List/46464")
    } returns
      Bundle().apply {
        entry =
          listOf(
            BundleEntryComponent()
              .setResource(
                Bundle().apply { entry = listOf(BundleEntryComponent().setResource(listResource)) },
              ),
          )
      }

    coEvery { fhirEngine.get(any(), any()) } throws
      ResourceNotFoundException(ResourceType.Group.name, "some-id-not-found")

    coEvery { fhirEngine.createRemote(any()) } just runs

    runTest { configRegistry.fetchNonWorkflowConfigResources() }

    val requestPathArgumentSlot = mutableListOf<Resource>()

    coVerify(exactly = 3) { fhirEngine.createRemote(capture(requestPathArgumentSlot)) }

    Assert.assertEquals(3, requestPathArgumentSlot.size)

    Assert.assertEquals("Bundle/the-commodities-bundle-id", requestPathArgumentSlot[0].id)
    Assert.assertEquals(ResourceType.Bundle, requestPathArgumentSlot[0].resourceType)

    Assert.assertEquals("Group/1000001", requestPathArgumentSlot[1].id)
    Assert.assertEquals(ResourceType.Group, requestPathArgumentSlot[1].resourceType)

    Assert.assertEquals("Group/2000001", requestPathArgumentSlot.last().id)
    Assert.assertEquals(ResourceType.Group, requestPathArgumentSlot.last().resourceType)
  }
}
