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

package org.smartregister.fhircore.eir.ui.adverseevent

import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PositiveIntType
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.eir.coroutine.CoroutineTestRule
import org.smartregister.fhircore.eir.data.PatientRepository
import org.smartregister.fhircore.eir.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.ADVERSE_EVENT_IMMUNIZATION_ITEM_KEY
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider

@ExperimentalCoroutinesApi
internal class AdverseEventViewModelTest : RobolectricTest() {
  private lateinit var fhirEngine: FhirEngine

  private lateinit var adverseEventViewModel: AdverseEventViewModel

  private lateinit var patientRepository: PatientRepository

  private lateinit var defaultRepo: DefaultRepository

  private val patientId = "samplePatientId"

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    clearAllMocks()
    MockKAnnotations.init(this, relaxUnitFun = true)
    fhirEngine = mockk(relaxed = true)
    patientRepository = mockk()

    val immunization = spyk<Immunization>()
    every { immunization.protocolApplied } returns
      listOf(Immunization.ImmunizationProtocolAppliedComponent(PositiveIntType(1)))
    every { immunization.vaccineCode.coding } returns listOf(Coding("sys", "code", "disp"))
    coEvery { patientRepository.getPatientImmunizations(any()) } returns listOf(immunization)
    val dispatcher = mockk<DispatcherProvider>()
    every { dispatcher.io() } returns DefaultDispatcherProvider().main()

    defaultRepo = spyk(DefaultRepository(fhirEngine, DefaultDispatcherProvider()))
    adverseEventViewModel = spyk(AdverseEventViewModel(defaultRepo, patientRepository, dispatcher))
  }

  @Test
  fun testGetPatientImmunizations() =
    coroutinesTestRule.runBlockingTest {
      val immunizationList = adverseEventViewModel.getPatientImmunizations(patientId = patientId)
      Assert.assertNotNull(immunizationList)
      val liveImmunizationList = getLiveDataValue(immunizationList)
      Assert.assertNotNull(liveImmunizationList)
      Assert.assertTrue(liveImmunizationList is List<Immunization>)
      Assert.assertNotNull(liveImmunizationList?.isNotEmpty())
    }

  @Test
  fun testGetPopulationResourcesShouldReturnArrayOfImmunization() {
    val defaultRepo = mockk<DefaultRepository>()

    every { adverseEventViewModel.defaultRepository } returns defaultRepo
    coEvery { defaultRepo.loadImmunization("1") } returns Immunization().apply { id = "1" }

    val intent = Intent()
    intent.putExtra(ADVERSE_EVENT_IMMUNIZATION_ITEM_KEY, "1")

    runBlockingTest {
      val list = adverseEventViewModel.getPopulationResources(intent)
      Assert.assertEquals(1, list.size)
      Assert.assertEquals("1", (list[0] as Immunization).logicalId)
    }
  }

  @Test
  fun testLoadImmunizationShouldReturnLiveDataContainWithImmunization() {

    coEvery { fhirEngine.get(ResourceType.Immunization, "1") } returns
      Immunization().apply { id = "1" }
    val immunizationLiveData: Immunization? =
      getLiveDataValue(adverseEventViewModel.loadImmunization("1"))
    Assert.assertEquals("1", immunizationLiveData?.logicalId)
  }

  fun getPatient(): Patient {
    val patient =
      spyk<Patient>().apply {
        id = "samplePatientId"
        gender = Enumerations.AdministrativeGender.MALE
        name =
          listOf(HumanName().setFamily("Mandela").setGiven(mutableListOf(StringType("Nelson"))))
        birthDate = Date()
      }
    return patient
  }

  fun getImmunizations(): List<Immunization> {
    val patient = getPatient()
    val immunization1 =
      spyk<Immunization>().apply {
        patientTarget = patient
        vaccineCode =
          CodeableConcept(Coding("system", "vaccine_code", "code display")).setText("Astrazeneca")
        protocolApplied =
          listOf(Immunization.ImmunizationProtocolAppliedComponent(PositiveIntType(1)))
        occurrence = DateTimeType("2021-07-30")
      }

    val immunization2 =
      spyk<Immunization>().apply {
        patientTarget = patient
        vaccineCode =
          CodeableConcept(Coding("system", "vaccine_code", "code display")).setText("Astrazeneca")
        protocolApplied =
          listOf(Immunization.ImmunizationProtocolAppliedComponent(PositiveIntType(2)))
        occurrence = DateTimeType("2021-07-30")
      }

    val immunization3 =
      spyk<Immunization>().apply {
        patientTarget = patient
        vaccineCode =
          CodeableConcept(Coding("system", "vaccine_code", "code display")).setText("Pfizer")
        protocolApplied =
          listOf(Immunization.ImmunizationProtocolAppliedComponent(PositiveIntType(2)))
        occurrence = DateTimeType("2021-07-30")
      }
    return listOf(immunization1, immunization2, immunization3)
  }
}
