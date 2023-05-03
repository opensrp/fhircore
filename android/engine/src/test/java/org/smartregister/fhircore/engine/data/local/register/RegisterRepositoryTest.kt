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

package org.smartregister.fhircore.engine.data.local.register

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Search
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Enumerations.DataType
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Specimen
import org.hl7.fhir.r4.model.Task
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.rulesengine.RulesFactory
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor

@HiltAndroidTest
class RegisterRepositoryTest : RobolectricTest() {
  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val coroutineTestRule = CoroutineTestRule()
  @Inject lateinit var rulesFactory: RulesFactory
  @Inject lateinit var fhirPathDataExtractor: FhirPathDataExtractor
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val fhirEngine: FhirEngine = mockk()
  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
  private val patient = Faker.buildPatient("12345")
  private lateinit var registerRepository: RegisterRepository

  @Before
  fun setUp() {
    hiltRule.inject()
    registerRepository =
      spyk(
        RegisterRepository(
          fhirEngine = fhirEngine,
          dispatcherProvider = coroutineTestRule.testDispatcherProvider,
          sharedPreferencesHelper = mockk(),
          configurationRegistry = configurationRegistry,
          configService = mockk(),
          fhirPathDataExtractor = fhirPathDataExtractor
        )
      )
    coEvery { fhirEngine.search<Immunization>(Search(type = ResourceType.Immunization)) } returns
      listOf(Immunization())
  }

  @Test
  fun countRegisterDataReturnsCorrectCount() {
    runTest {
      coEvery { fhirEngine.count(Search(type = ResourceType.Patient)) } returns 20
      val recordsCount = registerRepository.countRegisterData("patientRegister")
      Assert.assertEquals(20, recordsCount)
    }
  }

  @Test
  fun countRegisterDataReturnsCorrectCountForGroups() {
    runTest {
      coEvery { fhirEngine.count(Search(type = ResourceType.Group)) } returns 10
      val recordsCount = registerRepository.countRegisterData("householdRegister")
      Assert.assertEquals(10, recordsCount)
    }
  }

  @Test
  fun countRegisterDataWithParams() {
    runTest {
      val paramsList =
        arrayListOf(
          ActionParameter(
            key = "paramsName",
            paramType = ActionParameterType.PARAMDATA,
            value = "testing1",
            dataType = DataType.STRING,
            linkId = null
          ),
          ActionParameter(
            key = "paramName2",
            paramType = ActionParameterType.PARAMDATA,
            value = "testing2",
            dataType = DataType.STRING,
            linkId = null
          ),
        )
      paramsList
        .asSequence()
        .filter { it.paramType == ActionParameterType.PARAMDATA && it.value.isNotEmpty() }
        .associate { it.key to it.value }
      val paramsMap = emptyMap<String, String>()
      coEvery { fhirEngine.count(Search(type = ResourceType.Patient)) } returns 20
      val recordsCount = registerRepository.countRegisterData("patientRegister", paramsMap)
      Assert.assertEquals(20, recordsCount)
    }
  }

  @Test
  fun testLoadRegisterDataWithForwardAndReverseIncludedResources() {
    runTest {
      val registerId = "householdRegister"
      every { registerRepository.retrieveRegisterConfiguration(registerId, emptyMap()) } returns
        RegisterConfiguration(appId = "app", id = registerId, fhirResource = fhirResourceConfig())

      // Mock search for Groups; should return list of Group resources.
      coEvery {
        fhirEngine.search<Resource>(Search(type = ResourceType.Group, count = 10, from = 0))
      } returns
        listOf(
          retrieveGroup(),
          Group().apply {
            id = "inactiveGroup"
            active = false
          }
        )

      // Mock search for Group member (Patient) with forward include
      val patientId = "12345"
      coEvery { fhirEngine.get(type = ResourceType.Patient, id = patientId) } returns patient

      // Mock searchWithRevInclude for CarePlan and Task related resources for the Patient
      coEvery {
        fhirEngine.searchWithRevInclude<Resource>(Search(ResourceType.Patient, null, null))
      } returns mutableMapOf(patient to retrieveRelatedResourcesMap())

      val registerData =
        registerRepository.loadRegisterData(
          currentPage = 0,
          registerId = registerId,
          mutableMapOf()
        )
      Assert.assertTrue(registerData.isNotEmpty())
      val repositoryResourceData = registerData.firstOrNull()
      Assert.assertTrue(repositoryResourceData is RepositoryResourceData.Search)
      Assert.assertTrue((repositoryResourceData as RepositoryResourceData.Search).resource is Group)
      Assert.assertEquals("theGroup", (repositoryResourceData.resource as Group).id)
      Assert.assertTrue((repositoryResourceData.resource as Group).member.isNotEmpty())

      // Ensure the related resources were included
      val relatedResources = repositoryResourceData.relatedResources
      Assert.assertTrue(relatedResources.isNotEmpty())
      Assert.assertTrue(relatedResources.containsKey("groupMembers"))

      val patientRepositoryResourceData: RepositoryResourceData.Search? =
        relatedResources["groupMembers"]?.firstOrNull() as RepositoryResourceData.Search?
      Assert.assertNotNull(patientRepositoryResourceData)
      val patientRelatedResources = patientRepositoryResourceData?.relatedResources
      Assert.assertTrue(patientRelatedResources?.containsKey("memberTasks")!!)
      Assert.assertTrue(patientRelatedResources["memberTasks"]!!.isNotEmpty())
      Assert.assertTrue(patientRelatedResources.containsKey("memberCarePlans"))
      Assert.assertTrue(patientRelatedResources["memberCarePlans"]!!.isNotEmpty())
    }
  }

