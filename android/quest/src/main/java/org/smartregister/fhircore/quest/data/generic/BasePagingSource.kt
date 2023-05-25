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

package org.smartregister.fhircore.quest.data.generic

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState

class BasePagingSource<V : Any>(private val block: suspend (Int) -> List<V>) :
  PagingSource<Int, V>() {
  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, V> {
    val currentPage = params.key ?: 0
    return try {
      val data = block(currentPage)
      val prevKey = if (currentPage == 0) null else currentPage - 1
      val nextKey = if (data.isNotEmpty()) currentPage + 1 else null

      LoadResult.Page(data = data, prevKey = prevKey, nextKey = nextKey)
    } catch (e: Exception) {
      LoadResult.Error(e)
    }
  }

  override fun getRefreshKey(state: PagingState<Int, V>): Int? {
    return state.anchorPosition?.let { anchorPosition ->
      state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
        ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
    }
  }
}

fun <V : Any> createPager(
  pageSize: Int,
  enablePlaceholders: Boolean = false,
  block: suspend (Int) -> List<V>
): Pager<Int, V> =
  Pager(
    config = PagingConfig(enablePlaceholders = enablePlaceholders, pageSize = pageSize),
    pagingSourceFactory = { BasePagingSource(block) }
  )
