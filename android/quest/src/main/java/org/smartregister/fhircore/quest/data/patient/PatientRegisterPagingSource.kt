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

package org.smartregister.fhircore.quest.data.patient

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.smartregister.fhircore.engine.data.local.register.PatientRegisterRepository
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.rulesengine.RulesFactory
import org.smartregister.fhircore.quest.data.patient.model.PatientPagingSourceState
import org.smartregister.fhircore.quest.ui.shared.models.RegisterCardData
import timber.log.Timber

/**
 * @property _patientPagingSourceState as state containing the properties used in the
 * [PatientRegisterRepository] function for loading data to the paging source.
 */
class PatientRegisterPagingSource(
  private val patientRegisterRepository: PatientRegisterRepository,
  private val rulesFactory: RulesFactory
) : PagingSource<Int, RegisterCardData>() {

  private lateinit var _patientPagingSourceState: PatientPagingSourceState

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
  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RegisterCardData> {
    return try {
      val currentPage = params.key ?: _patientPagingSourceState.currentPage
      val data =
        patientRegisterRepository.loadRegisterData(
            currentPage = currentPage,
            registerId = _patientPagingSourceState.registerId,
            loadAll = _patientPagingSourceState.loadAll
          )
          .map {
            RegisterCardData(
              resourceData = it,
              computedRegisterCardData = computeRegisterCardRules(it)
            )
          }
      val prevKey =
        when {
          _patientPagingSourceState.loadAll -> if (currentPage == 0) null else currentPage - 1
          else -> null
        }
      val nextKey =
        when {
          _patientPagingSourceState.loadAll -> if (data.isNotEmpty()) currentPage + 1 else null
          else -> null
        }

      LoadResult.Page(data = data, prevKey = prevKey, nextKey = nextKey)
    } catch (exception: Exception) {
      Timber.e(exception)
      LoadResult.Error(exception)
    }
  }

  private fun computeRegisterCardRules(resourceData: ResourceData): Map<String, Any> {
    val rules = _patientPagingSourceState.registerCardConfig.columnOne.rowOne?.rules
    return rulesFactory.fireRule(rules ?: listOf(), resourceData)
  }

  fun setPatientPagingSourceState(patientPagingSourceState: PatientPagingSourceState) {
    this._patientPagingSourceState = patientPagingSourceState
  }

  override fun getRefreshKey(state: PagingState<Int, RegisterCardData>): Int? {
    return state.anchorPosition
  }

  companion object {
    const val DEFAULT_PAGE_SIZE = 20
    const val DEFAULT_INITIAL_LOAD_SIZE = 20
  }
}
