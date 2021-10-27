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

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.fhir.FhirEngine
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PositiveIntType
import org.hl7.fhir.r4.model.StringType
import org.junit.Before
import org.junit.Rule
import org.smartregister.fhircore.eir.coroutine.CoroutineTestRule
import org.smartregister.fhircore.eir.data.PatientRepository
import org.smartregister.fhircore.eir.data.model.PatientItem
import org.smartregister.fhircore.eir.ui.patient.register.PatientItemMapper

@ExperimentalCoroutinesApi
internal class AdverseEventViewModelTest {
  private lateinit var fhirEngine: FhirEngine

  private lateinit var ancDetailsViewModel: AdverseEventViewModel

  private lateinit var patientRepository: PatientRepository

  private val patientId = "samplePatientId"

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)

    fhirEngine = mockk(relaxed = true)
    patientRepository = mockk()

    val ancPatientDetailItem = spyk<PatientItemMapper>()

    every { ancPatientDetailItem.mapToDomainModel(Pair(getPatient(), getImmunizations())) } returns
      PatientItem("samplePatientId", "Mandela Nelson", "M", "0")

    ancDetailsViewModel = spyk(AdverseEventViewModel(Application(), patientRepository))
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
