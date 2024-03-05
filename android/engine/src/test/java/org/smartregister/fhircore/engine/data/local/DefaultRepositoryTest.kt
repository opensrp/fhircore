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

package org.smartregister.fhircore.engine.data.local

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.SearchResult
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.gson.Gson
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.DataRequirement
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Procedure
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.ServiceRequest
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.Task
import org.joda.time.LocalDate
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.event.EventWorkflow
import org.smartregister.fhircore.engine.configuration.event.UpdateWorkflowValueConfig
import org.smartregister.fhircore.engine.configuration.profile.ManagingEntityConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository.Companion.PATIENT_CONDITION_RESOLVED_CODE
import org.smartregister.fhircore.engine.data.local.DefaultRepository.Companion.PATIENT_CONDITION_RESOLVED_DISPLAY
import org.smartregister.fhircore.engine.data.local.DefaultRepository.Companion.SNOMED_SYSTEM
import org.smartregister.fhircore.engine.domain.model.Code
import org.smartregister.fhircore.engine.domain.model.KeyValueConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceFilterExpression
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rulesengine.ConfigRulesExecutor
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.generateMissingId
import org.smartregister.fhircore.engine.util.extension.loadResource
import org.smartregister.fhircore.engine.util.extension.plusDays
import org.smartregister.fhircore.engine.util.extension.updateLastUpdated
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor

