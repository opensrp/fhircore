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

package org.smartregister.fhircore.quest.ui.tracing.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.sync.SyncJobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.data.local.tracing.TracingRepository
import org.smartregister.fhircore.engine.domain.model.TracingOutcomeDetails
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.quest.navigation.NavigationArg

@HiltViewModel
class TracingHistoryDetailsViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  syncBroadcaster: SyncBroadcaster,
  val repository: TracingRepository,
) : ViewModel() {
  val patientId = savedStateHandle.get<String>(NavigationArg.PATIENT_ID) ?: ""
  val historyId = savedStateHandle.get<String>(NavigationArg.TRACING_ID) ?: ""
  val encounterId = savedStateHandle.get<String>(NavigationArg.TRACING_ENCOUNTER_ID) ?: ""

  private val _tracingHistoryDetailsViewDataFlow = MutableStateFlow<TracingOutcomeDetails?>(null)
  val tracingHistoryDetailsViewData: StateFlow<TracingOutcomeDetails?>
    get() = _tracingHistoryDetailsViewDataFlow.asStateFlow()

  init {
    syncBroadcaster.registerSyncListener(
      object : OnSyncListener {
        override fun onSync(state: SyncJobStatus) {
          when (state) {
            is SyncJobStatus.Finished, is SyncJobStatus.Failed -> {
              fetchTracingData()
            }
            else -> {}
          }
        }
      },
      viewModelScope
    )

    fetchTracingData()
  }

  private fun fetchTracingData() {
    viewModelScope.launch {
      try {
        _tracingHistoryDetailsViewDataFlow.emit(
                repository.getHistoryDetails(historyId = historyId, encounterId = encounterId)
        )
      } catch (e: Exception) {
        e.printStackTrace()
      }

    }
  }
}
