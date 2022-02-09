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
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.filter
import kotlin.math.ceil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.data.domain.util.PaginatedDataSource
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider

class RegisterDataViewModel<I : Any, O : Any>(
  application: Application,
  val registerRepository: RegisterRepository<I, O>,
  val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) : AndroidViewModel(application) {

  private val _showLoader = MutableLiveData(false)
  val showLoader
    get() = _showLoader

  private val _showResultsCount = MutableLiveData(false)
  val showResultsCount
    get() = _showResultsCount

  private val _showPageCount = MutableLiveData(true)
  val showPageCount
    get() = _showPageCount

  private val _totalRecordsCount = MutableLiveData(1L)

  private val _currentPage = MutableLiveData(0)
  val currentPage
    get() = _currentPage

  @Stable
  private val allRegisterData: MutableStateFlow<Flow<PagingData<O>>> =
    MutableStateFlow(getPagingData(currentPage = 0, loadAll = true))

  var registerData: MutableStateFlow<Flow<PagingData<O>>> = MutableStateFlow(emptyFlow())

  private val _registerViewConfiguration: MutableLiveData<RegisterViewConfiguration> =
    MutableLiveData()
  val registerViewConfiguration
    get() = _registerViewConfiguration

  init {
    viewModelScope.launch { _totalRecordsCount.postValue(registerRepository.countAll()) }
  }

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
        allRegisterData.value.map { pagingData: PagingData<O> ->
          pagingData.filter { registerFilter(registerFilterType, it, filterValue) }
        }
    }
  }

  fun loadPageData(currentPage: Int) {
    viewModelScope.launch(dispatcherProvider.io()) {
      registerData.run { value = getPagingData(currentPage = currentPage, loadAll = false) }
    }
  }

  private fun getPagingData(currentPage: Int, loadAll: Boolean) =
    Pager(
        config =
          PagingConfig(
            pageSize = PaginationUtil.DEFAULT_PAGE_SIZE,
            initialLoadSize = PaginationUtil.DEFAULT_INITIAL_LOAD_SIZE,
          ),
        pagingSourceFactory = {
          PaginatedDataSource(registerRepository).apply {
            this.loadAll = loadAll
            this.currentPage = currentPage
          }
        }
      )
      .flow

  fun updateViewConfigurations(viewConfiguration: RegisterViewConfiguration) {
    this._registerViewConfiguration.postValue(viewConfiguration)
    registerViewConfiguration.value?.showPageCount?.let { this.showPageCount(it) }
  }

  fun previousPage() {
    this._currentPage.value?.let { if (it > 0) _currentPage.value = it.minus(1) }
  }

  fun nextPage() {
    this._currentPage.value = this._currentPage.value?.plus(1)
  }

  fun currentPage() = this.currentPage.value?.plus(1) ?: 1

  fun countPages() =
    _totalRecordsCount.value?.toDouble()?.div(PaginationUtil.DEFAULT_PAGE_SIZE.toLong())?.let {
      ceil(it).toInt()
    }
      ?: 1

  fun showResultsCount(showResultsCount: Boolean) {
    this._showResultsCount.postValue(showResultsCount)
  }

  fun showPageCount(showPageCount: Boolean) {
    this._showPageCount.postValue(showPageCount)
  }

  fun setShowLoader(showLoader: Boolean) {
    this._showLoader.postValue(showLoader)
  }

  fun reloadCurrentPageData(refreshTotalRecordsCount: Boolean = false) {
    _currentPage.value?.let { loadPageData(it) }
    if (refreshTotalRecordsCount) {
      viewModelScope.launch { _totalRecordsCount.postValue(registerRepository.countAll()) }
    }
  }
}
