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
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.generateMissingId
import org.smartregister.fhircore.engine.util.extension.loadPatientImmunizations
import org.smartregister.fhircore.engine.util.extension.loadRelatedPersons

class DefaultRepositoryTest : RobolectricTest() {

  private val dispatcherProvider = spyk(DefaultDispatcherProvider())

  @Test
  fun `addOrUpdate() should call fhirEngine#update when resource exists`() {
    val patientId = "15672-9234"
    val patient =
      Patient().apply {
        id = patientId
        active = true
        birthDate = Date(1996, 8, 17)
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
    coEvery { fhirEngine.load(Patient::class.java, patient.idElement.idPart) } returns patient
    coEvery { fhirEngine.update(any()) } just runs

    val defaultRepository =
      DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = dispatcherProvider)

    // Call the function under test
    runBlocking { defaultRepository.addOrUpdate(patient) }

    coVerify { fhirEngine.load(Patient::class.java, patientId) }
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
    coEvery { fhirEngine.load(Patient::class.java, any()) } throws
      mockk<ResourceNotFoundException>()
    coEvery { fhirEngine.save(any()) } returns Unit
    runBlocking { defaultRepository.addOrUpdate(Patient()) }
    coVerify(exactly = 1) { fhirEngine.save(any()) }
  }

  @Test
  fun `loadRelatedPersons() should call FhirEngine#loadRelatedPersons`() {
    val patientId = "15672-9234"
    val fhirEngine: FhirEngine = mockk()
    coEvery { fhirEngine.loadRelatedPersons(patientId) } returns listOf()

    val defaultRepository =
      DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = dispatcherProvider)

    runBlocking { defaultRepository.loadRelatedPersons(patientId) }

    coVerify { fhirEngine.loadRelatedPersons(patientId) }
  }

  @Test
  fun `loadImmunizations() should call FhirEngine#loadImmunizations`() {
    val patientId = "15672-9234"
    val fhirEngine: FhirEngine = mockk()
    coEvery { fhirEngine.loadPatientImmunizations(patientId) } returns listOf()

    val defaultRepository =
      DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = dispatcherProvider)

    runBlocking { defaultRepository.loadPatientImmunizations(patientId) }

    coVerify { fhirEngine.loadPatientImmunizations(patientId) }
  }

  @Test
  fun `save() should call Resource#generateMissingId()`() {
    mockkStatic(Resource::generateMissingId)
    val resource = spyk(Patient())

    val fhirEngine: FhirEngine = mockk()
    coEvery { fhirEngine.load(Patient::class.java, any()) } throws
      ResourceNotFoundException("Exce", "Exce")
    coEvery { fhirEngine.save(any()) } just runs
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
    coEvery { fhirEngine.load(Patient::class.java, any()) } throws
      ResourceNotFoundException("Exce", "Exce")
    coEvery { fhirEngine.save(any()) } just runs
    val defaultRepository =
      DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = dispatcherProvider)

    runBlocking { defaultRepository.addOrUpdate(resource) }

    verify { resource.generateMissingId() }

    unmockkStatic(Resource::generateMissingId)
  }
}
