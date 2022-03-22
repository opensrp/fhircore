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

package org.smartregister.fhircore.engine.data.local.patient

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.smartregister.fhircore.engine.appfeature.AppFeature
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.domain.model.RegisterRow

/**
 * @property _currentPage Set the current page to load data for (database table offset)
 * @property _loadAll Indicate whether to load all data incrementally in the background
 */
class PatientRegisterPagingSource(private val patientRepository: PatientRepository) :
  PagingSource<Int, RegisterRow>() {

  private var _healthModule: HealthModule? = null

  private var _appFeature: AppFeature? = null

  private var _currentPage: Int = 0

  private var _loadAll: Boolean = false

  /**
   * To load data for the [_currentPage], nextKey and prevKey for [params] are both set to null to
   * prevent automatic loading of by the [PagingSource]. This is done in order to explicitly allow
   * loading of data by manually clicking navigation previous or next buttons.
   *
   * To load all data (by automatically paginating the results in the background), [_loadAll] has to
   * be set to true (Checking if data is not empty prevents querying for more results when there is
   * none):
   *
   * prevKey = if (pageNumber == 0) null else pageNumber - 1
   *
   * nextKey = if (data.isNotEmpty()) pageNumber + 1 else null
   */
  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RegisterRow> {
    return try {
      val pageNumber = params.key ?: _currentPage
      val data =
        patientRepository.loadRegisterData(appFeature = _appFeature, healthModule = _healthModule)
      val prevKey =
        when {
          _loadAll -> if (pageNumber == 0) null else pageNumber - 1
          else -> null
        }

      LoadResult.Page(data = data, prevKey = prevKey, nextKey = null)
    } catch (exception: Exception) {
      LoadResult.Error(exception)
    }
  }

  fun updateCurrentPage(currentPage: Int) {
    this._currentPage = currentPage
  }

  fun updateHealthModule(appFeature: AppFeature?, healthModule: HealthModule?) {
    this._appFeature = appFeature
    this._healthModule = healthModule
  }

  fun setLoadAll(loadAll: Boolean) {
    this._loadAll = loadAll
  }

  override fun getRefreshKey(state: PagingState<Int, RegisterRow>): Int? {
    return state.anchorPosition
  }

  companion object {
    const val DEFAULT_PAGE_SIZE = 20
    const val DEFAULT_INITIAL_LOAD_SIZE = 20
  }
}
