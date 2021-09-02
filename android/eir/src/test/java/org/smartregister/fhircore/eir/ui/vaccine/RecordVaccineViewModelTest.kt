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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.PositiveIntType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.eir.coroutine.CoroutineTestRule
import org.smartregister.fhircore.eir.robolectric.RobolectricTest
import org.smartregister.fhircore.eir.shadow.EirApplicationShadow
import org.smartregister.fhircore.eir.data.PatientRepository
import org.smartregister.fhircore.eir.data.model.PatientVaccineSummary

@ExperimentalCoroutinesApi
@Config(shadows = [EirApplicationShadow::class])
internal class RecordVaccineViewModelTest : RobolectricTest() {

  private lateinit var recordVaccineViewModel: RecordVaccineViewModel

  private lateinit var patientRepository: PatientRepository

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    patientRepository = mockk()
    val immunization = spyk<Immunization>()
    every { immunization.protocolApplied } returns
      listOf(Immunization.ImmunizationProtocolAppliedComponent(PositiveIntType(1)))
    every { immunization.vaccineCode.coding } returns listOf(Coding("sys", "code", "disp"))
    coEvery { patientRepository.getPatientImmunizations(any()) } returns listOf(immunization)
    recordVaccineViewModel =
      spyk(RecordVaccineViewModel(ApplicationProvider.getApplicationContext(), patientRepository))
  }

  @Test
  fun testGetVaccineSummary() =
    coroutinesTestRule.runBlockingTest {
      val vaccineSummary = recordVaccineViewModel.getVaccineSummary("1")
      Assert.assertNotNull(vaccineSummary)

      val patientVaccineSummary = getLiveDataValue(vaccineSummary)
      Assert.assertNotNull(patientVaccineSummary)
      Assert.assertTrue(patientVaccineSummary is PatientVaccineSummary)
      Assert.assertEquals(1, patientVaccineSummary?.doseNumber)
      Assert.assertEquals("code", patientVaccineSummary?.initialDose)
    }
}
