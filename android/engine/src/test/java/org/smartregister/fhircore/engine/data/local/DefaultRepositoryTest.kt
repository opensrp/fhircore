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

package org.smartregister.fhircore.engine.data.local

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Composition
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
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.generateMissingId
import org.smartregister.fhircore.engine.util.extension.loadResource

class DefaultRepositoryTest : RobolectricTest() {

  private val dispatcherProvider = DefaultDispatcherProvider()

  @Test
  fun `loadResource() should get resource using id`() {
    val samplePatientId = "12345"
    val samplePatient: Patient = Patient().apply { id = samplePatientId }

    val fhirEngine: FhirEngine = mockk()
    coEvery { fhirEngine.get<Patient>(any()) } answers { samplePatient }

    val defaultRepository =
      DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = dispatcherProvider)

    runBlocking {
      val actualPatient = defaultRepository.loadResource<Patient>(samplePatientId)
      Assert.assertEquals("12345", actualPatient?.id)
    }

    coVerify { fhirEngine.get<Patient>("12345") }
  }

  @Test
  fun `searchResourceFor(reference) should search CarePlan that is related to a Patient using patientId`() {
    val samplePatientId = "12345"
    val fhirEngine: FhirEngine = mockk()
    coEvery { fhirEngine.search<CarePlan> {} } returns listOf(mockk())

    val defaultRepository =
      DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = dispatcherProvider)

    runBlocking {
      val actualCarePlans =
        defaultRepository.searchResourceFor<CarePlan>(
          subjectId = samplePatientId,
          subjectType = ResourceType.Patient,
          subjectParam = CarePlan.SUBJECT
        )
      Assert.assertEquals(1, actualCarePlans.size)
    }

    coVerify { fhirEngine.search<CarePlan> {} }
  }

  @Test
  fun `searchResourceFor(token) should return 1 Patient using patientId`() {
    val samplePatientId = "12345"
    val fhirEngine: FhirEngine = mockk()
    coEvery { fhirEngine.search<Patient> {} } returns listOf(mockk())

    val defaultRepository =
      DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = dispatcherProvider)

    runBlocking {
      val actualPatients =
        defaultRepository.searchResourceFor<Patient>(
          token = Patient.RES_ID,
          subjectId = samplePatientId,
          subjectType = ResourceType.Patient
        )
      Assert.assertEquals(1, actualPatients.size)
    }

    coVerify { fhirEngine.search<Patient> {} }
  }

  @Test
  fun `search() should return 1 Condition given Condition type DataRequirement`() {
    val fhirEngine: FhirEngine = mockk()
    coEvery { fhirEngine.search<Condition> {} } returns listOf(mockk())

    val defaultRepository =
      DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = dispatcherProvider)

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
  fun `addOrUpdate() should call fhirEngine#update when resource exists`() {
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

    val fhirEngine: FhirEngine = mockk()
    coEvery { fhirEngine.get(any(), any()) } answers { patient }
    coEvery { fhirEngine.update(any()) } just runs

    val defaultRepository =
      DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = dispatcherProvider)

    // Call the function under test
    runBlocking { defaultRepository.addOrUpdate(patient) }

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
    runBlocking { defaultRepository.addOrUpdate(Patient()) }
    coVerify(exactly = 1) { fhirEngine.create(any()) }
  }

  @Test
  fun `save() should call Resource#generateMissingId()`() {
    mockkStatic(Resource::generateMissingId)
    val resource = spyk(Patient())

    val fhirEngine: FhirEngine = mockk()
    coEvery { fhirEngine.get(ResourceType.Patient, any()) } throws
      ResourceNotFoundException("Exce", "Exce")
    coEvery { fhirEngine.create(any()) } returns listOf()
    val defaultRepository =
      DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = dispatcherProvider)

    runBlocking { defaultRepository.save(resource) }

    verify { resource.generateMissingId() }

    unmockkStatic(Resource::generateMissingId)
  }

  @Test
  fun `addOrUpdate() should call Resource#generateMissingId() when ResourceId is null`() {
    mockkStatic(Resource::generateMissingId)
    val resource = Patient()

    val fhirEngine: FhirEngine = mockk()
    coEvery { fhirEngine.get(ResourceType.Patient, any()) } throws
      ResourceNotFoundException("Exce", "Exce")
    coEvery { fhirEngine.create(any()) } returns listOf()
    val defaultRepository =
      DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = dispatcherProvider)

    runBlocking { defaultRepository.addOrUpdate(resource) }

    verify { resource.generateMissingId() }

    unmockkStatic(Resource::generateMissingId)
  }

  @Test
  fun testSearchCompositionByIdentifier() = runBlockingTest {
    val fhirEngine: FhirEngine = mockk()
    coEvery { fhirEngine.search<Composition>(any()) } returns
      listOf(Composition().apply { id = "123" })

    val defaultRepository =
      DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = dispatcherProvider)

    val result = defaultRepository.searchCompositionByIdentifier("appId")

    coVerify { fhirEngine.search<Composition>(any()) }

    Assert.assertEquals("123", result!!.logicalId)
  }

  @Test
  fun testGetBinaryResource() = runBlockingTest {
    val fhirEngine: FhirEngine = mockk()
    coEvery { fhirEngine.get(ResourceType.Binary, any()) } returns Binary().apply { id = "111" }

    val defaultRepository =
      DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = dispatcherProvider)

    val result = defaultRepository.getBinary("111")

    coVerify { fhirEngine.get(ResourceType.Binary, any()) }

    Assert.assertEquals("111", result.logicalId)
  }

  @Test
  fun testLoadManagingEntity_shouldReturn_patient() {
    val fhirEngine: FhirEngine = mockk()
    val defaultRepository =
      DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = dispatcherProvider)

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
  fun testChangeManagingEntity_shouldVerifyFhirEngineCalls() {
    val fhirEngine: FhirEngine = mockk()
    val defaultRepository =
      DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = dispatcherProvider)

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
  fun testRemoveGroup_givenGroupAlreadyDeleted_shouldThrow_IllegalStateException() {
    val fhirEngine: FhirEngine = mockk()
    val defaultRepository =
      DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = dispatcherProvider)

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
}
