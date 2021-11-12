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

package org.smartregister.fhircore.anc.data.report

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.android.fhir.FhirEngine
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Encounter
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

@ExperimentalCoroutinesApi
class ReportRepositoryTest : RobolectricTest() {

  private lateinit var repository: ReportRepository
  private lateinit var fhirEngine: FhirEngine

  @Before
  fun setUp() {
    fhirEngine = mockk()
    repository = ReportRepository(fhirEngine, "")
  }

  @Test
  fun testGetRefreshKeyShouldReturnValidAnchorPosition() {
    val pagingState = PagingState<Int, ReportItem>(listOf(), 0, PagingConfig(1), 10)
    val anchorPosition = repository.getRefreshKey(pagingState)
    Assert.assertEquals(0, anchorPosition)
  }

  @Test
  //  @Ignore(
  //    "This test should be update as per FHIR resource update (Encounter -> Patient/any)" +
  //      "Davison will be working on reports items so we can take care for this with #675"
  //  )
  fun testLoadReturnsPageWhenOnSuccessfulLoadOfItemKeyedData() = runBlockingTest {
    val encounter1 = getTestEncounter1()
    val encounter2 = getTestEncounter2()
    val report1 = getTestReport1()
    val report2 = getTestReport2()
    coEvery {
      hint(Encounter::class)
      fhirEngine.search<Encounter>(any())
    } returns listOf(encounter1, encounter2)

    Assert.assertEquals(
      PagingSource.LoadResult.Page(
        listOf(
          ReportItem(report1.id, report1.title, report1.description, report1.reportType),
          ReportItem(report2.id, report2.title, report2.description, report2.reportType)
        ),
        null,
        null
      ),
      repository.load(PagingSource.LoadParams.Refresh(1, 1, false))
    )
  }

  private fun getTestReport1(): ReportItem {
    return ReportItem(
      id = "1",
      title = "First ANC",
      description = "Women having test reports encounters",
      reportType = "4"
    )
  }

  private fun getTestReport2(): ReportItem {
    return ReportItem(
      id = "2",
      title = "Received Supplements",
      description = "Women having test reports ANC",
      reportType = "4"
    )
  }

  private fun getTestEncounter1(): Encounter {
    return Encounter().apply { id = "1" }
  }

  private fun getTestEncounter2(): Encounter {
    return Encounter().apply { id = "2" }
  }
}
