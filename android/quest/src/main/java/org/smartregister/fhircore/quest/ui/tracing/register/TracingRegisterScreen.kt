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

package org.smartregister.fhircore.quest.ui.tracing.register

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import org.smartregister.fhircore.quest.ui.LocalExposedDropdownMenuBox
import org.smartregister.fhircore.quest.ui.PageRegisterScreen
import org.smartregister.fhircore.quest.ui.StandardRegisterEvent
import org.smartregister.fhircore.quest.ui.components.FilterDialog

@Composable
fun TracingRegisterScreen(
  modifier: Modifier = Modifier,
  screenTitle: String,
  navController: NavHostController,
  registerViewModel: TracingRegisterViewModel = hiltViewModel(),
) {
  var showFiltersDialog by remember { mutableStateOf(false) }
  val currentFilterState by registerViewModel.filtersStateFlow.collectAsStateWithLifecycle()
  val activeFilters = registerViewModel.getActiveFilters(currentFilterState)

  PageRegisterScreen(
    modifier = modifier,
    screenTitle = screenTitle,
    navController = navController,
    registerViewModel = registerViewModel,
    filterNavClickAction = { showFiltersDialog = true },
    activeFilters = activeFilters,
    showFilterValues = true,
  )

  if (showFiltersDialog) {
    FilterTracingRegisterModal(
      currentFilterState,
      onDismissAction = { showFiltersDialog = false },
      onApplyAction = {
        registerViewModel.onEvent(StandardRegisterEvent.ApplyFilter(it))
        showFiltersDialog = false
      },
      hasActiveFilters = activeFilters.isNotEmpty(),
      clearFilter = registerViewModel::clearFilters,
    )
  }
}

@Composable
fun FilterTracingRegisterModal(
  filterState: TracingRegisterFilterState,
  onDismissAction: () -> Unit,
  onApplyAction: (TracingRegisterFilterState) -> Unit,
  hasActiveFilters: Boolean,
  clearFilter: () -> Unit,
) {
  var filtersState by remember { mutableStateOf(filterState) }

  FilterDialog(
    onFiltersApply = { onApplyAction.invoke(filtersState) },
    onDismissAction = onDismissAction,
    hasActiveFilters = hasActiveFilters,
    clearFilter = clearFilter,
  ) {
    TracingRegisterExposedDropdown(
      filter = filtersState.patientAssignment,
      onItemSelected = {
        filtersState =
          filtersState.copy(
            patientAssignment = it,
            patientCategory =
              TracingRegisterUiFilter(
                TracingPatientCategory.ALL_PATIENT_CATEGORIES,
                TracingPatientCategory.values().asList(),
              ),
            reason =
              TracingRegisterUiFilter(
                TracingReason.ALL_REASONS,
                TracingReason.values().asList(),
              ),
            age = TracingRegisterUiFilter(AgeFilter.ALL_AGES, AgeFilter.values().asList()),
          )
      },
    )

    TracingRegisterExposedDropdown(
      filter = filtersState.patientCategory,
      onItemSelected = {
        filtersState =
          filtersState.copy(
            patientCategory = it,
            reason =
              TracingRegisterUiFilter(
                TracingReason.ALL_REASONS,
                TracingReason.values().asList(),
              ),
            age = TracingRegisterUiFilter(AgeFilter.ALL_AGES, AgeFilter.values().asList()),
          )
      },
    )

    TracingRegisterExposedDropdown(
      filter = filtersState.reason,
      onItemSelected = {
        filtersState =
          filtersState.copy(
            reason = it,
            age = TracingRegisterUiFilter(AgeFilter.ALL_AGES, AgeFilter.values().asList()),
          )
      },
    )

    TracingRegisterExposedDropdown(
      filter = filtersState.age,
      onItemSelected = { filtersState = filtersState.copy(age = it) },
    )
  }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T : TracingFilterOption> TracingRegisterExposedDropdown(
  filter: TracingRegisterUiFilter<T>,
  onItemSelected: (TracingRegisterUiFilter<T>) -> Unit,
) {
  LocalExposedDropdownMenuBox(
    selectedItem = filter.selected,
    options = filter.options,
    onItemSelected = { onItemSelected.invoke(filter.copy(selected = it)) },
  )
}
