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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.dtree.fhircore.dataclerk.ui.home.paging.PatientPagingSource
import org.dtree.fhircore.dataclerk.ui.main.AppDataStore

@HiltViewModel
class HomeViewModel @Inject constructor(private val dataStore: AppDataStore) : ViewModel() {
  fun getPatients() =
    Pager(
        config =
          PagingConfig(
            pageSize = 20,
          ),
        pagingSourceFactory = { PatientPagingSource(dataStore) }
      )
      .flow
      .cachedIn(viewModelScope)
}