  @Test
  fun testLoadProfileDataWithForwardAndReverseIncludedResources() {
    runTest {
      val profileId = "profile"
      every { registerRepository.retrieveProfileConfiguration(profileId, emptyMap()) } returns
        ProfileConfiguration(
          appId = "app",
          id = profileId,
          fhirResource = fhirResourceConfig(),
          // Load extra resources not related to the baseResource
          secondaryResources =
            listOf(
              FhirResourceConfig(
                baseResource = ResourceConfig(resource = "Group"),
                relatedResources =
                  listOf(
                    ResourceConfig(
                      resource = "Specimen",
                      fhirPathExpression = "Group.member.entity"
                    )
                  )
              )
            )
        )

      // Mock search for Groups; should return list of Group resources
      val group = retrieveGroup()
      coEvery { fhirEngine.get(type = ResourceType.Group, id = group.id) } returns group

      // Mock search for Group member (Patient) with forward include
      val patientId = "12345"
      coEvery { fhirEngine.get(type = ResourceType.Patient, id = patientId) } returns patient

      // Mock searchWithRevInclude for CarePlan and Task related resources for the Patient
      coEvery {
        fhirEngine.searchWithRevInclude<Resource>(Search(ResourceType.Patient, null, null))
      } returns mutableMapOf(patient to retrieveRelatedResourcesMap())

      // Mock search for secondary resources
      val specimenId = "specimen123"
      coEvery {
        fhirEngine.search<Resource>(Search(type = ResourceType.Group, count = null, from = null))
      } returns
        listOf(
          Group().apply {
            id = "secondaryResourceGroup"
            active = true
            addMember(Group.GroupMemberComponent(Reference(specimenId)))
          }
        )

      // Mock search for secondary resource member (Specimen) with forward include
      coEvery { fhirEngine.get(type = ResourceType.Specimen, id = specimenId) } returns
        Specimen().apply { id = specimenId }

      val repositoryResourceData =
        registerRepository.loadProfileData(
          profileId = profileId,
          resourceId = group.id,
          fhirResourceConfig = null,
          paramsList = null
        )
      Assert.assertTrue(repositoryResourceData is RepositoryResourceData.Search)
      Assert.assertTrue((repositoryResourceData as RepositoryResourceData.Search).resource is Group)
      Assert.assertEquals("theGroup", (repositoryResourceData.resource as Group).id)
      Assert.assertTrue((repositoryResourceData.resource as Group).member.isNotEmpty())

      // Ensure the related resources were included
      val relatedResources = repositoryResourceData.relatedResources
      Assert.assertTrue(relatedResources.isNotEmpty())
      Assert.assertTrue(relatedResources.containsKey("groupMembers"))

      val patientRepositoryResourceData: RepositoryResourceData.Search? =
        relatedResources["groupMembers"]?.firstOrNull() as RepositoryResourceData.Search?
      Assert.assertNotNull(patientRepositoryResourceData)
      val patientRelatedResources = patientRepositoryResourceData?.relatedResources
      Assert.assertTrue(patientRelatedResources?.containsKey("memberTasks")!!)
      Assert.assertTrue(patientRelatedResources["memberTasks"]!!.isNotEmpty())
      Assert.assertTrue(patientRelatedResources.containsKey("memberCarePlans"))
      Assert.assertTrue(patientRelatedResources["memberCarePlans"]!!.isNotEmpty())

      // Assert that secondary resources are loaded
      val secondaryRepositoryResourceDataList =
        repositoryResourceData.secondaryRepositoryResourceData
      Assert.assertNotNull(secondaryRepositoryResourceDataList)
      Assert.assertTrue(secondaryRepositoryResourceDataList!!.isNotEmpty())
      val secondaryRepositoryResourceData: RepositoryResourceData =
        secondaryRepositoryResourceDataList[0]
      Assert.assertTrue(secondaryRepositoryResourceData is RepositoryResourceData.Search)
      Assert.assertTrue(
        (secondaryRepositoryResourceData as RepositoryResourceData.Search).resource is Group
      )
      Assert.assertEquals("secondaryResourceGroup", secondaryRepositoryResourceData.resource.id)
      Assert.assertTrue(secondaryRepositoryResourceData.relatedResources.isNotEmpty())
      Assert.assertTrue(secondaryRepositoryResourceData.relatedResources.containsKey("Specimen"))
    }
  }

  private fun fhirResourceConfig() =
    FhirResourceConfig(
      baseResource = ResourceConfig(resource = "Group"),
      relatedResources =
        listOf(
          ResourceConfig(
            resource = "Patient",
            id = "groupMembers",
            fhirPathExpression = "Group.member.entity",
            relatedResources =
              listOf(
                ResourceConfig(id = "memberTasks", resource = "Task", searchParameter = "subject"),
                ResourceConfig(
                  id = "memberCarePlans",
                  resource = "CarePlan",
                  searchParameter = "subject"
                )
              )
          )
        )
    )

  private fun retrieveRelatedResourcesMap(): Map<ResourceType, List<Resource>> =
    mapOf(
      ResourceType.Task to listOf(Task().apply { id = "taskId" }),
      ResourceType.CarePlan to listOf(CarePlan().apply { id = "carePlan" })
    )

  private fun retrieveGroup() =
    Group().apply {
      id = "theGroup"
      active = true
      addMember(Group.GroupMemberComponent(Reference("Patient/12345")))
    }
}
