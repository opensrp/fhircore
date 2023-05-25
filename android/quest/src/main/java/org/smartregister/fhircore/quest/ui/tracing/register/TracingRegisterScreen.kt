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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.LocalExposedDropdownMenuBox
import org.smartregister.fhircore.quest.ui.PageRegisterScreen
import org.smartregister.fhircore.quest.ui.StandardRegisterEvent

@Composable
fun TracingRegisterScreen(
  modifier: Modifier = Modifier,
  screenTitle: String,
  navController: NavHostController,
  registerViewModel: TracingRegisterViewModel = hiltViewModel()
) {
  var showFiltersDialog by remember { mutableStateOf(false) }
  val currentFilterState by registerViewModel.filtersStateFlow.collectAsStateWithLifecycle()

  PageRegisterScreen(
    modifier = modifier,
    screenTitle = screenTitle,
    navController = navController,
    registerViewModel = registerViewModel,
    filterNavClickAction = { showFiltersDialog = true }
  )

  if (showFiltersDialog) {
    FilterTracingRegisterModal(
      currentFilterState,
      onDismissAction = { showFiltersDialog = false },
      onApplyAction = {
        registerViewModel.onEvent(StandardRegisterEvent.ApplyFilter(it))
        showFiltersDialog = false
      }
    )
  }
}

@Composable
fun FilterTracingRegisterModal(
  filterState: TracingRegisterFilterState,
  onDismissAction: () -> Unit,
  onApplyAction: (TracingRegisterFilterState) -> Unit
) {
  var filtersState by remember { mutableStateOf(filterState) }

  Dialog(
    onDismissRequest = onDismissAction,
    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
  ) {
    Card(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
      Column(modifier = Modifier.padding(8.dp)) {
        Text(
          text = stringResource(id = R.string.filters).uppercase(),
          textAlign = TextAlign.Start,
          style = MaterialTheme.typography.h5,
          color = MaterialTheme.colors.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
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
                      TracingPatientCategory.values().asList()
                    ),
                  reason =
                    TracingRegisterUiFilter(
                      TracingReason.ALL_REASONS,
                      TracingReason.values().asList()
                    ),
                  age = TracingRegisterUiFilter(AgeFilter.ALL_AGES, AgeFilter.values().asList())
                )
            }
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
                      TracingReason.values().asList()
                    ),
                  age = TracingRegisterUiFilter(AgeFilter.ALL_AGES, AgeFilter.values().asList())
                )
            }
          )

          TracingRegisterExposedDropdown(
            filter = filtersState.reason,
            onItemSelected = {
              filtersState =
                filtersState.copy(
                  reason = it,
                  age = TracingRegisterUiFilter(AgeFilter.ALL_AGES, AgeFilter.values().asList())
                )
            }
          )

          TracingRegisterExposedDropdown(
            filter = filtersState.age,
            onItemSelected = { filtersState = filtersState.copy(age = it) }
          )
        }

        Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
          TextButton(
            onClick = { onDismissAction() },
            modifier = Modifier.wrapContentWidth(),
            colors =
              ButtonDefaults.textButtonColors(
                contentColor = Color.DarkGray.copy(alpha = ContentAlpha.medium)
              )
          ) { Text(text = stringResource(id = R.string.cancel).uppercase()) }

          TextButton(
            onClick = { onApplyAction.invoke(filtersState) },
            modifier = Modifier.wrapContentWidth()
          ) { Text(text = stringResource(id = R.string.apply).uppercase()) }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T : TracingFilterOption> TracingRegisterExposedDropdown(
  filter: TracingRegisterUiFilter<T>,
  onItemSelected: (TracingRegisterUiFilter<T>) -> Unit
) {
  LocalExposedDropdownMenuBox(
    selectedItem = filter.selected,
    options = filter.options,
    onItemSelected = { onItemSelected.invoke(filter.copy(selected = it)) }
  )
}
