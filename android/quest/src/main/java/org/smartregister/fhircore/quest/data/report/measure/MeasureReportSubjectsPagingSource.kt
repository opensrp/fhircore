/*
 * Copyright 2021-2023 Ona Systems, Inc
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
import androidx.paging.PagingState
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportSubjectViewData
import org.smartregister.fhircore.quest.util.mappers.MeasureReportSubjectViewDataMapper

class MeasureReportSubjectsPagingSource(
    private val measureReportPagingSource: MeasureReportPagingSource,
    val measureReportSubjectViewDataMapper: MeasureReportSubjectViewDataMapper
) : PagingSource<Int, MeasureReportSubjectViewData>() {

  override suspend fun load(
    params: LoadParams<Int>
  ): LoadResult<Int, MeasureReportSubjectViewData> {
    return try {
      val currentPage = params.key ?: 0
      val pageSize = params.loadSize
      val data =
        measureReportPagingSource.retrieveSubjects(currentPage).map { resourceData ->
          measureReportSubjectViewDataMapper.transformInputToOutputModel(resourceData)
        }
      val prevKey = if (currentPage == 0) null else currentPage - 1
      val nextKey = if (data.isNotEmpty() && data.size > pageSize) currentPage + 1 else null
      LoadResult.Page(data = data, prevKey = prevKey, nextKey = nextKey)
    } catch (exception: Exception) {
      LoadResult.Error(exception)
    }
  }

  override fun getRefreshKey(state: PagingState<Int, MeasureReportSubjectViewData>): Int? {
    return state.anchorPosition
  }
}
