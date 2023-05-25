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

package org.smartregister.fhircore.quest.ui.tracing.outcomes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import org.smartregister.fhircore.engine.data.local.tracing.TracingRepository
import org.smartregister.fhircore.engine.domain.model.TracingOutcome
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.quest.data.generic.createPager
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.util.GeneralListViewModel

@HiltViewModel
class TracingOutcomesViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  syncBroadcaster: SyncBroadcaster,
  val repository: TracingRepository,
) : GeneralListViewModel<TracingOutcome>(syncBroadcaster) {
  private val patientId = savedStateHandle.get<String>(NavigationArg.PATIENT_ID) ?: ""
  private val tracingId = savedStateHandle.get<String>(NavigationArg.TRACING_ID) ?: ""

  fun onEvent(event: TracingOutcomesEvent) {
    when (event) {
      is TracingOutcomesEvent.OpenHistoryDetailsScreen -> {
        val urlParams =
          NavigationArg.bindArgumentsOf(
            Pair(NavigationArg.PATIENT_ID, patientId),
            Pair(NavigationArg.TRACING_ID, event.historyId),
            Pair(NavigationArg.TRACING_ENCOUNTER_ID, event.encounterId),
            Pair(NavigationArg.SCREEN_TITLE, event.title)
          )
        event.navController.navigate(
          route = MainNavigationScreen.TracingHistoryDetails.route + urlParams
        )
      }
    }
  }

  override fun paginateRegisterDataFlow(page: Int): Flow<PagingData<TracingOutcome>> {
    paginateData.value =
      createPager(
          pageSize = DEFAULT_PAGE_SIZE,
          block = { currentPage -> repository.getTracingOutcomes(currentPage, tracingId) }
        )
        .flow
        .cachedIn(viewModelScope)
    return paginateData.value
  }

  companion object {
    private const val DEFAULT_PAGE_SIZE = 20
  }
}
