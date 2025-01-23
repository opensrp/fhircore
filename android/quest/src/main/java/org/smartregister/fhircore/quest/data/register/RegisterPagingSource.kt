/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.data.register

import android.database.SQLException
import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.rulesengine.RulesExecutor
import org.smartregister.fhircore.quest.data.register.model.RegisterPagingSourceState
import timber.log.Timber

/**
 * @property registerPagingSourceState as state containing the properties used in the
 *   [RegisterRepository] function for loading data to the paging source.
 */
class RegisterPagingSource(
  private val registerRepository: RegisterRepository,
  private val fhirResourceConfig: FhirResourceConfig?,
  private val actionParameters: Map<String, String>?,
  private val registerPagingSourceState: RegisterPagingSourceState,
  private val rulesExecutor: RulesExecutor,
) : PagingSource<Int, ResourceData>() {

  /**
   * To load data for the current page, nextKey and prevKey for [params] are both set to null to
   * prevent automatic loading of by the [PagingSource]. This is done in order to explicitly allow
   * loading of data by manually clicking navigation previous or next buttons.
   *
   * To load all data (by automatically paginating the results in the background), set loadAll to
   * true (Checking if data is not empty prevents querying for more results when there is none):
   *
   * prevKey = if (pageNumber == 0) null else pageNumber - 1
   *
   * nextKey = if (data.isNotEmpty()) pageNumber + 1 else null
   */
  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ResourceData> {
    return try {
      val currentPage = params.key ?: registerPagingSourceState.currentPage
      val registerData =
        registerRepository
          .loadRegisterData(
            currentPage = currentPage,
            registerId = registerPagingSourceState.registerId,
            fhirResourceConfig = fhirResourceConfig,
            paramsMap = actionParameters,
          )
          .map {
            rulesExecutor.processResourceData(
              repositoryResourceData = it,
              params = actionParameters,
              rules = registerPagingSourceState.rules,
            )
          }

      val prevKey =
        if (registerPagingSourceState.loadAll && currentPage > 0) currentPage - 1 else null
      val nextKey =
        if (registerPagingSourceState.loadAll && registerData.isNotEmpty()) {
          currentPage + 1
        } else {
          null
        }
      LoadResult.Page(data = registerData, prevKey = prevKey, nextKey = nextKey)
    } catch (exception: SQLException) {
      Timber.e(exception)
      LoadResult.Error(exception)
    } catch (exception: Exception) {
      Timber.e(exception)
      LoadResult.Error(exception)
    }
  }

  override fun getRefreshKey(state: PagingState<Int, ResourceData>): Int? {
    return state.anchorPosition?.let { anchorPosition ->
      state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
        ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
    }
  }
}
