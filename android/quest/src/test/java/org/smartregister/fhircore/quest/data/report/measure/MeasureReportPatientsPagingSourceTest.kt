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

package org.smartregister.fhircore.quest.data.report.measure

import androidx.paging.PagingSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportPatientViewData
import org.smartregister.fhircore.quest.util.mappers.MeasureReportPatientViewDataMapper

@OptIn(ExperimentalCoroutinesApi::class)
class MeasureReportPatientsPagingSourceTest {
  val measureReportRepository = spyk(MeasureReportRepository(mockk(), mockk(), mockk(), mockk()))
  val measureReportPatientViewDataMapper = spyk(MeasureReportPatientViewDataMapper(mockk()))
  val measureReportPatientData = mockk<RegisterData.AncRegisterData>()
  val measureReportPatientViewData = mockk<MeasureReportPatientViewData>()

  @Before
  fun setUp() {
    coEvery { measureReportRepository.retrievePatients(any()) } returns
      listOf(measureReportPatientData)
    every {
      measureReportPatientViewDataMapper.transformInputToOutputModel(eq(measureReportPatientData))
    } returns measureReportPatientViewData
  }

  @Test
  fun loadReturnsPageOnSuccess() = runTest {
    val measureReportPatientPagingSource =
      MeasureReportPatientsPagingSource(measureReportRepository, measureReportPatientViewDataMapper)
    val loadParams =
      PagingSource.LoadParams.Refresh(key = null, loadSize = 2, placeholdersEnabled = false) as
        PagingSource.LoadParams<Int>
    val result = measureReportPatientPagingSource.load(loadParams)
    val expectedData =
      listOf(
        measureReportPatientViewDataMapper.transformInputToOutputModel(measureReportPatientData)
      )
    assertEquals(
      expected = PagingSource.LoadResult.Page(data = expectedData, prevKey = null, nextKey = 1),
      actual = result
    )
    coVerify { measureReportRepository.retrievePatients(eq(loadParams.key ?: 0)) }
  }
}
