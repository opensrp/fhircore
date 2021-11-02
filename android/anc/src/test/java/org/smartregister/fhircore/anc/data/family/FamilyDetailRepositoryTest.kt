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

package org.smartregister.fhircore.anc.data.family

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.fhir.FhirEngine
import io.mockk.coEvery
import io.mockk.mockk
import java.util.Date
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

class FamilyDetailRepositoryTest : RobolectricTest() {

  private lateinit var repository: FamilyDetailRepository
  private lateinit var fhirEngine: FhirEngine

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    fhirEngine = mockk()
    repository = FamilyDetailRepository("", fhirEngine, CoroutineTestRule().testDispatcherProvider)
  }

  @Test
  fun testFetchDemographicsShouldReturnDummyPatient() {

    coEvery {
      hint(Patient::class)
      fhirEngine.load<Patient>(any(), any())
    } answers
      {
        Patient().apply {
          name =
            listOf(
              HumanName().apply {
                given = listOf(StringType("john"))
                family = "doe"
              }
            )
        }
      }

    val patient = repository.fetchDemographics().value

    Assert.assertEquals("john", patient?.name?.first()?.given?.first()?.value)
    Assert.assertEquals("doe", patient?.name?.first()?.family)
  }

  @Test
  fun testGetFamilyMembersShouldReturnDummyList() {

    coEvery {
      hint(Patient::class)
      fhirEngine.search<Patient>(any())
    } answers
      {
        listOf(
          Patient().apply {
            id = "1"
            name =
              listOf(
                HumanName().apply {
                  given = listOf(StringType("john"))
                  family = "doe"
                }
              )
            gender = Enumerations.AdministrativeGender.FEMALE
            birthDate = Date()
          }
        )
      }

    val items = repository.fetchFamilyMembers().value

    Assert.assertEquals(1, items?.count())

    Assert.assertEquals("John Doe", items?.get(0)?.name)
    Assert.assertEquals("1", items?.get(0)?.id)
    Assert.assertEquals("", items?.get(0)?.age)
    Assert.assertEquals("F", items?.get(0)?.gender)
    Assert.assertFalse(items?.get(0)?.pregnant!!)
  }

  @Test
  fun testGetEncountersShouldReturnSingleEncounterList() {

    coEvery {
      hint(Encounter::class)
      fhirEngine.search<Encounter>(any())
    } answers { listOf(Encounter().apply { class_ = Coding("", "", "first encounter") }) }

    val items = repository.fetchEncounters().value

    Assert.assertEquals(1, items?.size)
    Assert.assertEquals("first encounter", items?.get(0)?.class_?.display)
  }
}
