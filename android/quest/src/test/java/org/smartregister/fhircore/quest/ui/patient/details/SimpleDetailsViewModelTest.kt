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

import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.quest.configuration.view.Properties
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

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
    val config =
      ConfigurationRegistry(ApplicationProvider.getApplicationContext(), mockk(), mockk()).apply {
        appId = "g6pd"
      }
    config.loadAppConfigurations("g6pd", mockk(relaxed = true), {})

    every { patientRepository.configurationRegistry } returns config
    coEvery { patientRepository.loadEncounter(any()) } returns
      Encounter().apply { id = encounterId }

    coEvery { viewModel.getCondition(any(), any()) } returns
      Condition().apply { code.addCoding(Coding("s", "c", "d")) }
    coEvery { viewModel.getObservation(any(), any()) } returns
      Observation().apply { value = StringType("1234") }

    viewModel.loadData(encounterId)

    coVerify { patientRepository.loadEncounter(any()) }
    coVerify { viewModel.getCondition(any(), any()) }
    coVerify { viewModel.getObservation(any(), any()) }
  }

  @Test
  fun testGetConditionShouldReturnValidCondition() = runBlockingTest {
    val fhirEngine = mockk<FhirEngine>()
    coEvery { patientRepository.fhirEngine } returns fhirEngine
    coEvery { fhirEngine.search<Condition>(any()) } returns listOf(Condition().apply { id = "c1" })

    val result =
      viewModel.getCondition(
        Encounter().apply { id = "123" },
        filterOf("code", "Code", Properties())
      )

    coVerify { fhirEngine.search<Condition>(any()) }

    Assert.assertEquals("c1", result!!.logicalId)
  }

  @Test
  fun testGetObservationShouldReturnValidObservation() = runBlockingTest {
    val fhirEngine = mockk<FhirEngine>()
    coEvery { patientRepository.fhirEngine } returns fhirEngine
    coEvery { fhirEngine.search<Observation>(any()) } returns
      listOf(Observation().apply { id = "o1" })

    val result =
      viewModel.getObservation(
        Encounter().apply { id = "123" },
        filterOf("code", "Code", Properties())
      )

    coVerify { fhirEngine.search<Observation>(any()) }

    Assert.assertEquals("o1", result!!.logicalId)
  }

  @Test
  fun testOnBackPressed() {
    viewModel.onBackPressed(true)
    Assert.assertNotNull(viewModel.onBackPressClicked.value)
    Assert.assertTrue(viewModel.onBackPressClicked.value!!)
  }
}
