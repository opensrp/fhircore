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

package org.smartregister.fhircore.eir.ui.vaccine

import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.fhir.FhirEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PositiveIntType
import org.hl7.fhir.r4.model.Resource
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.eir.coroutine.CoroutineTestRule
import org.smartregister.fhircore.eir.data.PatientRepository
import org.smartregister.fhircore.eir.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider

@ExperimentalCoroutinesApi
internal class RecordVaccineViewModelTest : RobolectricTest() {

  private lateinit var recordVaccineViewModel: RecordVaccineViewModel
  private lateinit var patientRepository: PatientRepository
  private lateinit var defaultRepository: DefaultRepository
  private lateinit var fhirEngine: FhirEngine
  @get:Rule var coroutinesTestRule = CoroutineTestRule()
  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    patientRepository = mockk()
    fhirEngine = mockk()
    defaultRepository = spyk(DefaultRepository(fhirEngine, DefaultDispatcherProvider()))

    val immunization =
      Immunization().apply {
        addProtocolApplied().doseNumber = PositiveIntType(1)
        vaccineCode.addCoding(Coding("sys", "code", "disp"))
        occurrence = DateTimeType.now()
      }
    coEvery { patientRepository.getPatientImmunizations(any()) } returns listOf(immunization)
    recordVaccineViewModel =
      spyk(
        RecordVaccineViewModel(
          fhirEngine,
          defaultRepository,
          mockk(),
          mockk(),
          patientRepository,
          DefaultDispatcherProvider(),
          mockk(),
          mockk()
        )
      )
  }

  @Test
  fun testGetVaccineSummaryShouldReturnValidData() = runBlockingTest {
    val patientVaccineSummary = recordVaccineViewModel.loadLatestVaccine("1")

    Assert.assertNotNull(patientVaccineSummary)
    Assert.assertEquals(1, patientVaccineSummary?.doseNumber)
    Assert.assertEquals("code", patientVaccineSummary?.initialDose)
  }

  @Test
  fun testGetVaccineSummaryShouldReturnNullWithMissingOccurence() = runBlockingTest {
    coEvery { patientRepository.getPatientImmunizations(any()) } returns listOf(Immunization())

    val patientVaccineSummary = recordVaccineViewModel.loadLatestVaccine("1")

    Assert.assertNull(patientVaccineSummary)
  }

  @Test
  fun `getPopulationResources() should call loadPatient() and loadPatientImmunization()`() {
    val patientId = "2892347"
    val patient = Patient()
    val immunization = Immunization()
    val intent = Intent()
    intent.putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, patientId)

    // coEvery { recordVaccineViewModel.loadPatient(patientId) } returns patient
    coEvery { fhirEngine.get(ResourceType.Patient, patientId) } returns patient
    coEvery { recordVaccineViewModel.loadPatientImmunization(patientId) } returns immunization

    val resources: Array<Resource>
    runBlocking { resources = recordVaccineViewModel.getPopulationResources(intent) }

    coVerify { fhirEngine.get(ResourceType.Patient, patientId) }
    coVerify { recordVaccineViewModel.loadPatientImmunization(patientId) }
    Assert.assertEquals(patient, resources[0])
    Assert.assertEquals(immunization, resources[1])
  }

  @Test
  fun `loadPatientImmunization() should call defaultRepository#loadPatientImmunizations`() {
    val patientId = "2892347"

    val defaultRepository: DefaultRepository = mockk()
    coEvery { defaultRepository.loadPatientImmunizations(patientId) } returns listOf()
    ReflectionHelpers.setField(recordVaccineViewModel, "defaultRepository", defaultRepository)

    runBlocking { recordVaccineViewModel.loadPatientImmunization(patientId) }

    coVerify { defaultRepository.loadPatientImmunizations(patientId) }
  }

  @Test
  fun `loadPatientImmunization() should return first immunization from defaultRepository#loadPatientImmunizations`() {
    val patientId = "2892347"
    val immunizationsList: List<Immunization> = listOf(Immunization(), Immunization())

    val defaultRepository: DefaultRepository = mockk(relaxed = true)
    coEvery { defaultRepository.loadPatientImmunizations(patientId) } returns immunizationsList
    ReflectionHelpers.setField(recordVaccineViewModel, "defaultRepository", defaultRepository)

    val actualImmunizations: Immunization
    runBlocking {
      actualImmunizations = recordVaccineViewModel.loadPatientImmunization(patientId)!!
    }

    Assert.assertEquals(immunizationsList[0], actualImmunizations)
  }
}
