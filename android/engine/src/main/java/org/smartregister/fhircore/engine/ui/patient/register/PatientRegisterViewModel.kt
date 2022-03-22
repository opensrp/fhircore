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

package org.smartregister.fhircore.engine.ui.patient.register

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.android.fhir.sync.State
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.data.local.patient.PatientRegisterPagingSource
import org.smartregister.fhircore.engine.data.local.patient.PatientRegisterPagingSource.Companion.DEFAULT_INITIAL_LOAD_SIZE
import org.smartregister.fhircore.engine.data.local.patient.PatientRegisterPagingSource.Companion.DEFAULT_PAGE_SIZE
import org.smartregister.fhircore.engine.data.local.patient.PatientRepository

@HiltViewModel
class PatientRegisterViewModel @Inject constructor(val patientRepository: PatientRepository) :
  ViewModel() {

  private val _currentPage = MutableLiveData(0)
  val currentPage
    get() = _currentPage

  private val _searchText = mutableStateOf("")
  val searchText: androidx.compose.runtime.State<String>
    get() = _searchText

  private val _totalRecordsCount = MutableLiveData(1L)

  init {
    viewModelScope.launch { _totalRecordsCount.postValue(patientRepository.countRegisterData()) }
  }

  fun paginateData(currentPage: Int, loadAll: Boolean) =
    MutableStateFlow(
      Pager(
          config =
            PagingConfig(pageSize = DEFAULT_PAGE_SIZE, initialLoadSize = DEFAULT_INITIAL_LOAD_SIZE),
          pagingSourceFactory = {
            PatientRegisterPagingSource(patientRepository).apply {
              setLoadAll(loadAll)
              updateCurrentPage(currentPage)
            }
          }
        )
        .flow
    )

  fun countPages(): Int =
    _totalRecordsCount.value?.toDouble()?.div(DEFAULT_PAGE_SIZE.toLong())?.let { ceil(it).toInt() }
      ?: 1

  fun onEvent(event: PatientRegisterEvent) {
    when (event) {
      is PatientRegisterEvent.SearchRegister -> _searchText.value = event.searchText
      PatientRegisterEvent.MoveToNextPage ->
        this._currentPage.value = this._currentPage.value?.plus(1)
      PatientRegisterEvent.MoveToPreviousPage ->
        this._currentPage.value?.let { if (it > 0) _currentPage.value = it.minus(1) }
    }
  }
}
