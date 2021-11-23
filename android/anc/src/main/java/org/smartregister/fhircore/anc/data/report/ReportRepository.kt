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

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.android.fhir.FhirEngine
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import javax.inject.Inject

class ReportRepository @Inject constructor(val fhirEngine: FhirEngine) :
  PagingSource<Int, ReportItem>() {

  override fun getRefreshKey(state: PagingState<Int, ReportItem>): Int? {
    return state.anchorPosition
  }

  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ReportItem> {
    return try {
      // Todo: here @Davison will update logic to load reports type items
      val nextPage = params.key ?: 0

      //      val encounters =
      //        fhirEngine.search<Encounter> {
      //          filter(Encounter.SUBJECT) { value = "Patient/$patientId" }
      //          from = nextPage * PaginationUtil.DEFAULT_PAGE_SIZE
      //          count = PaginationUtil.DEFAULT_PAGE_SIZE
      //        }
      //
      //      var data = encounters.map { ReportItem(it.id, it.id, "it.status", "it.status",
      // "it.status") }

      //      if(data.isEmpty()){
      //        data = listOf(ReportItem(title = "Test Report", description = "Women having test
      // reports encounters"))
      //      }

      val data =
        listOf(
          ReportItem(
            id = "1",
            title = "Test Report 1",
            description = "Women having test reports encounters",
            reportType = "4"
          ),
          ReportItem(
            id = "2",
            title = "Test Report 2",
            description = "Women having test reports ANC",
            reportType = "4"
          )
        )

      LoadResult.Page(data = data, prevKey = null, nextKey = null)
    } catch (e: Exception) {
      LoadResult.Error(e)
    }
  }
}
