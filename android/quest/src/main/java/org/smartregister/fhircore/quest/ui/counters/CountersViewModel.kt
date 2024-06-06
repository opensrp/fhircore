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

package org.smartregister.fhircore.quest.ui.counters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.sync.SyncJobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.data.local.register.AppRegisterRepository
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.util.DispatcherProvider

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CountersViewModel
@Inject
constructor(
  syncBroadcaster: SyncBroadcaster,
  val dispatcherProvider: DispatcherProvider,
  val registerRepository: AppRegisterRepository,
) : ViewModel() {

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing = _isRefreshing.asStateFlow()

  private val _refreshCounter = MutableStateFlow(0)

  val patientsCountStateFlow =
    _refreshCounter
      .flatMapLatest { registerRepository.countRegisterData(healthModule = HealthModule.HIV) }
      .onEach { _isRefreshing.update { false } }
      .stateIn(viewModelScope, SharingStarted.Lazily, initialValue = 0L)

  val homeTracingCountStateFlow =
    _refreshCounter
      .flatMapLatest {
        registerRepository.countRegisterData(healthModule = HealthModule.HOME_TRACING)
      }
      .onEach { _isRefreshing.update { false } }
      .stateIn(viewModelScope, SharingStarted.Lazily, initialValue = 0L)

  val phoneTracingCountStateFlow =
    _refreshCounter
      .flatMapLatest {
        registerRepository.countRegisterData(healthModule = HealthModule.PHONE_TRACING)
      }
      .onEach { _isRefreshing.update { false } }
      .stateIn(viewModelScope, SharingStarted.Lazily, initialValue = 0L)

  val appointmentsCountStateFlow =
    _refreshCounter
      .flatMapLatest {
        registerRepository.countRegisterData(healthModule = HealthModule.APPOINTMENT)
      }
      .onEach { _isRefreshing.update { false } }
      .stateIn(viewModelScope, SharingStarted.Lazily, initialValue = 0L)

  init {

    syncBroadcaster.registerSyncListener(
      { status ->
        if (status is SyncJobStatus.Failed || status is SyncJobStatus.Succeeded) {
          refresh()
        }
      },
      scope = viewModelScope,
    )
  }

  fun refresh() {
    _isRefreshing.update { true }
    _refreshCounter.update { it + 1 }
  }
}
