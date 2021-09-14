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

package org.smartregister.fhircore.eir.data

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Search
import io.mockk.coEvery
import io.mockk.mockk
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.PositiveIntType
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.eir.data.model.VaccineStatus
import org.smartregister.fhircore.eir.robolectric.RobolectricTest
import org.smartregister.fhircore.eir.shadow.TestUtils.TEST_PATIENT_1
import org.smartregister.fhircore.eir.ui.patient.register.PatientItemMapper

class PatientRepositoryTest : RobolectricTest() {

  private lateinit var patientRepository: PatientRepository
  private lateinit var fhirEngine: FhirEngine

  @Before
  fun setUp() {
    fhirEngine = mockk()

    coEvery {
      hint(Resource::class)
      fhirEngine.search<Resource>(any())
    } answers
      {
        when (firstArg<Search>().type) {
          ResourceType.Patient -> listOf(TEST_PATIENT_1)
          ResourceType.Immunization -> getImmunizationList()
          else -> listOf()
        }
      }

    patientRepository = PatientRepository(fhirEngine, PatientItemMapper)
  }

  @Test
  fun testLoadDataShouldReturnListOfPatients() {

    runBlocking {
      val patientItems = patientRepository.loadData("", 1, false)

      Assert.assertEquals(1, patientItems.size)
      with(patientItems.first()) {
        Assert.assertEquals("test_patient_1_id", patientIdentifier)
        Assert.assertEquals("Jane Mc", name)
        Assert.assertEquals("M", gender)
        Assert.assertEquals("0", age)
        Assert.assertEquals("Jane Mc, M, 0", demographics)
        Assert.assertEquals("2021-09-14", lastSeen)
        Assert.assertEquals(VaccineStatus.PARTIAL, vaccineStatus.status)
        Assert.assertEquals("14-09-21", vaccineStatus.date)
        Assert.assertEquals("", atRisk)
      }
    }
  }

  @Test
  fun testCountAllShouldReturnAsExpected() {
    coEvery { fhirEngine.count(any()) } returns 1
    runBlocking { Assert.assertEquals(1, patientRepository.countAll()) }
  }

  private fun getImmunizationList() =
    listOf(
      Immunization().apply {
        recorded = Date()
        vaccineCode =
          CodeableConcept().apply {
            this.text = "vaccineA"
            this.coding = listOf(Coding("", "vaccineA", "vaccineA"))
          }
        occurrence = DateTimeType.today()

        protocolApplied =
          listOf(
            Immunization.ImmunizationProtocolAppliedComponent().apply {
              doseNumber = PositiveIntType(1)
            }
          )
      }
    )
}