@HiltAndroidTest
class DefaultRepositoryTest : RobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var gson: Gson

  @Inject lateinit var configRulesExecutor: ConfigRulesExecutor

  @Inject lateinit var fhirPathDataExtractor: FhirPathDataExtractor

  @Inject lateinit var configService: ConfigService

  @Inject lateinit var fhirEngine: FhirEngine

  @Inject lateinit var parser: IParser
  private val application = ApplicationProvider.getApplicationContext<Application>()
  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
  private lateinit var dispatcherProvider: DefaultDispatcherProvider
  private lateinit var sharedPreferenceHelper: SharedPreferencesHelper
  private lateinit var defaultRepository: DefaultRepository
  private lateinit var spiedConfigService: ConfigService

  @Before
  fun setUp() {
    hiltRule.inject()
    dispatcherProvider = DefaultDispatcherProvider()
    sharedPreferenceHelper = SharedPreferencesHelper(application, gson)
    spiedConfigService = spyk(configService)
    defaultRepository =
      DefaultRepository(
        fhirEngine = fhirEngine,
        dispatcherProvider = dispatcherProvider,
        sharedPreferencesHelper = sharedPreferenceHelper,
        configurationRegistry = configurationRegistry,
        configService = spiedConfigService,
        configRulesExecutor = configRulesExecutor,
        fhirPathDataExtractor = fhirPathDataExtractor,
        parser = parser,
      )
  }

  @Test
  fun loadResourceShouldGetResourceUsingId() {
    val samplePatientId = "12345"
    val samplePatient: Patient = Patient().apply { id = samplePatientId }

    coEvery { fhirEngine.get<Patient>(any()) } answers { samplePatient }

    runBlocking {
      val actualPatient = defaultRepository.loadResource<Patient>(samplePatientId)
      Assert.assertEquals("12345", actualPatient?.id)
    }

    coVerify { fhirEngine.get<Patient>("12345") }
  }

  @Test
  fun loadResourceShouldGetResourceWithReference() = runTest {
    val sampleResource = Patient().apply { id = "123345677" }
    val sampleResourceReference = sampleResource.asReference()
    coEvery { fhirEngine.get(any(), sampleResource.logicalId) } answers { sampleResource }

    val result = defaultRepository.loadResource(sampleResourceReference)
    Assert.assertEquals(sampleResource, result)
    coVerify(exactly = 1) { fhirEngine.get(sampleResource.resourceType, sampleResource.logicalId) }
  }

  @Test
  fun loadResourceShouldGetResourceWithResourceTypeAndLogicalId() = runTest {
    val sampleResource = Patient().apply { id = "123345677" }
    coEvery { fhirEngine.get(any(), sampleResource.logicalId) } answers { sampleResource }

    val result = defaultRepository.loadResource(sampleResource.logicalId, ResourceType.Patient)
    Assert.assertEquals(sampleResource, result)
    coVerify(exactly = 1) { fhirEngine.get(sampleResource.resourceType, sampleResource.logicalId) }
  }

  @Test
  fun searchResourceForGivenTokenShouldReturn1PatientUsingId() {
    val samplePatientId = "12345"
    coEvery { fhirEngine.search<Patient>(any()) } returns
      listOf(SearchResult(resource = Faker.buildPatient(id = samplePatientId), null, null))

    runBlocking {
      val actualPatients =
        defaultRepository.searchResourceFor<Patient>(
          token = Patient.RES_ID,
          subjectId = samplePatientId,
          subjectType = ResourceType.Patient,
          dataQueries = emptyList(),
          configComputedRuleValues = emptyMap(),
        )
      Assert.assertEquals(1, actualPatients.size)
    }

    coVerify { fhirEngine.search<Patient>(any()) }
  }

  @Test
  fun searchShouldReturn1ConditionGivenConditionTypeDataRequirement() = runTest {
    val coding = Coding("https://system.co", "codi", "Condition code")
    val condition = Condition().apply { code = CodeableConcept(coding) }

    fhirEngine.create(condition)

    runBlocking {
      val actualPatients =
        defaultRepository.searchCondition(
          dataRequirement =
            DataRequirement().apply {
              type = Enumerations.ResourceType.CONDITION.toCode()
              addCodeFilter(
                DataRequirement.DataRequirementCodeFilterComponent()
                  .addCode(coding)
                  .setPath("code"),
              )
            },
        )
      Assert.assertEquals(1, actualPatients.size)
    }
  }

  @Test
  fun addOrUpdateShouldCallFhirEngineUpdateWhenResourceExists() {
    val patientId = "15672-9234"
    val patient: Patient =
      Patient().apply {
        id = patientId
        active = true
        birthDate = LocalDate.parse("1996-08-17").toDate()
        gender = Enumerations.AdministrativeGender.MALE
        address =
          listOf(
            Address().apply {
              city = "Lahore"
              country = "Pakistan"
            },
          )
        name =
          listOf(
            HumanName().apply {
              given = mutableListOf(StringType("Salman"))
              family = "Ali"
            },
          )
        telecom = listOf(ContactPoint().apply { value = "12345" })
      }

    val savedPatientSlot = slot<Patient>()

    coEvery { fhirEngine.get(any(), any()) } answers { patient }
    coEvery { fhirEngine.update(any()) } just runs

    // Call the function under test
    runBlocking { defaultRepository.addOrUpdate(resource = patient) }

    coVerify { fhirEngine.get(ResourceType.Patient, patientId) }
    coVerify { fhirEngine.update(capture(savedPatientSlot)) }

    savedPatientSlot.captured.run {
      Assert.assertEquals(patient.idElement.idPart, idElement.idPart)
      Assert.assertEquals(patient.active, active)
      Assert.assertEquals(patient.birthDate, birthDate)
      Assert.assertEquals(patient.telecom[0].value, telecom[0].value)
      Assert.assertEquals(patient.name[0].family, name[0].family)
      Assert.assertEquals(patient.name[0].given[0].value, name[0].given[0].value)
      Assert.assertEquals(patient.address[0].city, address[0].city)
      Assert.assertEquals(patient.address[0].country, address[0].country)
    }

    // verify exception scenario
    coEvery { fhirEngine.get(ResourceType.Patient, any()) } throws
      mockk<ResourceNotFoundException>()
    coEvery { fhirEngine.create(any()) } returns listOf()
    runBlocking { defaultRepository.addOrUpdate(resource = Patient()) }
    coVerify(exactly = 1) { fhirEngine.create(any()) }
  }

  @Test
  fun saveShouldCallResourceGenerateMissingId() {
    mockkStatic(Resource::generateMissingId)
    val resource = spyk(Patient())

    coEvery { fhirEngine.get(ResourceType.Patient, any()) } throws
      ResourceNotFoundException("Exce", "Exce")
    coEvery { fhirEngine.create(any()) } returns listOf()

    runBlocking { defaultRepository.create(true, resource) }

    verify { resource.generateMissingId() }

    unmockkStatic(Resource::generateMissingId)
  }

  @Test
  fun `create() should add Resource_meta_lastUpdated`() {
    mockkStatic(Resource::updateLastUpdated)
    val resource = spyk(Patient())

    coEvery { fhirEngine.create(any()) } returns listOf()
    Assert.assertNull(resource.meta.lastUpdated)

    runBlocking { defaultRepository.create(true, resource) }

    Assert.assertNotNull(resource.meta.lastUpdated)
    verify { resource.updateLastUpdated() }
    unmockkStatic(Resource::updateLastUpdated)
  }

  @Test
  fun testCreateShouldNotDuplicateMetaTagsWithSameSystemCode() {
    val system = "https://smartregister.org/location-tag-id"
    val code = "86453"
    val anotherCode = "10200"
    val coding = Coding(system, code, "Location")
    val anotherCoding = Coding(system, anotherCode, "Location")
    val resource = Patient().apply { meta.addTag(coding) }

    // Meta contains 1 tag with code 86453
    Assert.assertEquals(1, resource.meta.tag.size)
    val firstTag = resource.meta.tag.first()
    Assert.assertEquals(code, firstTag.code)
    Assert.assertEquals(system, firstTag.system)

    coEvery { fhirEngine.create(any()) } returns listOf(resource.id)
    every { spiedConfigService.provideResourceTags(sharedPreferenceHelper) } returns
      listOf(coding, anotherCoding)
    runBlocking { defaultRepository.create(true, resource) }

    // Expecting 2 tags; tag with code 86453 should not be duplicated.
    Assert.assertEquals(2, resource.meta.tag.size)
    Assert.assertNotNull(resource.meta.lastUpdated)
    Assert.assertNotNull(resource.meta.getTag(system, code))
  }

  @Test
  fun `update() should call Resource#updateLastUpdated and FhirEngine#update`() {
    mockkStatic(Resource::updateLastUpdated)
    val resource = spyk(Patient())

    coEvery { fhirEngine.update(any()) } just runs
    Assert.assertNull(resource.meta.lastUpdated)

    runBlocking { defaultRepository.update(resource) }

    Assert.assertNotNull(resource.meta.lastUpdated)
    verify { resource.updateLastUpdated() }
    coVerify { fhirEngine.update(resource) }
    unmockkStatic(Resource::updateLastUpdated)
  }

  @Test
  fun deleteShouldDeleteResourceFromEngine() = runTest {
    coEvery { fhirEngine.delete(any(), any()) } just runs
    val sampleResource = Patient().apply { id = "testid" }
    defaultRepository.delete(sampleResource)

    coVerify { fhirEngine.delete(sampleResource.resourceType, sampleResource.logicalId) }
  }

  @Test
  fun addOrUpdateShouldCallResourceGenerateMissingIdWhenResourceIdIsNull() {
    mockkStatic(Resource::generateMissingId)
    val resource = Patient()

    coEvery { fhirEngine.get(ResourceType.Patient, any()) } throws
      ResourceNotFoundException("Exce", "Exce")
    coEvery { fhirEngine.create(any()) } returns listOf()

    runBlocking { defaultRepository.addOrUpdate(resource = resource) }

    coVerify { resource.generateMissingId() }

    unmockkStatic(Resource::generateMissingId)
  }

  @Test
  fun loadManagingEntityShouldReturnPatient() =
    runTest(timeout = 30.seconds) {
      val patient = Faker.buildPatient(id = "12345")

      val relatedPerson =
        RelatedPerson().apply {
          id = "12983"
          this.patient = Reference("Patient/12345")
        }

      val group =
        Group().apply {
          id = "groupId"
          managingEntity = relatedPerson.asReference()
        }

      // Add required resources to the database
      try {
        fhirEngine.create(patient, relatedPerson, group)
      } catch (exception: Throwable) {
        println(exception.localizedMessage)
      }

      val managingEntity = defaultRepository.loadManagingEntity(group)
      Assert.assertEquals("12345", managingEntity?.logicalId)
    }

  @Test
  fun changeManagingEntityShouldVerifyFhirEngineCalls() {
    val patient =
      Patient().apply {
        id = "54321"
        addName().apply {
          addGiven("Sam")
          family = "Smith"
        }
        addTelecom().apply { value = "ssmith@mail.com" }
        addAddress().apply {
          district = "Mawar"
          city = "Jakarta"
        }
        gender = Enumerations.AdministrativeGender.MALE
      }
    val relatedPerson =
      RelatedPerson().apply {
        active = true
        name = patient.name
        birthDate = patient.birthDate
        telecom = patient.telecom
        address = patient.address
        gender = patient.gender
        this.patient = patient.asReference()
        id = "testRelatedPersonId"
      }

    val defaultRepositorySpy = spyk(defaultRepository)

    coEvery { fhirEngine.get<Patient>("54321") } returns patient

    coEvery { fhirEngine.get<RelatedPerson>("12983") } returns relatedPerson

    coEvery { fhirEngine.create(any()) } returns listOf()

    val group =
      Group().apply {
        id = "73847"
        managingEntity = Reference("RelatedPerson/12983")
        managingEntity.id = "33292"
      }
    coEvery { fhirEngine.get<Group>("73847") } returns group

    coEvery { fhirEngine.update(any()) } just runs

    coEvery { fhirEngine.get(relatedPerson.resourceType, relatedPerson.logicalId) } answers
      {
        relatedPerson
      }
    runBlocking {
      defaultRepositorySpy.changeManagingEntity(
        newManagingEntityId = "54321",
        groupId = "73847",
        ManagingEntityConfig(
          resourceType = ResourceType.Patient,
          relationshipCode =
            Code().apply {
              system = "http://hl7.org/fhir/ValueSet/relatedperson-relationshiptype"
              code = "99990006"
              display = "Family Head"
            },
        ),
      )
    }

    coVerify { fhirEngine.get<Patient>("54321") }

    coVerify { fhirEngine.get<Group>("73847") }

    coVerify { defaultRepositorySpy.addOrUpdate(resource = relatedPerson) }
  }

  @Test
  fun changeManagingEntityShouldVerifyFhirEngineCallsEvenWithAnOrganizationAsTheCurrentManagingEntity() {
    val patient =
      Patient().apply {
        id = "54321"
        addName().apply {
          addGiven("Sam")
          family = "Smith"
        }
        addTelecom().apply { value = "ssmith@mail.com" }
        addAddress().apply {
          district = "Mawar"
          city = "Jakarta"
        }
        gender = Enumerations.AdministrativeGender.MALE
      }
    val relatedPerson =
      RelatedPerson().apply {
        active = true
        name = patient.name
        birthDate = patient.birthDate
        telecom = patient.telecom
        address = patient.address
        gender = patient.gender
        this.patient = patient.asReference()
        id = "testRelatedPersonId"
      }

    val organization =
      Organization().apply {
        id = "12983"
        name = "Test Organization"
      }

    val defaultRepositorySpy = spyk(defaultRepository)

    coEvery { fhirEngine.get<Patient>("54321") } returns patient
    coEvery { fhirEngine.get<Organization>("12983") } returns organization
    coEvery { fhirEngine.get<RelatedPerson>(any()) } returns relatedPerson
    coEvery { fhirEngine.create(any()) } returns listOf()

    val group =
      Group().apply {
        id = "73847"
        managingEntity = Reference("Organization/12983")
        managingEntity.id = "33292"
      }
    coEvery { fhirEngine.get<Group>("73847") } returns group
    coEvery { fhirEngine.update(any()) } just runs

    runBlocking {
      defaultRepositorySpy.changeManagingEntity(
        newManagingEntityId = "54321",
        groupId = "73847",
        ManagingEntityConfig(
          resourceType = ResourceType.Patient,
          relationshipCode =
            Code().apply {
              system = "http://hl7.org/fhir/ValueSet/relatedperson-relationshiptype"
              code = "99990006"
              display = "Family Head"
            },
        ),
      )
    }

    coVerify { fhirEngine.get<Patient>("54321") }
    coVerify { fhirEngine.get<Group>("73847") }
    coVerify(inverse = true) { defaultRepositorySpy.addOrUpdate(resource = relatedPerson) }
  }

  @Test
  fun removeGroupGivenGroupAlreadyDeletedShouldThrowIllegalStateException() {
    val group =
      Group().apply {
        id = "73847"
        active = false
      }
    coEvery { fhirEngine.loadResource<Group>("73847") } returns group

    Assert.assertThrows(IllegalStateException::class.java) {
      runBlocking { defaultRepository.removeGroup(group.logicalId, false, emptyMap()) }
    }
  }

  @Test
  fun removeGroupShouldRemoveManagingEntityRelatedPerson() = runTest {
    val managingEntityRelatedPerson = RelatedPerson().apply { id = "testRelatedPersonId" }
    val defaultRepositorySpy =
      spyk(
        DefaultRepository(
          fhirEngine = fhirEngine,
          dispatcherProvider = dispatcherProvider,
          sharedPreferencesHelper = mockk(),
          configurationRegistry = mockk(),
          configService = mockk(),
          configRulesExecutor = mockk(),
          fhirPathDataExtractor = fhirPathDataExtractor,
          parser = parser,
        ),
      )
    coEvery { fhirEngine.search<RelatedPerson>(any()) } returns
      listOf(SearchResult(resource = managingEntityRelatedPerson, null, null))
    coEvery { defaultRepositorySpy.delete(any()) } just runs
    coEvery { defaultRepositorySpy.addOrUpdate(resource = any()) } just runs
    val group =
      Group().apply {
        id = "testGroupId"
        active = true
        managingEntity = managingEntityRelatedPerson.asReference()
      }
    coEvery { fhirEngine.loadResource<Group>(group.id) } returns group

    defaultRepositorySpy.removeGroup(group.id, isDeactivateMembers = false, emptyMap())
    coVerify { defaultRepositorySpy.delete(managingEntityRelatedPerson) }
    Assert.assertFalse(group.active)
  }

  @Test
  fun removeGroupMemberShouldDeactivateGroupMember() = runTest {
    val memberId = "testMemberId"
    val patientMemberRep =
      Patient().apply {
        id = memberId
        active = true
      }
    val defaultRepositorySpy = spyk(defaultRepository)
    coEvery { defaultRepositorySpy.addOrUpdate(resource = any()) } just runs
    coEvery { fhirEngine.get(patientMemberRep.resourceType, memberId) } returns patientMemberRep

    defaultRepositorySpy.removeGroupMember(
      memberId = memberId,
      groupId = null,
      groupMemberResourceType = patientMemberRep.resourceType,
      configComputedRuleValues = emptyMap(),
    )
    Assert.assertFalse(patientMemberRep.active)
    coVerify { defaultRepositorySpy.addOrUpdate(resource = patientMemberRep) }
  }

  @Test
  fun removeGroupMemberShouldCatchResourceNotFoundException() = runTest {
    val memberId = "testMemberId"
    val patientMemberRep =
      Patient().apply {
        id = memberId
        active = true
      }
    val defaultRepositorySpy = spyk(defaultRepository)
    coEvery { defaultRepositorySpy.addOrUpdate(resource = any()) } just runs
    coEvery { fhirEngine.get(patientMemberRep.resourceType, memberId) }
      .throws(ResourceNotFoundException("type", "id"))

    defaultRepositorySpy.removeGroupMember(
      memberId = memberId,
      groupId = null,
      groupMemberResourceType = patientMemberRep.resourceType,
      configComputedRuleValues = emptyMap(),
    )
    Assert.assertTrue(patientMemberRep.active)
  }

  @Test
  fun testDeleteWithResourceId() = runTest {
    val fhirEngine: FhirEngine = mockk(relaxUnitFun = true)
    val defaultRepository =
      spyk(
        DefaultRepository(
          fhirEngine = fhirEngine,
          dispatcherProvider = dispatcherProvider,
          sharedPreferencesHelper = mockk(),
          configurationRegistry = mockk(),
          configService = mockk(),
          configRulesExecutor = mockk(),
          fhirPathDataExtractor = fhirPathDataExtractor,
          parser = parser,
        ),
      )

    defaultRepository.delete(resourceType = ResourceType.Patient, resourceId = "123")

    coVerify { fhirEngine.delete(any(), any()) }
  }

  @Test
  fun testRemoveGroupDeletesRelatedPersonAndUpdatesGroup() {
    defaultRepository = spyk(defaultRepository)
    val groupId = "73847"
    val patientId = "6745"
    val patient = Patient().setId(patientId)

    val group =
      Group().apply {
        id = groupId
        active = true
        member = mutableListOf(Group.GroupMemberComponent(Reference("Patient/$patientId")))
      }
    coEvery { fhirEngine.loadResource<Group>("73847") } returns group
    coEvery { fhirEngine.get(ResourceType.Patient, patientId) } returns patient

    val relatedPersonId = "1234"
    val relatedPerson = RelatedPerson().setId(relatedPersonId)
    coEvery { fhirEngine.search<RelatedPerson>(any()) } returns
      listOf(SearchResult(resource = relatedPerson as RelatedPerson, null, null))
    coEvery { defaultRepository.delete(any()) } just runs
    coEvery { defaultRepository.addOrUpdate(resource = any()) } just runs

    runBlocking { defaultRepository.removeGroup(groupId, true, emptyMap()) }

    coVerify { defaultRepository.delete(relatedPerson) }
    coVerify { defaultRepository.addOrUpdate(resource = patient) }
    coVerify { defaultRepository.addOrUpdate(resource = group) }
  }

  @Test
  fun removeGroupMemberDeletesRelatedPersonAndUpdatesGroup() {
    defaultRepository = spyk(defaultRepository)
    val groupId = "73847"
    val memberId = "6745"
    val groupMemberResourceType = ResourceType.Patient
    val patient =
      Patient().apply {
        id = memberId
        active = true
      }
    val relatedPersonId = "1234"
    val relatedPerson =
      RelatedPerson(Reference(patient).apply { id = memberId }).apply { id = relatedPersonId }

    val group =
      Group().apply {
        id = groupId
        active = true
        member = mutableListOf(Group.GroupMemberComponent(Reference("Patient/$memberId")))
        managingEntity = Reference("RelatedPerson/$relatedPersonId")
      }
    coEvery { fhirEngine.loadResource<Group>("73847") } returns group
    coEvery { fhirEngine.get(ResourceType.Patient, memberId) } returns patient

    coEvery { fhirEngine.search<RelatedPerson>(any()) } returns
      listOf(SearchResult(resource = relatedPerson, null, null))
    coEvery { defaultRepository.delete(any()) } just runs
    coEvery { defaultRepository.addOrUpdate(resource = any()) } just runs

    runBlocking {
      defaultRepository.removeGroupMember(
        memberId = memberId,
        groupId = groupId,
        groupMemberResourceType = groupMemberResourceType,
        emptyMap(),
      )
    }

    coVerify { defaultRepository.delete(relatedPerson) }
    coVerify { defaultRepository.addOrUpdate(resource = group) }
  }

  @Test
  fun addOrUpdateShouldUpdateLastUpdatedToNow() {
    val date = Date()
    val patientId = "15672-9234"
    val patient: Patient =
      Patient().apply {
        meta.lastUpdated = date.plusDays(-20)
        id = "15672-9234"
        active = true
      }
    val savedPatientSlot = slot<Patient>()
    coEvery { fhirEngine.get(any(), any()) } answers { patient }
    coEvery { fhirEngine.update(any()) } just runs
    runBlocking { defaultRepository.addOrUpdate(resource = patient) }
    coVerify { fhirEngine.get(ResourceType.Patient, patientId) }
    coVerify { fhirEngine.update(capture(savedPatientSlot)) }
    Assert.assertEquals(
      date.formatDate("mm-dd-yyyy"),
      patient.meta.lastUpdated.formatDate("mm-dd-yyyy"),
    )
  }

  @Test
  fun testCloseResourceUpdatesCorrectTaskStatus() {
    val task =
      Task().apply {
        id = "37793d31-def5-40bd-a2e3-fdaf5a0ddc53"
        status = Task.TaskStatus.READY
      }
    coEvery { fhirEngine.update(any()) } just runs
    val taskSlot = slot<Task>()

    val updatedValues =
      UpdateWorkflowValueConfig(
        jsonPathExpression = "Task.status",
        value = JsonPrimitive("cancelled"),
      )
    val eventWorkflow = EventWorkflow(updateValues = listOf(updatedValues))

    runBlocking { defaultRepository.closeResource(task, eventWorkflow) }
    coVerify { fhirEngine.update(capture(taskSlot)) }
    Assert.assertEquals("37793d31-def5-40bd-a2e3-fdaf5a0ddc53", taskSlot.captured.id)
    Assert.assertEquals(Task.TaskStatus.CANCELLED, taskSlot.captured.status)
  }

  @Test
  fun testCloseResourceUpdatesCorrectCarePlanStatus() {
    val carePlan =
      CarePlan().apply {
        id = "37793d31-def5-40bd-a2e3-fdaf5a0ddc53"
        status = CarePlan.CarePlanStatus.DRAFT
      }
    coEvery { fhirEngine.update(any()) } just runs
    val carePlanSlot = slot<CarePlan>()

    val updatedValues =
      UpdateWorkflowValueConfig(
        jsonPathExpression = "CarePlan.status",
        value = JsonPrimitive("completed"),
      )
    val eventWorkflow = EventWorkflow(updateValues = listOf(updatedValues))

    runBlocking { defaultRepository.closeResource(carePlan, eventWorkflow) }
    coVerify { fhirEngine.update(capture(carePlanSlot)) }
    Assert.assertEquals("37793d31-def5-40bd-a2e3-fdaf5a0ddc53", carePlanSlot.captured.id)
    Assert.assertEquals(CarePlan.CarePlanStatus.COMPLETED, carePlanSlot.captured.status)
  }

  @Test
  fun testUpdateResourcesRecursivelyClosesFilteredResource() = runTest {
    val patient =
      Patient().apply {
        id = "123345677"
        active = true
      }

    val serviceRequest =
      ServiceRequest().apply {
        id = "37793d31-def5-40bd-a2e3-fdaf5a0ddc53"
        status = ServiceRequest.ServiceRequestStatus.DRAFT
        subject = patient.asReference()
      }

    val resourceConfig =
      ResourceConfig(id = "serviceRequest-id", resource = serviceRequest.resourceType)

    fhirEngine.create(patient, serviceRequest)

    val updatedValues =
      UpdateWorkflowValueConfig(
        jsonPathExpression = "ServiceRequest.status",
        value = JsonPrimitive("revoked"),
      )
    val resourceFilterExpression =
      ResourceFilterExpression(
        conditionalFhirPathExpressions = listOf("ServiceRequest.status != 'completed'"),
      )
    val eventWorkflow =
      EventWorkflow(
        updateValues = listOf(updatedValues),
        resourceFilterExpression = resourceFilterExpression,
      )

    defaultRepository.updateResourcesRecursively(
      resourceConfig = resourceConfig,
      subject = patient,
      eventWorkflow = eventWorkflow,
    )

    val serviceRequestSlot = slot<ServiceRequest>()
    coVerify { fhirEngine.update(capture(serviceRequestSlot)) }
    Assert.assertEquals("37793d31-def5-40bd-a2e3-fdaf5a0ddc53", serviceRequestSlot.captured.id)
    Assert.assertEquals(
      ServiceRequest.ServiceRequestStatus.REVOKED,
      serviceRequestSlot.captured.status,
    )
  }

  @Test
  fun testUpdateResourcesRecursivelyDoesNotCloseFilteredOutResource() = runTest {
    val patient =
      Patient().apply {
        id = "123345677"
        active = true
      }

    val serviceRequest =
      ServiceRequest().apply {
        id = "37793d31-def5-40bd-a2e3-fdaf5a0ddc53"
        status = ServiceRequest.ServiceRequestStatus.COMPLETED
        subject = patient.asReference()
      }

    val resourceConfig =
      ResourceConfig(id = "serviceRequest-id", resource = serviceRequest.resourceType)

    fhirEngine.create(patient, serviceRequest)

    val updatedValues =
      UpdateWorkflowValueConfig(
        jsonPathExpression = "ServiceRequest.status",
        value = JsonPrimitive("revoked"),
      )

    val resourceFilterExpression =
      ResourceFilterExpression(
        conditionalFhirPathExpressions = listOf("ServiceRequest.status != 'completed'"),
      )
    val eventWorkflow =
      EventWorkflow(
        updateValues = listOf(updatedValues),
        resourceFilterExpression = resourceFilterExpression,
      )

    defaultRepository.updateResourcesRecursively(
      resourceConfig = resourceConfig,
      subject = patient,
      eventWorkflow = eventWorkflow,
    )

    coVerify(exactly = 0) { fhirEngine.update(any()) }
  }

  @Test
  fun testUpdateResourcesRecursivelyClosesResource() = runTest {
    val patient =
      Patient().apply {
        id = "123345677"
        active = true
      }

    val carePlan =
      CarePlan().apply {
        id = "37793d31-def5-40bd-a2e3-fdaf5a0ddc53"
        status = CarePlan.CarePlanStatus.DRAFT
        subject = patient.asReference()
      }

    val resourceConfig = ResourceConfig(id = "carePlan-id", resource = carePlan.resourceType)

    fhirEngine.create(patient, carePlan)

    val updatedValues =
      UpdateWorkflowValueConfig(
        jsonPathExpression = "CarePlan.status",
        value = JsonPrimitive("completed"),
      )
    val eventWorkflow = EventWorkflow(updateValues = listOf(updatedValues))
    defaultRepository.updateResourcesRecursively(
      resourceConfig = resourceConfig,
      subject = patient,
      eventWorkflow = eventWorkflow,
    )

    val carePlanSlot = slot<CarePlan>()
    coVerify { fhirEngine.update(capture(carePlanSlot)) }
    Assert.assertEquals("37793d31-def5-40bd-a2e3-fdaf5a0ddc53", carePlanSlot.captured.id)
    Assert.assertEquals(CarePlan.CarePlanStatus.COMPLETED, carePlanSlot.captured.status)
  }

  @Test
  fun testFilterRelatedResourcesShouldReturnTrueIfProvidedExpressionEvaluatesToTrue() {
    val resourceConfig =
      ResourceConfig(
        resource = ResourceType.Task,
        filterFhirPathExpressions = listOf(KeyValueConfig("Task.status", "ready")),
      )
    val task =
      Task().apply {
        id = "37793d31-def5-40bd-a2e3-fdaf5a0ddc53"
        status = Task.TaskStatus.READY
      }
    val result = defaultRepository.filterRelatedResource(task, resourceConfig)
    Assert.assertTrue(result)
  }

  @Test
  fun testFilterRelatedResourcesShouldReturnFalseIfProvidedExpressionEvaluatesToFalse() {
    val resourceConfig =
      ResourceConfig(
        resource = ResourceType.Task,
        filterFhirPathExpressions = listOf(KeyValueConfig("Task.status", "cancelled")),
      )
    val task =
      Task().apply {
        id = "37793d31-def5-40bd-a2e3-fdaf5a0ddc53"
        status = Task.TaskStatus.READY
      }
    val result = defaultRepository.filterRelatedResource(task, resourceConfig)
    Assert.assertFalse(result)
  }

  @Test
  fun testFilterRelatedResourcesShouldReturnTrueIfExpressionIsNotProvided() {
    val resourceConfig = ResourceConfig(resource = ResourceType.Task)
    val task =
      Task().apply {
        id = "37793d31-def5-40bd-a2e3-fdaf5a0ddc53"
        status = Task.TaskStatus.READY
      }
    val result = defaultRepository.filterRelatedResource(task, resourceConfig)
    Assert.assertTrue(result)
  }

  @Test
  fun testCloseResourceUpdatesCorrectProcedureStatus() {
    val procedure =
      Procedure().apply {
        id = "37793d31-def5-40bd-a2e3-fdaf5a0ddc53"
        status = Procedure.ProcedureStatus.UNKNOWN
      }
    coEvery { fhirEngine.update(any()) } just runs
    val procedureSlot = slot<Procedure>()

    val updatedValues =
      UpdateWorkflowValueConfig(
        jsonPathExpression = "Procedure.status",
        value = JsonPrimitive("stopped"),
      )
    val eventWorkflow = EventWorkflow(updateValues = listOf(updatedValues))
    runBlocking { defaultRepository.closeResource(procedure, eventWorkflow) }
    coVerify { fhirEngine.update(capture(procedureSlot)) }
    Assert.assertEquals("37793d31-def5-40bd-a2e3-fdaf5a0ddc53", procedureSlot.captured.id)
    Assert.assertEquals(Procedure.ProcedureStatus.STOPPED, procedureSlot.captured.status)
  }

  @Test
  fun testCloseResourceUpdatesCorrectServiceRequestStatus() {
    val serviceRequest =
      ServiceRequest().apply {
        id = "37793d31-def5-40bd-a2e3-fdaf5a0ddc53"
        status = ServiceRequest.ServiceRequestStatus.ACTIVE
      }
    coEvery { fhirEngine.update(any()) } just runs
    val serviceRequestSlot = slot<ServiceRequest>()
    val updatedValues =
      UpdateWorkflowValueConfig(
        jsonPathExpression = "ServiceRequest.status",
        value = JsonPrimitive("revoked"),
      )
    val eventWorkflow = EventWorkflow(updateValues = listOf(updatedValues))
    runBlocking {
      defaultRepository.closeResource(
        serviceRequest,
        eventWorkflow,
      )
    }
    coVerify { fhirEngine.update(capture(serviceRequestSlot)) }
    Assert.assertEquals("37793d31-def5-40bd-a2e3-fdaf5a0ddc53", serviceRequestSlot.captured.id)
    Assert.assertEquals(
      ServiceRequest.ServiceRequestStatus.REVOKED,
      serviceRequestSlot.captured.status,
    )
  }

  @Test
  fun testCloseResourceUpdatesCorrectConditionStatus() {
    val condition =
      Condition().apply {
        id = "37793d31-def5-40bd-a2e3-fdaf5a0ddc53"
        clinicalStatus =
          CodeableConcept().apply {
            coding =
              listOf(
                Coding().apply {
                  system = "sample system"
                  display = "sample display"
                  code = "sample code"
                },
              )
          }
      }
    coEvery { fhirEngine.update(any()) } just runs
    val conditionSlot = slot<Condition>()

    val updatedValueCode =
      UpdateWorkflowValueConfig(
        "Condition.clinicalStatus.coding[0].code",
        JsonPrimitive(PATIENT_CONDITION_RESOLVED_CODE),
      )
    val updatedValueDisplay =
      UpdateWorkflowValueConfig(
        "Condition.clinicalStatus.coding[0].display",
        JsonPrimitive(PATIENT_CONDITION_RESOLVED_DISPLAY),
      )
    val updatedValueSystem =
      UpdateWorkflowValueConfig(
        "Condition.clinicalStatus.coding[0].system",
        JsonPrimitive(SNOMED_SYSTEM),
      )

    val eventWorkflow =
      EventWorkflow(
        updateValues = listOf(updatedValueCode, updatedValueDisplay, updatedValueSystem),
      )
    runBlocking { defaultRepository.closeResource(condition, eventWorkflow) }
    coVerify { fhirEngine.update(capture(conditionSlot)) }
    val capturedCode = conditionSlot.captured.clinicalStatus.coding.first()
    Assert.assertEquals("37793d31-def5-40bd-a2e3-fdaf5a0ddc53", conditionSlot.captured.id)
    Assert.assertEquals(DefaultRepository.PATIENT_CONDITION_RESOLVED_CODE, capturedCode.code)
    Assert.assertEquals(DefaultRepository.SNOMED_SYSTEM, capturedCode.system)
    Assert.assertEquals(DefaultRepository.PATIENT_CONDITION_RESOLVED_DISPLAY, capturedCode.display)
  }

  // TODO Refactor/Remove after https://github.com/opensrp/fhircore/issues/2488
  @Test
  fun testCloseResourceUpdatesCorrectConditionStatusForClosePNCCondition() {
    val condition =
      Condition().apply {
        id = "37793d31-def5-40bd-a2e3-fdaf5a0ddc53"
        clinicalStatus =
          CodeableConcept().apply {
            coding =
              listOf(
                Coding().apply {
                  system = "sample system"
                  display = "sample display"
                  code = "sample code"
                },
              )
          }
        onset =
          DateTimeType(
            Date.from(
              java.time.LocalDate.now()
                .minusDays(30)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant(),
            ),
          )
      }
    coEvery { fhirEngine.update(any()) } just runs
    val conditionSlot = slot<Condition>()
    val updatedValueCode =
      UpdateWorkflowValueConfig(
        "Condition.clinicalStatus.coding[0].code",
        JsonPrimitive(PATIENT_CONDITION_RESOLVED_CODE),
      )
    val updatedValueDisplay =
      UpdateWorkflowValueConfig(
        "Condition.clinicalStatus.coding[0].display",
        JsonPrimitive(PATIENT_CONDITION_RESOLVED_DISPLAY),
      )
    val updatedValueSystem =
      UpdateWorkflowValueConfig(
        "Condition.clinicalStatus.coding[0].system",
        JsonPrimitive(SNOMED_SYSTEM),
      )

    val eventWorkflow =
      EventWorkflow(
        updateValues = listOf(updatedValueCode, updatedValueDisplay, updatedValueSystem),
      )

    runBlocking {
      defaultRepository.closeResource(
        condition,
        eventWorkflow = eventWorkflow,
      )
    }
    coVerify { fhirEngine.update(capture(conditionSlot)) }
    val capturedCode = conditionSlot.captured.clinicalStatus.coding.first()
    Assert.assertEquals("37793d31-def5-40bd-a2e3-fdaf5a0ddc53", conditionSlot.captured.id)
    Assert.assertEquals(DefaultRepository.PATIENT_CONDITION_RESOLVED_CODE, capturedCode.code)
    Assert.assertEquals(DefaultRepository.SNOMED_SYSTEM, capturedCode.system)
    Assert.assertEquals(DefaultRepository.PATIENT_CONDITION_RESOLVED_DISPLAY, capturedCode.display)
  }

  // TODO Refactor/Remove after https://github.com/opensrp/fhircore/issues/2488
  @Test
  fun testCloseResourceUpdatesCorrectConditionStatusForCloseSickChildCondition() {
    val condition =
      Condition().apply {
        id = "37793d31-def5-40bd-a2e3-fdaf5a0ddc53"
        clinicalStatus =
          CodeableConcept().apply {
            coding =
              listOf(
                Coding().apply {
                  system = "sample system"
                  display = "sample display"
                  code = "sample code"
                },
              )
          }
        onset =
          DateTimeType(
            Date.from(
              java.time.LocalDate.now()
                .minusDays(30)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant(),
            ),
          )
      }
    coEvery { fhirEngine.update(any()) } just runs
    val conditionSlot = slot<Condition>()
    val updatedValueCode =
      UpdateWorkflowValueConfig(
        "Condition.clinicalStatus.coding[0].code",
        JsonPrimitive(PATIENT_CONDITION_RESOLVED_CODE),
      )
    val updatedValueDisplay =
      UpdateWorkflowValueConfig(
        "Condition.clinicalStatus.coding[0].display",
        JsonPrimitive(PATIENT_CONDITION_RESOLVED_DISPLAY),
      )
    val updatedValueSystem =
      UpdateWorkflowValueConfig(
        "Condition.clinicalStatus.coding[0].system",
        JsonPrimitive(SNOMED_SYSTEM),
      )

    val eventWorkflow =
      EventWorkflow(
        updateValues = listOf(updatedValueCode, updatedValueDisplay, updatedValueSystem),
      )

    runBlocking {
      defaultRepository.closeResource(
        condition,
        eventWorkflow,
      )
    }
    coVerify { fhirEngine.update(capture(conditionSlot)) }
    val capturedCode = conditionSlot.captured.clinicalStatus.coding.first()
    Assert.assertEquals("37793d31-def5-40bd-a2e3-fdaf5a0ddc53", conditionSlot.captured.id)
    Assert.assertEquals(DefaultRepository.PATIENT_CONDITION_RESOLVED_CODE, capturedCode.code)
    Assert.assertEquals(DefaultRepository.SNOMED_SYSTEM, capturedCode.system)
    Assert.assertEquals(DefaultRepository.PATIENT_CONDITION_RESOLVED_DISPLAY, capturedCode.display)
  }

  @Test
  fun `createRemote() should correctly invoke FhirEngine#createRemote`() {
    val resource = spyk(Patient())
    coEvery { fhirEngine.create(resource, isLocalOnly = true) } returns listOf(resource.id)

    runBlocking { defaultRepository.createRemote(false, resource) }

    coVerify { fhirEngine.create(resource, isLocalOnly = true) }
  }
}
