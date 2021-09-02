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

package org.smartregister.fhircore.engine.ui.register

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.filter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.data.domain.util.PaginatedDataSource
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider

class RegisterDataViewModel<I : Any, O : Any>(
  application: Application,
  val registerRepository: RegisterRepository<I, O>,
  val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : AndroidViewModel(application) {

  private val originalData: MutableStateFlow<Flow<PagingData<O>>> =
    MutableStateFlow(getPagingData(0))

  var registerData: MutableStateFlow<Flow<PagingData<O>>> = MutableStateFlow(emptyFlow())

  /**
   * Perform filter on the paginated data by invoking the [registerFilter] lambda, passing
   * [filterValue] and [registerFilterType] as the type of filter that you wish to apply e.g.
   * searching by name filter, filter overdue vaccines etc.
   */
  fun filterRegisterData(
    registerFilterType: RegisterFilterType,
    filterValue: Any,
    registerFilter: (RegisterFilterType, O, Any) -> Boolean
  ) {
    viewModelScope.launch(dispatcherProvider.io()) {
      registerData.value =
        originalData.value.map { pagingData: PagingData<O> ->
          pagingData.filter { registerFilter(registerFilterType, it, filterValue) }
        }
    }
  }

  fun loadPageData(currentPage: Int) {
    viewModelScope.launch(dispatcherProvider.io()) {
      registerData.run { value = getPagingData(currentPage) }
    }
  }

  private fun getPagingData(currentPage: Int) =
    Pager(
        config =
          PagingConfig(
            pageSize = PaginationUtil.DEFAULT_PAGE_SIZE,
            initialLoadSize = PaginationUtil.DEFAULT_INITIAL_LOAD_SIZE
          ),
        pagingSourceFactory = {
          paginatedDataSourceInstance().apply { this.currentPage = currentPage }
        }
      )
      .flow

  /**
   * Extend function so as to ensure that the pagingSourceFactory passed to Pager always returns a
   * new instance of [PaginatedDataSource]
   */
  private fun paginatedDataSourceInstance(): PaginatedDataSource<I, O> {
    return PaginatedDataSource(registerRepository)
  }
}
