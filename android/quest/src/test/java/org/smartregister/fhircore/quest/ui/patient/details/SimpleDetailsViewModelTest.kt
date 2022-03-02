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

package org.smartregister.fhircore.quest.ui.patient.details

import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.MedicationRequest
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.configuration.view.Code
import org.smartregister.fhircore.quest.configuration.view.Filter
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.util.FhirPathUtil.doesSatisfyFilter

@HiltAndroidTest
class SimpleDetailsViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @BindValue val patientRepository: PatientRepository = mockk()

  private val encounterId = "123456"

  private lateinit var viewModel: SimpleDetailsViewModel

  @Before
  fun setUp() {
    hiltRule.inject()
    viewModel = spyk(SimpleDetailsViewModel(patientRepository = patientRepository))
  }

  @Test
  fun testLoadData() = runBlockingTest {
    coEvery { patientRepository.configurationRegistry } returns
      Faker.buildTestConfigurationRegistry("g6pd", mockk())
    coEvery { patientRepository.loadEncounter(any()) } returns
      Encounter().apply { id = encounterId }

    coEvery { viewModel.getDataMap(any()) } returns
      mutableMapOf(
        Enumerations.ResourceType.OBSERVATION to listOf(Observation().apply { id = "o1" }),
        Enumerations.ResourceType.CONDITION to listOf(Condition().apply { id = "c1" }),
        Enumerations.ResourceType.MEDICATIONREQUEST to
          listOf(MedicationRequest().apply { id = "mr1" }),
      )

    viewModel.loadData(encounterId)

    coVerify { patientRepository.loadEncounter(any()) }
    coVerify { viewModel.getDataMap(any()) }
  }

  @Test
  fun testGetDataMap() = runBlockingTest {
    coEvery { patientRepository.getCondition(any(), any()) } returns
      listOf(Condition().apply { code.addCoding(Coding("s", "c", "d")) })
    coEvery { patientRepository.getObservation(any(), any()) } returns
      listOf(Observation().apply { value = StringType("1234") })
    coEvery { patientRepository.getMedicationRequest(any(), any()) } returns
      listOf(
        MedicationRequest().apply {
          this.intent = MedicationRequest.MedicationRequestIntent.FILLERORDER
        }
      )
    coEvery { viewModel.getPatient(any()) } returns Patient()

    viewModel.getDataMap(
      Encounter().apply {
        id = "123"
        subject = Reference().apply { reference = "Encounter/123" }
      }
    )

    coVerify { patientRepository.getCondition(any(), any()) }
    coVerify { patientRepository.getObservation(any(), any()) }
    coVerify { patientRepository.getMedicationRequest(any(), any()) }
  }

  @Test
  fun testDoesSatisfiesFilterWithCorrectFilterShouldReturnValidTrue() = runBlockingTest {
    val filter =
      Filter(
        resourceType = Enumerations.ResourceType.OBSERVATION,
        key = "code",
        valueType = Enumerations.DataType.CODEABLECONCEPT,
        valueCoding = Code("http://a.b.com", "c1"),
        valuePrefix = null
      )

    val obs =
      Observation().apply {
        code = CodeableConcept().addCoding(Coding("http://a.b.com", "c1", "D1"))
      }

    Assert.assertTrue(doesSatisfyFilter(obs, filter)!!)
  }

  @Test
  fun testOnBackPressed() {
    viewModel.onBackPressed(true)
    Assert.assertNotNull(viewModel.onBackPressClicked.value)
    Assert.assertTrue(viewModel.onBackPressClicked.value!!)
  }
}
