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

package org.smartregister.fhircore.eir.ui.patient.details

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.eir.coroutine.CoroutineTestRule
import org.smartregister.fhircore.eir.data.PatientRepository
/*
@ExperimentalCoroutinesApi
internal class PatientDetailsViewModelTest {
  private lateinit var patientRepository: PatientRepository

  private lateinit var patientDetailsViewModel: PatientDetailsViewModel

  private val patientId = "samplePatientId"

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    patientRepository = mockk(relaxed = true)
    patientDetailsViewModel = spyk(PatientDetailsViewModel(patientRepository = patientRepository))
  }

  @Test
  fun fetchDemographics() {
    coroutinesTestRule.runBlockingTest {
      val patient = spyk<Patient>().apply { idElement.id = patientId }
      coEvery { patientRepository.fetchDemographics(patientId) } returns patient
      patientDetailsViewModel.fetchDemographics(patientId)
      Assert.assertNotNull(patientDetailsViewModel.patientDemographics.value)
      Assert.assertNotNull(patientDetailsViewModel.patientDemographics.value!!.idElement)
      Assert.assertEquals(
        patientDetailsViewModel.patientDemographics.value?.idElement!!.id,
        patientId
      )
    }
  }

  @Test
  fun fetchImmunizations() {
    coroutinesTestRule.runBlockingTest {
      val immunizations = listOf(mockk<Immunization>())

      coEvery { patientRepository.getPatientImmunizations(patientId) } returns immunizations

      patientDetailsViewModel.fetchImmunizations(patientId)
      Assert.assertNotNull(patientDetailsViewModel.patientImmunizations.value)
      Assert.assertEquals(patientDetailsViewModel.patientImmunizations.value?.size, 1)
    }
  }
}*/
