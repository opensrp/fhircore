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

package org.smartregister.fhircore.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilter
import com.google.android.fhir.search.StringFilterModifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.RobolectricTest
import org.smartregister.fhircore.domain.currentPageNumber
import org.smartregister.fhircore.sdk.PatientExtended
import org.smartregister.fhircore.shadow.FhirApplicationShadow
import org.smartregister.fhircore.shadow.TestUtils.getOrAwaitValue

@Config(shadows = [FhirApplicationShadow::class])
class FamilyListViewModelTest : RobolectricTest() {

  private lateinit var viewModel: FamilyListViewModel

  private lateinit var fhirEngine: FhirEngine

  @get:Rule var instantExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    fhirEngine = mockk()

    mockkObject(FhirApplication)
    every { FhirApplication.fhirEngine(any()) } returns fhirEngine

    viewModel = spyk(FamilyListViewModel(ApplicationProvider.getApplicationContext(), fhirEngine))
  }

  @After
  fun cleanup() {
    unmockkObject(FhirApplication)
  }

  @Test
  fun testSearchResultsPatientsFilter() {
    coEvery { fhirEngine.search<Patient>(any()) } returns listOf()
    coEvery { fhirEngine.count(any()) } returns 0

    viewModel.searchResults("jane")

    viewModel.paginatedDataList.getOrAwaitValue()

    val search = slot<Search>()

    coVerify { fhirEngine.search<Patient>(capture(search)) }

    val filters = ReflectionHelpers.getField<List<StringFilter>>(search.captured, "stringFilters")

    Assert.assertEquals("Family", filters[0].value)
    Assert.assertEquals(StringFilterModifier.CONTAINS, filters[0].modifier)
    Assert.assertEquals(PatientExtended.TAG, filters[0].parameter)
  }

  @Test
  fun testSearchResultsPaginatedPatientsEmptyList() {
    coEvery { fhirEngine.search<Patient>(any()) } returns listOf()
    coEvery { fhirEngine.count(any()) } returns 0

    viewModel.searchResults("jane")

    val data = viewModel.paginatedDataList.getOrAwaitValue()

    val patients = data?.first
    val pagination = data?.second

    Assert.assertEquals(0, patients?.size)
    Assert.assertEquals(0, pagination?.totalItems)
    Assert.assertEquals(0, pagination?.currentPage)
    Assert.assertEquals(10, pagination?.pageSize)
  }

  @Test
  fun testSearchResultsPaginatedPatientsNonEmptyList() {
    coEvery { fhirEngine.search<Patient>(any()) } returns
      listOf(makePatient(), makePatient(), makePatient())
    coEvery { fhirEngine.count(any()) } returns 3

    viewModel.searchResults("jane")

    val data = viewModel.paginatedDataList.getOrAwaitValue()

    val patients = data?.first
    val pagination = data?.second

    Assert.assertEquals(3, patients?.size)
    Assert.assertEquals(3, pagination?.totalItems)
    Assert.assertEquals(1, pagination?.currentPageNumber())
    Assert.assertEquals(10, pagination?.pageSize)
  }

  private fun makePatient(): Patient {
    return Patient().apply {
      this.id = "123456"
      name.add(HumanName().apply { this.family = "abc" })
    }
  }
}
