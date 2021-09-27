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
import com.google.android.fhir.search.search
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.data.anc.AncPatientRepository
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.family.register.FamilyItemMapper

class FamilyRepositoryTest : RobolectricTest() {

  private lateinit var repository: FamilyRepository
  private lateinit var fhirEngine: FhirEngine

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    fhirEngine = spyk()
    repository = FamilyRepository(fhirEngine, FamilyItemMapper)
  }

  @Test
  fun testLoadAllShouldReturnListOfFamilyItem() {
    val patients =
      listOf(buildPatient("1111", "Family1", "Given1"), buildPatient("2222", "Family2", "Given2"))

    val ancRepository = mockk<AncPatientRepository>()
    ReflectionHelpers.setField(
      FamilyRepository::class.java,
      repository,
      "ancPatientRepository",
      ancRepository
    )

    coEvery { ancRepository.searchCarePlan(any()) } returns emptyList()
    coEvery { fhirEngine.search<Patient>(any()) } returns patients
    coEvery { fhirEngine.count(any()) } returns 10

    runBlocking {
      val families = repository.loadData("", 0, true)

      Assert.assertEquals("Given1 Family1", families[0].name)
      Assert.assertEquals("1111", families[0].id)

      Assert.assertEquals("Given2 Family2", families[1].name)
      Assert.assertEquals("2222", families[1].id)
    }
  }

  private fun buildPatient(id: String, family: String, given: String): Patient {
    return Patient().apply {
      this.id = id
      this.addName().apply {
        this.family = family
        this.given.add(StringType(given))
      }
    }
  }
}
