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

package org.smartregister.fhircore.engine.data.local

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import com.google.gson.Gson
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.DataRequirement
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.joda.time.LocalDate
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.generateMissingId
import org.smartregister.fhircore.engine.util.extension.loadResource

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class DefaultRepositoryTest : RobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)

  private val application = ApplicationProvider.getApplicationContext<Application>()
  @Inject lateinit var gson: Gson
  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
  @Inject lateinit var configService: ConfigService
  private lateinit var dispatcherProvider: DefaultDispatcherProvider
  private lateinit var fhirEngine: FhirEngine
  private lateinit var sharedPreferenceHelper: SharedPreferencesHelper
  private lateinit var defaultRepository: DefaultRepository

  @Before
  fun setUp() {
    hiltRule.inject()
    //    runBlocking { configurationRegistry.loadConfigurations("app/debug", application) }

    dispatcherProvider = DefaultDispatcherProvider()
    fhirEngine = mockk(relaxUnitFun = true)
    sharedPreferenceHelper = SharedPreferencesHelper(application, gson)
    defaultRepository =
      DefaultRepository(
        fhirEngine = fhirEngine,
        dispatcherProvider = dispatcherProvider,
        sharedPreferencesHelper = sharedPreferenceHelper,
        configurationRegistry = configurationRegistry,
        configService = configService
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
  fun searchResourceForGivenReferenceShouldSearchCarePlanThatIsRelatedToAPatientUsingId() {
    val samplePatientId = "12345"

    coEvery { fhirEngine.search<CarePlan> {} } returns listOf(mockk())

    runBlocking {
      val actualCarePlans =
        defaultRepository.searchResourceFor<CarePlan>(
          subjectId = samplePatientId,
          subjectType = ResourceType.Patient,
          subjectParam = CarePlan.SUBJECT,
          filters = emptyList()
        )
      Assert.assertEquals(1, actualCarePlans.size)
    }

    coVerify { fhirEngine.search<CarePlan> {} }
  }

  @Test
  fun searchResourceForGivenTokenShouldReturn1PatientUsingId() {
    val samplePatientId = "12345"

    coEvery { fhirEngine.search<Patient> {} } returns listOf(mockk())

    runBlocking {
      val actualPatients =
        defaultRepository.searchResourceFor<Patient>(
          token = Patient.RES_ID,
          subjectId = samplePatientId,
          subjectType = ResourceType.Patient,
          filters = emptyList()
        )
      Assert.assertEquals(1, actualPatients.size)
    }

    coVerify { fhirEngine.search<Patient> {} }
  }

  @Test
  fun searchShouldReturn1ConditionGivenConditionTypeDataRequirement() {

    coEvery { fhirEngine.search<Condition> {} } returns listOf(mockk())

    runBlocking {
      val actualPatients =
        defaultRepository.search(
          dataRequirement =
            DataRequirement().apply { type = Enumerations.ResourceType.CONDITION.toCode() }
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
            }
          )
        name =
          listOf(
            HumanName().apply {
              given = mutableListOf(StringType("Salman"))
              family = "Ali"
            }
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

    verify { resource.generateMissingId() }

    unmockkStatic(Resource::generateMissingId)
  }

  @Test
  fun loadManagingEntityShouldReturnPatient() {

    val group = Group().apply { managingEntity = Reference("RelatedPerson/12983") }

    val relatedPerson =
      RelatedPerson().apply {
        id = "12983"
        patient = Reference("Patient/12345")
      }
    coEvery { fhirEngine.search<RelatedPerson> {} } returns listOf(relatedPerson)

    val patient = Patient().apply { id = "12345" }
    coEvery { fhirEngine.search<Patient> {} } returns listOf(patient)

    runBlocking {
      val managingEntity = defaultRepository.loadManagingEntity(group)
      Assert.assertEquals("12345", managingEntity?.logicalId)
    }
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

    coEvery { fhirEngine.get<Patient>("54321") } returns patient

    coEvery { fhirEngine.create(any()) } returns listOf()

    val group =
      Group().apply {
        id = "73847"
        managingEntity = Reference("RelatedPerson/12983")
      }
    coEvery { fhirEngine.get<Group>("73847") } returns group

    coEvery { fhirEngine.update(any()) } just runs

    runBlocking {
      defaultRepository.changeManagingEntity(newManagingEntityId = "54321", groupId = "73847")
    }

    coVerify { fhirEngine.get<Patient>("54321") }

    coVerify { fhirEngine.create(any()) }

    coVerify { fhirEngine.get<Group>("73847") }

    coVerify { fhirEngine.update(any()) }
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
      runBlocking { defaultRepository.removeGroup(group.logicalId, false) }
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
          configService = mockk()
        )
      )
    coEvery { fhirEngine.search<RelatedPerson>(any()) } returns listOf(managingEntityRelatedPerson)
    coEvery { defaultRepositorySpy.delete(any()) } just runs
    coEvery { defaultRepositorySpy.addOrUpdate(resource = any()) } just runs
    val group =
      Group().apply {
        id = "testGroupId"
        active = true
        managingEntity = managingEntityRelatedPerson.asReference()
      }
    coEvery { fhirEngine.loadResource<Group>(group.id) } returns group

    defaultRepositorySpy.removeGroup(group.id, isDeactivateMembers = false)
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

    defaultRepositorySpy.removeGroupMember(memberId, null, patientMemberRep.resourceType.name)
    Assert.assertFalse(patientMemberRep.active)
    coVerify { defaultRepositorySpy.addOrUpdate(resource = patientMemberRep) }
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
          configService = mockk()
        )
      )

    defaultRepository.delete(resourceType = "Patient", resourceId = "123")

    coVerify { fhirEngine.delete(any<ResourceType>(), any<String>()) }
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
      listOf(relatedPerson) as List<RelatedPerson>
    coEvery { defaultRepository.delete(any()) } just runs
    coEvery { defaultRepository.addOrUpdate(resource = any()) } just runs

    runBlocking { defaultRepository.removeGroup(groupId, true) }

    coVerify { defaultRepository.delete(relatedPerson) }
    coVerify { defaultRepository.addOrUpdate(resource = patient) }
    coVerify { defaultRepository.addOrUpdate(resource = group) }
  }

  @Test
  fun removeGroupMemberDeletesRelatedPersonAndUpdatesGroup() {
    defaultRepository = spyk(defaultRepository)
    val groupId = "73847"
    val memberId = "6745"
    val groupMemberResourceType: String = ResourceType.Patient.name
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
      listOf(relatedPerson) as List<RelatedPerson>
    coEvery { defaultRepository.delete(any()) } just runs
    coEvery { defaultRepository.addOrUpdate(resource = any()) } just runs

    runBlocking {
      defaultRepository.removeGroupMember(
        memberId = memberId,
        groupId = groupId,
        groupMemberResourceType = groupMemberResourceType
      )
    }

    coVerify { defaultRepository.delete(relatedPerson) }
    coVerify { defaultRepository.addOrUpdate(resource = group) }
  }
}
