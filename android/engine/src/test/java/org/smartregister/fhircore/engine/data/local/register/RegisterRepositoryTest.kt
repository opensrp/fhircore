/*
 * Copyright 2021-2024 Ona Systems, Inc
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

import android.app.Application
import androidx.compose.ui.state.ToggleableState
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.search.Search
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations.DataType
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.data.local.ContentCache
import org.smartregister.fhircore.engine.datastore.syncLocationIdsProtoStore
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.CountResultConfig
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.domain.model.SyncLocationState
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.rulesengine.RulesFactory
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.encodeJson
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor

private const val PATIENT_REGISTER = "patientRegister"
private const val PATIENT_ID = "12345"
private const val HOUSEHOLD_REGISTER_ID = "householdRegister"
private const val GROUP_ID = "theGroup"
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

  @Inject lateinit var fhirPathDataExtractor: FhirPathDataExtractor

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  @Inject lateinit var fhirEngine: FhirEngine

  @Inject lateinit var parser: IParser

  @Inject lateinit var contentCache: ContentCache

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
          dispatcherProvider = dispatcherProvider,
          sharedPreferencesHelper = mockk(),
          configurationRegistry = configurationRegistry,
          configService = mockk(),
          configRulesExecutor = mockk(),
          fhirPathDataExtractor = fhirPathDataExtractor,
          parser = parser,
          context = ApplicationProvider.getApplicationContext(),
          contentCache = contentCache,
        ),
      )
  }

  @Test
  fun countRegisterDataReturnsCorrectCount() {
    val search = slot<Search>()
    runTest {
      coEvery { fhirEngine.count(capture(search)) } returns 20
      val recordsCount = registerRepository.countRegisterData(PATIENT_REGISTER)
      Assert.assertEquals(ResourceType.Patient, search.captured.type)
      Assert.assertEquals(20, recordsCount)
    }
  }

  @Test
  fun countRegisterDataReturnsCorrectCountForGroups() {
    val searchSlot = slot<Search>()
    runTest {
      coEvery { fhirEngine.count(capture(searchSlot)) } returns 10
      val recordsCount = registerRepository.countRegisterData(HOUSEHOLD_REGISTER_ID)
      Assert.assertEquals(ResourceType.Group, searchSlot.captured.type)
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
            linkId = null,
          ),
          ActionParameter(
            key = "paramName2",
            paramType = ActionParameterType.PARAMDATA,
            value = "testing2",
            dataType = DataType.STRING,
            linkId = null,
          ),
        )
      paramsList
        .asSequence()
        .filter { it.paramType == ActionParameterType.PARAMDATA && it.value.isNotEmpty() }
        .associate { it.key to it.value }
      val paramsMap = emptyMap<String, String>()
      val searchSlot = slot<Search>()
      coEvery { fhirEngine.count(capture(searchSlot)) } returns 20
      val recordsCount =
        registerRepository.countRegisterData(registerId = PATIENT_REGISTER, paramsMap = paramsMap)
      Assert.assertEquals(ResourceType.Patient, searchSlot.captured.type)
      Assert.assertEquals(20, recordsCount)
    }
  }

  @Ignore("Refactor this test")
  @Test
  fun countRegisterDataWithParamsAndRelatedEntityLocationFilter() {
    runTest {
      val paramsList =
        arrayListOf(
          ActionParameter(
            key = "paramsName",
            paramType = ActionParameterType.PARAMDATA,
            value = "testing1",
            dataType = DataType.STRING,
            linkId = null,
          ),
          ActionParameter(
            key = "paramName2",
            paramType = ActionParameterType.PARAMDATA,
            value = "testing2",
            dataType = DataType.STRING,
            linkId = null,
          ),
        )
      paramsList
        .asSequence()
        .filter { it.paramType == ActionParameterType.PARAMDATA && it.value.isNotEmpty() }
        .associate { it.key to it.value }
      val paramsMap = emptyMap<String, String>()
      val searchSlot = slot<Search>()
      every {
        registerRepository.retrieveRegisterConfiguration(PATIENT_REGISTER, emptyMap())
      } returns
        RegisterConfiguration(
          appId = "app",
          id = PATIENT_REGISTER,
          fhirResource = fhirResourceConfig(),
          filterDataByRelatedEntityLocation = true,
        )
      coEvery { fhirEngine.count(capture(searchSlot)) } returns 20
      val recordsCount =
        registerRepository.countRegisterData(registerId = PATIENT_REGISTER, paramsMap = paramsMap)
      Assert.assertEquals(ResourceType.Group, searchSlot.captured.type)
      Assert.assertEquals(20, recordsCount)
    }
  }

  @Test
  fun testLoadRegisterDataWithForwardAndReverseIncludedResources() =
    runTest(timeout = 90.seconds) {
      runTest {
        val registerId = HOUSEHOLD_REGISTER_ID
        every { registerRepository.retrieveRegisterConfiguration(registerId, emptyMap()) } returns
          RegisterConfiguration(
            appId = "app",
            id = registerId,
            fhirResource = fhirResourceConfig(),
          )

        val group = createGroup(id = GROUP_ID, active = true, members = listOf(patient))
        val anotherGroup = createGroup(id = "inactiveGroup", active = false)
        val carePlan = createCarePlan(id = "carePlan", subject = patient.asReference())
        val parentTask = createTask(id = TASK_ID, partOf = null, subject = patient.asReference())
        val dependentTask =
          createTask(
            id = PART_OF_TASK_ID,
            partOf = parentTask.asReference(),
            subject = patient.asReference(),
          )
        val observation =
          Observation().apply {
            id = "obs1"
            subject = patient.asReference()
          }

        // Prepare database with required resources
        fhirEngine.create(
          patient,
          group,
          carePlan,
          anotherGroup,
          parentTask,
          dependentTask,
          observation,
        )

        val registerData =
          registerRepository.loadRegisterData(currentPage = 0, registerId = registerId)

        Assert.assertTrue(registerData.isNotEmpty())
        val repositoryResourceData = registerData.firstOrNull()
        Assert.assertTrue(repositoryResourceData?.resource is Group)
        Assert.assertEquals(GROUP_ID, repositoryResourceData?.resource?.id)
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
  fun testLoadProfileDataWithForwardAndReverseIncludedResources() =
    runTest(timeout = 120.seconds) {
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
                      isRevInclude = false,
                    ),
                  ),
              ),
            ),
        )

      val group = createGroup(id = GROUP_ID, active = true, members = listOf(patient))
      val carePlan = createCarePlan(id = "carePlan", subject = patient.asReference())
      val encounter =
        Encounter().apply {
          id = "encounter123"
          subject = patient.asReference()
        }
      val secondaryCarePlan =
        createCarePlan(
          id = SECONDARY_RESOURCE_CARE_PLAN_ID,
          encounter = encounter.asReference(),
          subject = patient.asReference(),
        )

      val parentTask = createTask(id = TASK_ID, partOf = null, subject = patient.asReference())
      val dependentTask =
        createTask(
          id = PART_OF_TASK_ID,
          partOf = parentTask.asReference(),
          subject = patient.asReference(),
        )

      val observation =
        Observation().apply {
          id = "obs1"
          subject = patient.asReference()
        }

      // Prepare database with the required resources
      fhirEngine.create(
        group,
        patient,
        carePlan,
        encounter,
        secondaryCarePlan,
        parentTask,
        dependentTask,
        observation,
      )

      val repositoryResourceData =
        registerRepository.loadProfileData(
          profileId = profileId,
          resourceId = GROUP_ID,
          fhirResourceConfig = null,
          paramsList = null,
        )
      Assert.assertTrue(repositoryResourceData.resource is Group)
      Assert.assertEquals(GROUP_ID, repositoryResourceData.resource.logicalId)
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
        secondaryRepositoryResourceDataList.find {
          it.resource.logicalId == SECONDARY_RESOURCE_CARE_PLAN_ID
        }
      val secondaryResource = secondaryRepositoryResourceData?.resource
      Assert.assertNotNull(secondaryResource)
      Assert.assertTrue(secondaryResource is CarePlan)
      Assert.assertEquals(SECONDARY_RESOURCE_CARE_PLAN_ID, secondaryResource?.logicalId)
      Assert.assertFalse(secondaryRepositoryResourceData?.relatedResourcesMap.isNullOrEmpty())
      Assert.assertTrue(
        secondaryRepositoryResourceData
          ?.relatedResourcesMap
          ?.containsKey(
            ResourceType.Encounter.name,
          )!!,
      )

      // Assert Observation and Encounter resource counts
      assertRepositoryResourceDataContainsCounts(repositoryResourceData)
    }

  private fun assertRepositoryResourceDataContainsCounts(
    repositoryResourceData: RepositoryResourceData,
  ) {
    val relatedResourceCountMap = repositoryResourceData.relatedResourcesCountMap
    Assert.assertEquals(2, relatedResourceCountMap.size)

    // Encounter resources counted
    val encounterRepositoryResourceCounts = relatedResourceCountMap[ENCOUNTERS_COUNT]
    val encounterRepositoryResourceCount = encounterRepositoryResourceCounts?.firstOrNull()
    Assert.assertNotNull(encounterRepositoryResourceCount)
    Assert.assertEquals(
      ResourceType.Encounter,
      encounterRepositoryResourceCount?.relatedResourceType,
    )
    Assert.assertEquals(patient.id, encounterRepositoryResourceCount?.parentResourceId)
    Assert.assertEquals(1L, encounterRepositoryResourceCount?.count)

    // Observation resources counted
    val observationRelatedResourceCounts = relatedResourceCountMap[OBSERVATIONS_COUNT]
    val observationRelatedResourceCount = observationRelatedResourceCounts?.firstOrNull()
    Assert.assertNotNull(observationRelatedResourceCount)
    Assert.assertEquals(
      ResourceType.Observation,
      observationRelatedResourceCount?.relatedResourceType,
    )
    Assert.assertEquals(patient.id, observationRelatedResourceCount?.parentResourceId)
    Assert.assertEquals(1L, observationRelatedResourceCount?.count)
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
                  countResultConfig = CountResultConfig(sumCounts = false),
                  resultAsCount = true,
                ),
                ResourceConfig(
                  id = OBSERVATIONS_COUNT,
                  resource = ResourceType.Observation,
                  searchParameter = SUBJECT,
                  resultAsCount = true,
                  countResultConfig = CountResultConfig(sumCounts = false),
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
                        isRevInclude = false,
                      ),
                    ),
                ),
                ResourceConfig(
                  id = MEMBER_CARE_PLANS,
                  resource = ResourceType.CarePlan,
                  searchParameter = SUBJECT,
                ),
              ),
          ),
        ),
    )

  @Ignore("Check why group meta tag is not set ")
  @Test
  fun testLoadRegisterDataWithAfterFilterByRelatedEntityLocation() {
    val locationId = "location1"
    runTest(timeout = 90.seconds) {
      val group1 =
        createGroup("group1", active = true, members = listOf(patient)).apply {
          meta =
            Meta().apply {
              addTag(
                Coding(
                  "https://smartregister.org/related-entity-location-tag-id",
                  locationId,
                  "Related Entity Location",
                ),
              )
            }
        }
      val group2 = createGroup("group2", active = true, members = listOf(patient))
      val task = createTask("task1", null, patient.asReference())

      // Replace Household Register configuration
      val registerConfiguration =
        configurationRegistry.retrieveConfiguration<RegisterConfiguration>(
          configType = ConfigType.Register,
          configId = HOUSEHOLD_REGISTER_ID,
        )
      configurationRegistry.configCacheMap.clear()
      configurationRegistry.configsJsonMap[HOUSEHOLD_REGISTER_ID] =
        registerConfiguration
          .copy(
            filterDataByRelatedEntityLocation = true,
            fhirResource =
              FhirResourceConfig(baseResource = ResourceConfig(resource = ResourceType.Group)),
          )
          .encodeJson()

      // Set locations
      ApplicationProvider.getApplicationContext<Application>()
        .syncLocationIdsProtoStore
        .updateData { mapOf(locationId to SyncLocationState(locationId, null, ToggleableState.On)) }

      // Prepare resources
      fhirEngine.run {
        create(patient)
        create(group1)
        create(group2)
        create(task)
      }

      val result = registerRepository.loadRegisterData(0, HOUSEHOLD_REGISTER_ID)

      Assert.assertEquals(1, result.size)

      // Re-set register configuration
      configurationRegistry.configsJsonMap[HOUSEHOLD_REGISTER_ID] =
        registerConfiguration.encodeJson()
    }
  }

  private fun createCarePlan(id: String, subject: Reference, encounter: Reference? = null) =
    CarePlan().apply {
      this.id = id
      this.subject = subject
      if (encounter != null) this.encounter = encounter
    }

  private fun createTask(id: String, partOf: Reference?, subject: Reference) =
    Task().apply {
      this.id = id
      if (partOf != null) {
        addPartOf(partOf)
      }
      this.`for` = subject
    }

  private fun createGroup(id: String, active: Boolean, members: List<Resource> = emptyList()) =
    Group().apply {
      this.id = id
      this.active = active
      members.forEach { addMember(Group.GroupMemberComponent(it.asReference())) }
    }
}
