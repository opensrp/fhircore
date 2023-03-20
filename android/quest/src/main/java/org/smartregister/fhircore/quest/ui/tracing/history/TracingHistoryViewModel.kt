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

package org.smartregister.fhircore.quest.ui.tracing.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import org.smartregister.fhircore.engine.data.local.tracing.TracingRepository
import org.smartregister.fhircore.engine.domain.model.TracingHistory
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.quest.data.tracing.TracingHistoryPagingSource
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.util.GeneralListViewModel

@HiltViewModel
class TracingHistoryViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  syncBroadcaster: SyncBroadcaster,
  val fhirEngine: FhirEngine,
) : GeneralListViewModel<TracingHistory>(syncBroadcaster) {
  val patientId = savedStateHandle.get<String>(NavigationArg.PATIENT_ID) ?: ""

  fun onEvent(event: TracingHistoryEvent) {
    when (event) {
      is TracingHistoryEvent.OpenOutComesScreen -> {
        val urlParams =
          NavigationArg.bindArgumentsOf(
            Pair(NavigationArg.PATIENT_ID, patientId),
            Pair(NavigationArg.TRACING_ID, event.historyId)
          )
        event.navController.navigate(route = MainNavigationScreen.TracingOutcomes.route + urlParams)
      }
    }
  }
  override fun paginateRegisterDataFlow(page: Int): Flow<PagingData<TracingHistory>> {
    paginateData.value =
      Pager(
          config = PagingConfig(pageSize = DEFAULT_PAGE_SIZE),
          pagingSourceFactory = {
            TracingHistoryPagingSource(TracingRepository(fhirEngine), patientId)
          }
        )
        .flow
        .cachedIn(viewModelScope)
    return paginateData.value
  }

  companion object {
    private const val DEFAULT_PAGE_SIZE = 20
  }
}
