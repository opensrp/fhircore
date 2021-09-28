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

package org.smartregister.fhircore.anc.ui.madx.details

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.fhir.FhirEngine
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Patient
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.madx.NonAncPatientRepository
import org.smartregister.fhircore.anc.data.madx.model.AncPatientDetailItem
import org.smartregister.fhircore.anc.data.madx.model.AncPatientItem

@ExperimentalCoroutinesApi
internal class NonAncDetailsViewModelTest {
  private lateinit var fhirEngine: FhirEngine

  private lateinit var patientDetailsViewModel: NonAncDetailsViewModel

  private lateinit var patientRepository: NonAncPatientRepository

  private val patientId = "samplePatientId"

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    fhirEngine = mockk(relaxed = true)
    patientRepository = mockk()

    val ancPatientDetailItem = spyk<AncPatientDetailItem>()

    every { ancPatientDetailItem.patientDetails } returns
      AncPatientItem(patientId, "Mandela Nelson", "M", "26")
    every { ancPatientDetailItem.patientDetailsHead } returns AncPatientItem()
    coEvery { patientRepository.fetchDemographics(patientId) } returns ancPatientDetailItem

    patientDetailsViewModel =
      spyk(
        NonAncDetailsViewModel(
          patientRepository,
          coroutinesTestRule.testDispatcherProvider,
          patientId
        )
      )
  }

  @Test
  fun fetchDemographics() {
    coroutinesTestRule.runBlockingTest {
      val patient = spyk<Patient>().apply { idElement.id = patientId }
      coEvery { fhirEngine.load(Patient::class.java, patientId) } returns patient
      val ancPatientDetailItem: AncPatientDetailItem =
        patientDetailsViewModel.fetchDemographics().value!!
      Assert.assertNotNull(ancPatientDetailItem)
      Assert.assertEquals(ancPatientDetailItem.patientDetails.patientIdentifier, patientId)
      val patientDetails =
        ancPatientDetailItem.patientDetails.name +
          ", " +
          ancPatientDetailItem.patientDetails.gender +
          ", " +
          ancPatientDetailItem.patientDetails.age
      val patientId =
        ancPatientDetailItem.patientDetailsHead.demographics +
          " ID: " +
          ancPatientDetailItem.patientDetails.patientIdentifier

      Assert.assertEquals(patientDetails, "Mandela Nelson, M, 26")
      Assert.assertEquals(patientId, " ID: samplePatientId")
    }
  }
}
