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

package org.dtree.fhircore.dataclerk.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.dtree.fhircore.dataclerk.ui.main.AppDataStore
import org.dtree.fhircore.dataclerk.ui.main.PatientItem
import timber.log.Timber

@HiltViewModel
class SearchViewModel @Inject constructor(private val dataStore: AppDataStore) : ViewModel() {
  private val searchText: MutableStateFlow<String> = MutableStateFlow("")
  private var showProgressBar: MutableStateFlow<Boolean> = MutableStateFlow(false)
  private var matchedPatients: MutableStateFlow<List<PatientItem>> = MutableStateFlow(arrayListOf())
  val userSearchModelState =
    combine(searchText, showProgressBar, matchedPatients) { text, progress, patients ->
      SearchModelState(searchText = text, patients = patients, showProgressBar = progress)
    }
  fun onSearchChanged(text: String) {
    Timber.e(text)
    searchText.value = text
    if (text.isBlank()) {
      matchedPatients.value = listOf()
      return
    }
    showProgressBar.value = true
    viewModelScope.launch {
      val data = dataStore.search(text)
      showProgressBar.value = false
      matchedPatients.value = data
    }
  }

  fun onClearClick() {
    searchText.value = ""
    matchedPatients.value = listOf()
  }
}
