package org.smartregister.fhircore.engine.data.domain.util

import androidx.paging.PagingSource
import androidx.paging.PagingState

abstract class PaginatedDataSource<I: Any, O : Any>(protected val registerRepository: RegisterRepository<I,O>) :
  PagingSource<Int, O>() {

  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, O> {
    return try {
      val pageNumber = params.key ?: 0
      val data: List<O> = loadData(pageNumber)
      LoadResult.Page(
        data = data,
        prevKey = if (pageNumber == 0) null else pageNumber - 1,
        nextKey = if (data.isNotEmpty()) pageNumber + 1 else null
      )
    } catch (exception: Exception) {
      LoadResult.Error(exception)
    }
  }

  /** Provide data to the [PagingSource] */
  abstract suspend fun loadData(pageNumber: Int): List<O>

  override fun getRefreshKey(state: PagingState<Int, O>): Int? {
    return state.anchorPosition
  }
}
