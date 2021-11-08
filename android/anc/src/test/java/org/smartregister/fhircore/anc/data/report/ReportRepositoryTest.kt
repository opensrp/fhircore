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
import io.mockk.mockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
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
  fun testLoadReturnsPageWhenOnSuccessfulLoadOfItemKeyedData() = runBlockingTest {
    val report = getReport()
    coEvery {
      hint(ReportItem::class)
      fhirEngine.search<ReportItem>(any())
    } returns listOf(report)

    Assert.assertEquals(
      PagingSource.LoadResult.Page(
        listOf(
          ReportItem(
            report.id,
            report.identifier,
            report.title,
            report.description,
            report.reportType
          )
        ),
        null,
        1
      ),
      repository.load(PagingSource.LoadParams.Refresh(null, 1, false))
    )

    mockkObject(PagingSource.LoadResult.Error::class)

    report.reportType = "null"

    coEvery {
      hint(ReportItem::class)
      fhirEngine.search<ReportItem>(any())
    } returns listOf(report)

    val result = repository.load(PagingSource.LoadParams.Refresh(null, 1, false))

    Assert.assertTrue(result is PagingSource.LoadResult.Error)
    Assert.assertEquals(
      NullPointerException::class.simpleName,
      (result as PagingSource.LoadResult.Error).throwable.javaClass.simpleName
    )
  }

  private fun getReport(): ReportItem {
    return ReportItem().apply {
      id = "1"
      identifier = "1"
      title = "4+ ANC Contacts"
      description = "Women with at least four ANC contacts"
      reportType = "4"
    }
  }
}
