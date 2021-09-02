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

package org.smartregister.fhircore.engine.data.domain.util

import androidx.paging.PagingSource
import androidx.paging.PagingState

/**
 * Subclass of [PagingSource] that is used to paginate data on the register. Requires
 * [RegisterRepository] to load data to the [PagingSource]. Value type [I] represents the Data
 * Transfer Object (DTO) like a FHIR Patient Resource. [O] represents the output type of
 * [RegisterRepository] data. [I] is transformed into [O] using a [DomainMapper]
 */
class PaginatedDataSource<I : Any, O : Any>(
  private val registerRepository: RegisterRepository<I, O>
) : PagingSource<Int, O>() {

  var currentPage: Int = 0
  var query: String = ""

  /**
   * Load data for the [currentPage]. nextKey and prevKey for [params] are both set to null to
   * prevent automatic loading of by the [PagingSource]. This is done in order to explicitly allow
   * loading of data by manually clicking navigation previous or next buttons.
   *
   * For infinite scroll (automatically loading data via the paging source in the background). You
   * may need to override this method [load] and set the prevKey and nextKey values of [params] to
   * something like this (Checking if data is not empty prevents querying for more results):
   *
   * prevKey = if (pageNumber == 0) null else pageNumber - 1
   *
   * nextKey = if (data.isNotEmpty()) pageNumber + 1 else null
   */
  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, O> {
    return try {
      val pageNumber = params.key ?: currentPage
      LoadResult.Page(
        data = registerRepository.loadData(query = query, pageNumber = pageNumber),
        prevKey = null,
        nextKey = null
      )
    } catch (exception: Exception) {
      LoadResult.Error(exception)
    }
  }

  override fun getRefreshKey(state: PagingState<Int, O>): Int? {
    return state.anchorPosition
  }
}
