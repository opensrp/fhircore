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
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg

@HiltViewModel
class TracingOutcomesViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
) : ViewModel() {
  val patientId = savedStateHandle.get<String>(NavigationArg.PATIENT_ID) ?: ""
  fun onEvent(event: TracingOutcomesEvent) {
    when (event) {
      is TracingOutcomesEvent.OpenHistoryDetailsScreen -> {
        val urlParams = NavigationArg.bindArgumentsOf(Pair(NavigationArg.PATIENT_ID, patientId))
        event.navController.navigate(
          route = MainNavigationScreen.TracingHistoryDetails.route + urlParams
        )
      }
    }
  }
}
