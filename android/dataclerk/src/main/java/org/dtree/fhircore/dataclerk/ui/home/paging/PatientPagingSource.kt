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

package org.dtree.fhircore.dataclerk.ui.home.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.dtree.fhircore.dataclerk.ui.main.AppDataStore
import org.dtree.fhircore.dataclerk.ui.main.PatientItem

class PatientPagingSource(private val dataStore: AppDataStore) : PagingSource<Int, PatientItem>() {
  override fun getRefreshKey(state: PagingState<Int, PatientItem>): Int? {
    return state.anchorPosition?.let { anchorPosition ->
      state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
        ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
    }
  }

  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PatientItem> {
    return try {
      val page = params.key ?: 1
      val response = dataStore.loadPatients(page = page)

      LoadResult.Page(
        data = response,
        prevKey = if (page == 1) null else page.minus(1),
        nextKey = if (response.isEmpty()) null else page.plus(1),
      )
    } catch (e: Exception) {
      LoadResult.Error(e)
    }
  }
}
