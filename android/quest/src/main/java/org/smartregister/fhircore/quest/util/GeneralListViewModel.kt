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

package org.smartregister.fhircore.quest.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.android.fhir.sync.SyncJobStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncBroadcaster

abstract class GeneralListViewModel<T : Any>(syncBroadcaster: SyncBroadcaster) : ViewModel() {

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean>
    get() = _isRefreshing.asStateFlow()

  private val _refreshCounter = MutableStateFlow(0)

  private val _currentPage = MutableLiveData(0)
  val currentPage: LiveData<Int>
    get() = _currentPage

  val paginateData: MutableStateFlow<Flow<PagingData<T>>> = MutableStateFlow(emptyFlow())

  init {
    val pageFlow = _currentPage.asFlow().debounce(200)
    val refreshCounterFlow = _refreshCounter
    viewModelScope.launch {
      combine(refreshCounterFlow, pageFlow) { r, p -> Pair(r, p) }
        .mapLatest {
          val pagingFlow = paginateRegisterDataFlow(page = it.second)
          return@mapLatest pagingFlow.onEach { _isRefreshing.emit(false) }
        }
        .collect { value -> paginateData.emit(value.cachedIn(viewModelScope)) }
    }

    //    viewModelScope.launch { paginateRegisterDataForSearch() }

    val syncStateListener =
      object : OnSyncListener {
        override fun onSync(state: SyncJobStatus) {
          val isStateCompleted = state is SyncJobStatus.Failed || state is SyncJobStatus.Finished
          if (isStateCompleted) {
            refresh()
          }
        }
      }
    syncBroadcaster.registerSyncListener(syncStateListener, viewModelScope)
  }
  private fun paginateRegisterDataForSearch() {
    paginateData.value = paginateRegisterDataFlow()
  }
  abstract fun paginateRegisterDataFlow(page: Int = 0): Flow<PagingData<T>>

  fun refresh() {
    _isRefreshing.value = true
    _refreshCounter.value += 1
  }
}
