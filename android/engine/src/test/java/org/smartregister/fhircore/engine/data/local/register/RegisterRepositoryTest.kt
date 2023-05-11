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

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Search
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations.DataType
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
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

private const val PATIENT_REGISTER = "patientRegister"
private const val PATIENT_ID = "12345"
private const val HOUSEHOLD_REGISTER_ID = "householdRegister"
private const val THE_GROUP_ID = "theGroup"
private const val GROUP_MEMBERS = "groupMembers"
private const val ALL_TASKS = "allTasks"
private const val TASK_ID = "taskId"
private const val PART_OF_TASK_ID = "partOfTaskId"
private const val MEMBER_CARE_PLANS = "memberCarePlans"
private const val OBSERVATIONS_COUNT = "observationsCount"
private const val ENCOUNTERS_COUNT = "encountersCount"
private const val SUBJECT = "subject"
private const val PART_OF = "part-of"
private const val MEMBER = "member"
private const val SECONDARY_RESOURCE_CARE_PLAN_ID = "secondaryResourceCarePlanId"

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class RegisterRepositoryTest : RobolectricTest() {
  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val coroutineTestRule = CoroutineTestRule()
  @Inject lateinit var rulesFactory: RulesFactory
  private val fhirEngine: FhirEngine = mockk()
  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
  private val patient = Faker.buildPatient(PATIENT_ID)
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
          configService = mockk()
        )
      )

    // Simulate count for Encounter & Observation resources
    coEvery { fhirEngine.count(Search(type = ResourceType.Encounter)) } returns 2
    coEvery { fhirEngine.count(Search(type = ResourceType.Observation)) } returns 5
  }

  @Test
  fun countRegisterDataReturnsCorrectCount() {
    runTest {
      coEvery { fhirEngine.count(Search(type = ResourceType.Patient)) } returns 20
      val recordsCount = registerRepository.countRegisterData(PATIENT_REGISTER)
      Assert.assertEquals(20, recordsCount)
    }
  }

  @Test
  fun countRegisterDataReturnsCorrectCountForGroups() {
    runTest {
      coEvery { fhirEngine.count(Search(type = ResourceType.Group)) } returns 10
      val recordsCount = registerRepository.countRegisterData(HOUSEHOLD_REGISTER_ID)
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
      val recordsCount = registerRepository.countRegisterData(PATIENT_REGISTER, paramsMap)
      Assert.assertEquals(20, recordsCount)
    }
  }

  @Test
  fun testLoadRegisterDataWithForwardAndReverseIncludedResources() {
    runTest {
      val registerId = HOUSEHOLD_REGISTER_ID
      every { registerRepository.retrieveRegisterConfiguration(registerId, emptyMap()) } returns
        RegisterConfiguration(appId = "app", id = registerId, fhirResource = fhirResourceConfig())

      // Mock search for Groups; should return list of Group resources.
      val retrievedGroup = retrieveGroup()
      coEvery {
        fhirEngine.search<Resource>(Search(type = ResourceType.Group, count = 10, from = 0))
      } returns
        listOf(
          retrievedGroup,
          Group().apply {
            id = "inactiveGroup"
            active = false
          }
        )

      // Mock search for Group member (Patient) with forward include
      coEvery {
        fhirEngine.searchWithRevInclude<Resource>(false, Search(ResourceType.Group, null, null))
      } returns
        mutableMapOf(
          retrievedGroup to
            mapOf<ResourceType, List<Resource>>(ResourceType.Patient to listOf(patient))
        )

      // Mock searchWithRevInclude for CarePlan and Task related resources for the Patient
      val patientRelatedResources = retrieveRelatedResourcesMap()
      coEvery {
        fhirEngine.searchWithRevInclude<Resource>(true, Search(ResourceType.Patient, null, null))
      } returns mapOf(patient to patientRelatedResources)

      // Mock searchWithRevInclude for nested Task that is Part of another Task
      val task = patientRelatedResources.getValue(ResourceType.Task).first()
      coEvery {
        fhirEngine.searchWithRevInclude<Resource>(false, Search(ResourceType.Task, null, null))
      } returns
        mapOf(task to mapOf(ResourceType.Task to listOf(Task().apply { id = PART_OF_TASK_ID })))

      val registerData =
        registerRepository.loadRegisterData(currentPage = 0, registerId = registerId)

      Assert.assertTrue(registerData.isNotEmpty())
      val repositoryResourceData = registerData.firstOrNull()
      Assert.assertTrue(repositoryResourceData?.resource is Group)
      Assert.assertEquals(THE_GROUP_ID, repositoryResourceData?.resource?.id)
      Assert.assertTrue((repositoryResourceData?.resource as Group).member.isNotEmpty())

      // Ensure all the related resources (including nested ones) are available in the final map
      val relatedResources = repositoryResourceData.relatedResourcesMap
      Assert.assertTrue(relatedResources.isNotEmpty())

      // All group members added to the map
      Assert.assertTrue(relatedResources.containsKey(GROUP_MEMBERS))
      val firstGroupMember = relatedResources[GROUP_MEMBERS]?.firstOrNull()
      Assert.assertNotNull(firstGroupMember)
      Assert.assertTrue(firstGroupMember is Patient)
      Assert.assertEquals(patient.id, firstGroupMember?.id)

      // All Task resources grouped together (nested ones flattened) and added to the map
      Assert.assertTrue(relatedResources.containsKey(ALL_TASKS))
      Assert.assertEquals(2, relatedResources[ALL_TASKS]?.size)

      val firstTask = relatedResources[ALL_TASKS]?.firstOrNull()
      Assert.assertNotNull(firstTask)
      Assert.assertTrue(firstTask is Task)
      Assert.assertEquals(TASK_ID, firstTask?.id)
      val lastTask = relatedResources[ALL_TASKS]?.lastOrNull()
      Assert.assertNotNull(lastTask)
      Assert.assertTrue(lastTask is Task)
      Assert.assertEquals(PART_OF_TASK_ID, lastTask?.id)

      // All CarePlan resources grouped together (nested ones flattened) and added to the map
      Assert.assertTrue(relatedResources.containsKey(MEMBER_CARE_PLANS))
      val firstMemberCarePlan = relatedResources[MEMBER_CARE_PLANS]?.firstOrNull()
      Assert.assertNotNull(firstMemberCarePlan)
      Assert.assertTrue(firstMemberCarePlan is CarePlan)
      Assert.assertEquals("carePlan", firstMemberCarePlan?.id)

      // Assert Observation and Encounter resource counts
      assertRepositoryResourceDataContainsCounts(repositoryResourceData)
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
                baseResource = ResourceConfig(resource = ResourceType.CarePlan),
                relatedResources =
                  listOf(
                    ResourceConfig(
                      resource = ResourceType.Encounter,
                      searchParameter = "encounter",
                      isRevInclude = false
                    )
                  )
              )
            )
        )

      // Mock search for Groups; should return list of Group resources
      val group = retrieveGroup()
      coEvery { fhirEngine.get(type = ResourceType.Group, id = group.id) } returns group

      // Mock search for Group member (Patient) with forward include
      coEvery {
        fhirEngine.searchWithRevInclude<Resource>(false, Search(ResourceType.Group, null, null))
      } returns
        mutableMapOf(
          group to mapOf<ResourceType, List<Resource>>(ResourceType.Patient to listOf(patient))
        )

      // Mock searchWithRevInclude for CarePlan and Task related resources for the Patient
      val patientRelatedResources: Map<ResourceType, List<Resource>> = retrieveRelatedResourcesMap()
      coEvery {
        fhirEngine.searchWithRevInclude<Resource>(true, Search(ResourceType.Patient, null, null))
      } returns mutableMapOf(patient to patientRelatedResources)

      // Mock searchWithRevInclude for nested Task that is Part of another Task
      val task = patientRelatedResources.getValue(ResourceType.Task).first()
      coEvery {
        fhirEngine.searchWithRevInclude<Resource>(false, Search(ResourceType.Task, null, null))
      } returns
        mutableMapOf(
          task to mapOf(ResourceType.Task to listOf(Task().apply { id = PART_OF_TASK_ID }))
        )

      // Mock search for secondary resources
      val encounterId = "encounter123"
      val carePlan =
        CarePlan().apply {
          id = SECONDARY_RESOURCE_CARE_PLAN_ID
          encounter = Reference("${ResourceType.Encounter.name}/$encounterId")
        }

      coEvery {
        fhirEngine.search<Resource>(Search(type = ResourceType.CarePlan, count = null, from = null))
      } returns listOf(carePlan)

      // Mock search for secondary resource member (Encounter) with forward include
      coEvery {
        fhirEngine.searchWithRevInclude<Resource>(false, Search(ResourceType.CarePlan, null, null))
      } returns
        mutableMapOf(
          carePlan to
            mapOf<ResourceType, List<Resource>>(
              ResourceType.Encounter to listOf(Encounter().apply { id = encounterId })
            )
        )

      val repositoryResourceData =
        registerRepository.loadProfileData(
          profileId = profileId,
          resourceId = group.id,
          fhirResourceConfig = null,
          paramsList = null
        )
      Assert.assertTrue(repositoryResourceData.resource is Group)
      Assert.assertEquals(THE_GROUP_ID, repositoryResourceData.resource.id)
      Assert.assertTrue((repositoryResourceData.resource as Group).member.isNotEmpty())

      // Ensure the related resources were included
      val relatedResources = repositoryResourceData.relatedResourcesMap
      Assert.assertTrue(relatedResources.isNotEmpty())
      Assert.assertTrue(relatedResources.containsKey(GROUP_MEMBERS))
      Assert.assertTrue(relatedResources.containsKey(ALL_TASKS))
      Assert.assertTrue(relatedResources.containsKey(MEMBER_CARE_PLANS))

      // Assert that secondary resources are loaded
      val secondaryRepositoryResourceDataList =
        repositoryResourceData.secondaryRepositoryResourceData
      Assert.assertNotNull(secondaryRepositoryResourceDataList)
      Assert.assertTrue(secondaryRepositoryResourceDataList!!.isNotEmpty())
      val secondaryRepositoryResourceData: RepositoryResourceData? =
        secondaryRepositoryResourceDataList.firstOrNull()
      Assert.assertTrue(secondaryRepositoryResourceData?.resource is CarePlan)
      Assert.assertEquals(
        SECONDARY_RESOURCE_CARE_PLAN_ID,
        secondaryRepositoryResourceData?.resource?.id
      )
      Assert.assertFalse(secondaryRepositoryResourceData?.relatedResourcesMap.isNullOrEmpty())
      Assert.assertTrue(
        secondaryRepositoryResourceData?.relatedResourcesMap?.containsKey(
          ResourceType.Encounter.name
        )!!
      )

      // Assert Observation and Encounter resource counts
      assertRepositoryResourceDataContainsCounts(repositoryResourceData)
    }
  }

  private fun assertRepositoryResourceDataContainsCounts(
    repositoryResourceData: RepositoryResourceData
  ) {
    val relatedResourceCountMap = repositoryResourceData.relatedResourcesCountMap
    Assert.assertEquals(2, relatedResourceCountMap.size)

    // Encounter resources counted
    val encounterRepositoryResourceCounts = relatedResourceCountMap[ENCOUNTERS_COUNT]
    val encounterRepositoryResourceCount = encounterRepositoryResourceCounts?.firstOrNull()
    Assert.assertNotNull(encounterRepositoryResourceCount)
    Assert.assertEquals(
      ResourceType.Encounter,
      encounterRepositoryResourceCount?.relatedResourceType
    )
    Assert.assertEquals(patient.id, encounterRepositoryResourceCount?.parentResourceId)
    Assert.assertEquals(2L, encounterRepositoryResourceCount?.count)

    // Observation resources counted
    val observationRelatedResourceCounts = relatedResourceCountMap[OBSERVATIONS_COUNT]
    val observationRelatedResourceCount = observationRelatedResourceCounts?.firstOrNull()
    Assert.assertNotNull(observationRelatedResourceCount)
    Assert.assertEquals(
      ResourceType.Observation,
      observationRelatedResourceCount?.relatedResourceType
    )
    Assert.assertEquals(patient.id, observationRelatedResourceCount?.parentResourceId)
    Assert.assertEquals(5L, observationRelatedResourceCount?.count)
  }

  private fun fhirResourceConfig() =
    FhirResourceConfig(
      baseResource = ResourceConfig(resource = ResourceType.Group),
      relatedResources =
        listOf(
          ResourceConfig(
            resource = ResourceType.Patient,
            id = GROUP_MEMBERS,
            searchParameter = MEMBER,
            isRevInclude = false,
            relatedResources =
              listOf(
                ResourceConfig(
                  id = ENCOUNTERS_COUNT,
                  resource = ResourceType.Encounter,
                  searchParameter = SUBJECT,
                  resultAsCount = true
                ),
                ResourceConfig(
                  id = OBSERVATIONS_COUNT,
                  resource = ResourceType.Observation,
                  searchParameter = SUBJECT,
                  resultAsCount = true
                ),
                ResourceConfig(
                  id = ALL_TASKS,
                  resource = ResourceType.Task,
                  searchParameter = SUBJECT,
                  relatedResources =
                    listOf(
                      ResourceConfig(
                        id = ALL_TASKS, // Referenced task
                        resource = ResourceType.Task,
                        searchParameter = PART_OF,
                        isRevInclude = false
                      ),
                    )
                ),
                ResourceConfig(
                  id = MEMBER_CARE_PLANS,
                  resource = ResourceType.CarePlan,
                  searchParameter = SUBJECT
                )
              )
          )
        )
    )

  private fun retrieveRelatedResourcesMap(): Map<ResourceType, List<Resource>> =
    mapOf(
      ResourceType.Task to listOf(retrieveTask()),
      ResourceType.CarePlan to listOf(CarePlan().apply { id = "carePlan" })
    )

  private fun retrieveTask() =
    Task().apply {
      id = TASK_ID
      partOf = listOf(Reference("${ResourceType.Task.name}/$PART_OF_TASK_ID"))
    }

  private fun retrieveGroup() =
    Group().apply {
      id = THE_GROUP_ID
      active = true
      addMember(Group.GroupMemberComponent(Reference("Patient/12345")))
    }
}
