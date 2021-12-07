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

package org.smartregister.fhircore.engine.util.extension

import ca.uhn.fhir.rest.gclient.IParam
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.TokenFilter
import com.google.android.fhir.sync.ResourceSyncParams
import com.google.android.fhir.sync.State
import com.google.android.fhir.sync.SyncJobImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.RelatedPerson
import org.junit.Assert
import org.junit.Test
import org.junit.jupiter.api.TestInstance
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 07-12-2021. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationExtensionTest {

  @Test
  fun `FhirEngine#loadPatientImmunizations() should return null when immunizations not found and ResourceNotFoundException is thrown`() {
    val fhirEngine = mockk<FhirEngine>()
    val patientId = "8912"

    coEvery { fhirEngine.search<Immunization>(any()) } throws
      ResourceNotFoundException(
        "resource not found",
        "immunizations for patient $patientId not found"
      )

    val immunizations: List<Immunization>?
    runBlocking { immunizations = fhirEngine.loadPatientImmunizations(patientId) }

    Assert.assertNull(immunizations)
  }

  @Test
  fun `FhirEngine#loadRelatedPersons() should return null when related persons not found and ResourceNotFoundException is thrown`() {
    val fhirEngine = mockk<FhirEngine>()
    val patientId = "8912"

    coEvery { fhirEngine.search<RelatedPerson>(any()) } throws
      ResourceNotFoundException(
        "resource not found",
        "RelatedPersons for patient $patientId not found"
      )

    val relatedPersons: List<RelatedPerson>?
    runBlocking { relatedPersons = fhirEngine.loadRelatedPersons(patientId) }

    Assert.assertNull(relatedPersons)
  }

  @Test
  fun `FhirEngine#loadResource() should call load and return resources`() {
    val fhirEngine = mockk<FhirEngine>()
    val patientId = "patient-john-doe"
    val patient2 = Patient().apply { id = patientId }

    coEvery { fhirEngine.load(Patient::class.java, patientId) } returns patient2

    val patient: Patient?
    runBlocking { patient = fhirEngine.loadResource(patientId) }

    coVerify { fhirEngine.load(Patient::class.java, patientId) }
    Assert.assertEquals(patient2, patient)
    Assert.assertEquals(patient2.id, patient!!.id)
  }

  @Test
  fun `FhirEngine#loadResource() should return null when resource not found and ResourceNotFoundException is thrown`() {
    val fhirEngine = mockk<FhirEngine>()
    val patientId = "patient-john-doe"
    coEvery { fhirEngine.load(Patient::class.java, patientId) } throws
      ResourceNotFoundException("Patient not found", "Patient with id $patientId was not found")

    val patient: Patient?
    runBlocking { patient = fhirEngine.loadResource(patientId) }

    coVerify { fhirEngine.load(Patient::class.java, patientId) }
    Assert.assertNull(patient)
  }

  @Test
  fun `FhirEngine#countActivePatients() should call count with filter Patient#active`() {
    val fhirEngine = mockk<FhirEngine>()
    val expectedPatientCount = 2398L
    val captureSlot = slot<Search>()
    coEvery { fhirEngine.count(capture(captureSlot)) } returns expectedPatientCount

    val patientsCount: Long?
    runBlocking { patientsCount = fhirEngine.countActivePatients() }

    coVerify { fhirEngine.count(any()) }
    val search = captureSlot.captured
    val tokenFilters: MutableList<TokenFilter> = ReflectionHelpers.getField(search, "tokenFilters")
    Assert.assertEquals(Patient.ACTIVE, tokenFilters[0].parameter)
    Assert.assertEquals(expectedPatientCount, patientsCount)
  }

  @Test
  fun `FhirEngine#searchActivePatients() should call search with filter Patient#active`() {
    val fhirEngine = mockk<FhirEngine>()
    val captureSlot = slot<Search>()
    val patient1 = Patient().apply { id = "patient-john-doe" }
    val patient2 = Patient().apply { id = "patient-mary-joe" }
    val expectedPatientList = listOf(patient1, patient2)
    coEvery { fhirEngine.search<Patient>(capture(captureSlot)) } returns expectedPatientList

    val patientsList: List<Patient>
    runBlocking { patientsList = fhirEngine.searchActivePatients("", 0, false) }

    coVerify { fhirEngine.search<Patient>(any()) }
    val search = captureSlot.captured
    val tokenFilters: MutableList<TokenFilter> = ReflectionHelpers.getField(search, "tokenFilters")
    Assert.assertEquals(Patient.ACTIVE, tokenFilters[0].parameter)
    Assert.assertEquals(expectedPatientList, patientsList)
  }

  @Test
  fun `FhirEngine#searchActivePatients() should call search and sort results by name`() {
    val fhirEngine = mockk<FhirEngine>()
    val captureSlot = slot<Search>()
    val expectedPatientList = listOf<Patient>()
    coEvery { fhirEngine.search<Patient>(capture(captureSlot)) } returns expectedPatientList

    runBlocking { fhirEngine.searchActivePatients("", 0, false) }

    coVerify { fhirEngine.search<Patient>(any()) }
    val search = captureSlot.captured
    Assert.assertEquals(Patient.NAME, ReflectionHelpers.getField(search, "sort") as IParam)
    Assert.assertEquals(Order.ASCENDING, ReflectionHelpers.getField(search, "order") as Order)
  }

  @Test
  fun `FhirEngine#searchActivePatients() should call search and set correct count when page is 0`() {
    val fhirEngine = mockk<FhirEngine>()
    val captureSlot = slot<Search>()
    val expectedPatientList = listOf<Patient>()
    coEvery { fhirEngine.search<Patient>(capture(captureSlot)) } returns expectedPatientList

    runBlocking { fhirEngine.searchActivePatients("", 0, false) }

    coVerify { fhirEngine.search<Patient>(any()) }
    val search = captureSlot.captured
    Assert.assertEquals(PaginationUtil.DEFAULT_PAGE_SIZE, search.count)
    Assert.assertEquals(0, search.from)
  }

  @Test
  fun `FhirEngine#searchActivePatients() should call search and set correct count when page is 3`() {
    val fhirEngine = mockk<FhirEngine>()
    val captureSlot = slot<Search>()
    val expectedPatientList = listOf<Patient>()
    coEvery { fhirEngine.search<Patient>(capture(captureSlot)) } returns expectedPatientList

    runBlocking { fhirEngine.searchActivePatients("", 3, false) }

    coVerify { fhirEngine.search<Patient>(any()) }
    val search = captureSlot.captured
    Assert.assertEquals(PaginationUtil.DEFAULT_PAGE_SIZE, search.count)
    Assert.assertEquals(PaginationUtil.DEFAULT_PAGE_SIZE * 3, search.from)
  }

  @Test
  fun `FhirEngine#searchActivePatients() should call search and countActivePatients with filter Patient#active when param loadAll is true`() {
    mockkStatic(FhirEngine::countActivePatients)

    val fhirEngine = mockk<FhirEngine>()
    val captureSlot = slot<Search>()
    val patient1 = Patient().apply { id = "patient-john-doe" }
    val patient2 = Patient().apply { id = "patient-mary-joe" }
    val expectedPatientList = listOf(patient1, patient2)
    coEvery { fhirEngine.search<Patient>(capture(captureSlot)) } returns expectedPatientList
    coEvery { fhirEngine.countActivePatients() } returns 923L

    val patientsList: List<Patient>
    runBlocking { patientsList = fhirEngine.searchActivePatients("", 0, true) }

    coVerify { fhirEngine.search<Patient>(any()) }
    coVerify { fhirEngine.countActivePatients() }
    val search = captureSlot.captured
    val tokenFilters: MutableList<TokenFilter> = ReflectionHelpers.getField(search, "tokenFilters")
    Assert.assertEquals(Patient.ACTIVE, tokenFilters[0].parameter)
    Assert.assertEquals(expectedPatientList, patientsList)

    unmockkStatic(FhirEngine::countActivePatients)
  }

  @Test
  fun `FhirEngine#runOneTimeSync() should call syncJob#run() with params`() {
    val fhirEngine = mockk<FhirEngine>()
    val syncJob = spyk(SyncJobImpl(mockk()))
    val sharedSyncStatus = mockk<MutableSharedFlow<State>>()
    val resourceSyncParams = spyk<ResourceSyncParams>()
    val fhirResourceDataSource = spyk(FhirResourceDataSource(mockk()))

    coEvery { syncJob.run(any(), any(), any(), any()) } returns mockk()

    runBlocking {
      fhirEngine.runOneTimeSync(
        sharedSyncStatus,
        syncJob,
        resourceSyncParams,
        fhirResourceDataSource
      )
    }

    coVerify {
      syncJob.run(fhirEngine, fhirResourceDataSource, resourceSyncParams, sharedSyncStatus)
    }
  }
}
