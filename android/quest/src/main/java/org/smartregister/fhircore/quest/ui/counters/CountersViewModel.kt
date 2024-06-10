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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
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

  private val _isRefreshingPatientsCount = MutableStateFlow(false)
  val isRefreshingPatientsCountStateFlow = _isRefreshingPatientsCount.asStateFlow()

  private val _isRefreshingHomeTracingCount = MutableStateFlow(false)
  val isRefreshingHomeTracingCountStateFlow = _isRefreshingHomeTracingCount.asStateFlow()

  private val _isRefreshingPhoneTracingCount = MutableStateFlow(false)
  val isRefreshingPhoneTracingCountStateFlow = _isRefreshingPhoneTracingCount.asStateFlow()

  private val _isRefreshingAppointmentsCount = MutableStateFlow(false)
  val isRefreshingAppointmentsCountStateFlow = _isRefreshingAppointmentsCount.asStateFlow()

  val isRefreshing =
    combine(
        _isRefreshingPatientsCount,
        _isRefreshingHomeTracingCount,
        _isRefreshingPhoneTracingCount,
        _isRefreshingAppointmentsCount,
      ) { p, ht, pt, ap ->
        p || ht || pt || ap
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, initialValue = false)

  private val _refreshCounter = MutableStateFlow(0)

  val patientsCountStateFlow =
    _refreshCounter
      .onEach { _isRefreshingPatientsCount.update { true } }
      .flatMapLatest {
        registerRepository.countRegisterData(healthModule = HealthModule.HIV).onCompletion {
          _isRefreshingPatientsCount.update { false }
        }
      }
      .stateIn(
        viewModelScope.plus(dispatcherProvider.io()),
        SharingStarted.Lazily,
        initialValue = 0L,
      )

  val homeTracingCountStateFlow =
    _refreshCounter
      .onEach { _isRefreshingHomeTracingCount.update { true } }
      .flatMapLatest {
        registerRepository
          .countRegisterData(healthModule = HealthModule.HOME_TRACING)
          .onCompletion { _isRefreshingHomeTracingCount.update { false } }
      }
      .stateIn(
        viewModelScope.plus(dispatcherProvider.io()),
        SharingStarted.Lazily,
        initialValue = 0L,
      )

  val phoneTracingCountStateFlow =
    _refreshCounter
      .onEach { _isRefreshingPhoneTracingCount.update { true } }
      .flatMapLatest {
        registerRepository
          .countRegisterData(healthModule = HealthModule.PHONE_TRACING)
          .onCompletion { _isRefreshingPhoneTracingCount.update { false } }
      }
      .stateIn(
        viewModelScope.plus(dispatcherProvider.io()),
        SharingStarted.Lazily,
        initialValue = 0L,
      )

  val appointmentsCountStateFlow =
    _refreshCounter
      .onEach { _isRefreshingAppointmentsCount.update { true } }
      .flatMapLatest {
        registerRepository.countRegisterData(healthModule = HealthModule.APPOINTMENT).onCompletion {
          _isRefreshingAppointmentsCount.update { false }
        }
      }
      .stateIn(
        viewModelScope.plus(dispatcherProvider.io()),
        SharingStarted.Lazily,
        initialValue = 0L,
      )

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
    _refreshCounter.update { it + 1 }
  }
}
