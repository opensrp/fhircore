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
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.logicalId
import com.google.android.fhir.workflow.FhirOperator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.util.Calendar
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

class ApplicationExtensionTest : RobolectricTest() {

  @Test
  fun `FhirEngine#loadResource() should call load and return resources`() {
    val fhirEngine = mockk<FhirEngine>()
    val patientId = "patient-john-doe"
    val patient2 = Patient().apply { id = patientId }

    coEvery { fhirEngine.get(ResourceType.Patient, patientId) } returns patient2

    val patient: Patient?
    runBlocking { patient = fhirEngine.loadResource(patientId) }

    coVerify { fhirEngine.get(ResourceType.Patient, patientId) }
    Assert.assertEquals(patient2, patient)
    Assert.assertEquals(patient2.id, patient!!.id)
  }

  @Test
  fun `FhirEngine#loadResource() should return null when resource not found and ResourceNotFoundException is thrown`() {
    val fhirEngine = mockk<FhirEngine>()
    val patientId = "patient-john-doe"
    coEvery { fhirEngine.get(ResourceType.Patient, patientId) } throws
      ResourceNotFoundException("Patient not found", "Patient with id $patientId was not found")

    val patient: Patient?
    runBlocking { patient = fhirEngine.loadResource(patientId) }

    coVerify { fhirEngine.get(ResourceType.Patient, patientId) }
    Assert.assertNull(patient)
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
    val fhirOperator: FhirOperator = mockk()
    val sharedPreferencesHelper: SharedPreferencesHelper = mockk()
    val measureResourceBundleUrl = "measure/ANCIND01-bundle.json"

    val prefsDataKey = SharedPreferencesHelper.MEASURE_RESOURCES_LOADED
    every { sharedPreferencesHelper.read(prefsDataKey, any<String>()) } returns ""
    every { sharedPreferencesHelper.write(prefsDataKey, any<String>()) } returns Unit
    coEvery { fhirOperator.loadLib(any()) } returns Unit
    coEvery { fhirEngine.create(any()) } returns listOf()

    runBlocking {
      fhirEngine.loadCqlLibraryBundle(
        context = context,
        fhirOperator = fhirOperator,
        sharedPreferencesHelper = sharedPreferencesHelper,
        resourcesBundlePath = measureResourceBundleUrl
      )
    }

    Assert.assertNotNull(sharedPreferencesHelper.read(prefsDataKey, ""))
  }
}
