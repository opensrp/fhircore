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

package org.dtree.fhircore.dataclerk.ui.home

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.dtree.fhircore.dataclerk.ui.home.paging.PatientPagingSource
import org.dtree.fhircore.dataclerk.ui.main.AppDataStore
import org.dtree.fhircore.dataclerk.ui.main.PatientItem

@HiltViewModel
class HomeViewModel @Inject constructor(private val dataStore: AppDataStore) : ViewModel() {
  val patientCount = mutableStateOf(0L)
  var patientsPaging: MutableStateFlow<Flow<PagingData<PatientItem>>> =
    MutableStateFlow(emptyFlow())
  init {
    refresh()
  }
  private fun getPatients() =
    Pager(
        config =
          PagingConfig(
            pageSize = 20,
          ),
        pagingSourceFactory = { PatientPagingSource(dataStore) }
      )
      .flow
      .cachedIn(viewModelScope)

  fun refresh() {
    viewModelScope.launch {
      patientCount.value = dataStore.patientCount()
      patientsPaging.emit(getPatients())
    }
  }
}
