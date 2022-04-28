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

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.rest.gclient.IParam
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.filter.StringParamFilterCriterion
import com.google.android.fhir.search.filter.TokenParamFilterCriterion
import com.google.android.fhir.sync.ResourceSyncParams
import com.google.android.fhir.sync.State
import com.google.android.fhir.sync.SyncJobImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkStatic
import java.util.Calendar
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.RelatedPerson
import org.junit.Assert
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

class ApplicationExtensionTest : RobolectricTest() {

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
    val tokenFilterParamCriterion: MutableList<Any> =
      ReflectionHelpers.getField(search, "tokenFilterCriteria")
    val tokenFilters: MutableList<TokenParamFilterCriterion> =
      ReflectionHelpers.getField(tokenFilterParamCriterion[0], "filters")
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
    val tokenFilterParamCriterion: MutableList<Any> =
      ReflectionHelpers.getField(search, "tokenFilterCriteria")
    val tokenFilters: MutableList<TokenParamFilterCriterion> =
      ReflectionHelpers.getField(tokenFilterParamCriterion[0], "filters")
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
  fun `FhirEngine#searchActivePatients() should call search and filter results by query when given query`() {
    val fhirEngine = mockk<FhirEngine>()
    val captureSlot = slot<Search>()
    val expectedPatientList = listOf<Patient>()
    coEvery { fhirEngine.search<Patient>(capture(captureSlot)) } returns expectedPatientList

    runBlocking { fhirEngine.searchActivePatients("be", 0, false) }

    coVerify { fhirEngine.search<Patient>(any()) }
    val search = captureSlot.captured
    val stringFilterParamCriterion: MutableList<Any> =
      ReflectionHelpers.getField(search, "stringFilterCriteria")
    val stringFilters: MutableList<StringParamFilterCriterion> =
      ReflectionHelpers.getField(stringFilterParamCriterion[0], "filters")
    Assert.assertEquals(Patient.NAME, stringFilters[0].parameter)
    Assert.assertEquals("be", stringFilters[0].value)
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
    Assert.assertEquals(PaginationConstant.DEFAULT_PAGE_SIZE, search.count)
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
    Assert.assertEquals(PaginationConstant.DEFAULT_PAGE_SIZE, search.count)
    Assert.assertEquals(PaginationConstant.DEFAULT_PAGE_SIZE * 3, search.from)
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
    val tokenFilterParamCriterion: MutableList<Any> =
      ReflectionHelpers.getField(search, "tokenFilterCriteria")
    val tokenFilters: MutableList<TokenParamFilterCriterion> =
      ReflectionHelpers.getField(tokenFilterParamCriterion[0], "filters")
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

  @Test
  fun `Context#loadResourceTemplate()`() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val dateOnset = DateType(Calendar.getInstance().time).format()
    val conditionData =
      mapOf(
        "#Id" to UUID.randomUUID().toString(),
        "#RefPatient" to "Patient/88a98d-234",
        "#RefCondition" to "Condition/ref-condition-id",
        "#RefEpisodeOfCare" to "EpisodeOfCare/ref-episode-of-case",
        "#RefEncounter" to "Encounter/ref-encounter-id",
        "#RefGoal" to "Goal/ref-goal-id",
        "#RefCareTeam" to "CareTeam/325",
        "#RefPractitioner" to "Practitioner/399",
        "#RefDateOnset" to dateOnset
      )

    val condition =
      context.loadResourceTemplate(
        "pregnancy_condition_template.json",
        Condition::class.java,
        conditionData
      )

    Assert.assertEquals(conditionData["#Id"], condition.logicalId)
    Assert.assertEquals(conditionData["#RefPatient"], condition.subject.reference)
    Assert.assertEquals(
      conditionData["#RefDateOnset"],
      condition.onsetDateTimeType.toHumanDisplay()
    )
  }

  @Test
  fun `FhirEngine#dateTimeTypeFormat()`() {
    val dateTimeTypeObject = Calendar.getInstance()
    dateTimeTypeObject.set(Calendar.YEAR, 2010)
    dateTimeTypeObject.set(Calendar.MONTH, 1)
    dateTimeTypeObject.set(Calendar.DAY_OF_YEAR, 1)
    // dateTimeTypeObject.set(Calendar.HOUR, 1)
    // dateTimeTypeObject.set(Calendar.MINUTE, 1)
    // dateTimeTypeObject.set(Calendar.MILLISECOND, 1)
    // dateTimeTypeObject.set(Calendar.ZONE_OFFSET, 1)
    // dateTimeTypeObject.set(Calendar.DST_OFFSET, 1)
    val expectedDateTimeFormat = "2010-01-01"
    Assert.assertEquals(
      expectedDateTimeFormat,
      DateTimeType(dateTimeTypeObject).format().split("T")[0]
    )
  }

  @Test
  fun `FhirEngine#loadCqlLibraryBundle()`() {

    val context = ApplicationProvider.getApplicationContext<Application>()
    val fhirEngine = mockk<FhirEngine>()
    val fhirOperatorDecorator: FhirOperatorDecorator = mockk()
    val sharedPreferencesHelper: SharedPreferencesHelper = mockk()
    val measureResourceBundleUrl = "measure/ANCIND01-bundle.json"

    val prefsDataKey = SharedPreferencesHelper.MEASURE_RESOURCES_LOADED
    every { sharedPreferencesHelper.read(prefsDataKey, any<String>()) } returns ""
    every { sharedPreferencesHelper.write(prefsDataKey, any<String>()) } returns Unit
    coEvery { fhirOperatorDecorator.loadLib(any()) } returns Unit
    coEvery { fhirEngine.save(any()) } returns Unit

    runBlocking {
      fhirEngine.loadCqlLibraryBundle(
        context = context,
        fhirOperator = fhirOperatorDecorator,
        sharedPreferencesHelper = sharedPreferencesHelper,
        resourcesBundlePath = measureResourceBundleUrl
      )
    }

    Assert.assertNotNull(sharedPreferencesHelper.read(prefsDataKey, ""))
  }
}
