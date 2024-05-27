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

package org.smartregister.fhircore.quest.ui.appointment.register

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import org.smartregister.fhircore.engine.ui.filter.DateFilterOption
import org.smartregister.fhircore.quest.ui.LocalDatePickerDialog
import org.smartregister.fhircore.quest.ui.LocalExposedDropdownMenuBox
import org.smartregister.fhircore.quest.ui.PageRegisterScreen
import org.smartregister.fhircore.quest.ui.StandardRegisterEvent
import org.smartregister.fhircore.quest.ui.components.FilterDialog

@Composable
fun AppointmentRegisterScreen(
  modifier: Modifier = Modifier,
  screenTitle: String,
  navController: NavHostController,
  registerViewModel: AppointmentRegisterViewModel = hiltViewModel(),
) {
  var showFiltersDialog by remember { mutableStateOf(false) }
  val currentFilterState by registerViewModel.filtersStateFlow.collectAsStateWithLifecycle()
  val activeFilters = currentFilterState.toFilterList()

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
    val activity = LocalContext.current as? AppCompatActivity ?: return
    FilterAppointmentsModal(
      currentFilterState,
      fragmentManager = activity.supportFragmentManager,
      onDismissAction = { showFiltersDialog = false },
      onFiltersApply = {
        registerViewModel.onEvent(StandardRegisterEvent.ApplyFilter(it))
        showFiltersDialog = false
      },
      hasActiveFilters = activeFilters.isNotEmpty(),
      clearFilter = registerViewModel::clearFilters,
    )
  }
}

@Composable
fun FilterAppointmentsModal(
  currentFilterState: AppointmentFilterState,
  fragmentManager: FragmentManager,
  onDismissAction: () -> Unit,
  onFiltersApply: (AppointmentFilterState) -> Unit,
  hasActiveFilters: Boolean,
  clearFilter: () -> Unit,
) {
  var filtersState by remember { mutableStateOf(currentFilterState) }

  FilterDialog(
    onFiltersApply = { onFiltersApply(filtersState) },
    onDismissAction = onDismissAction,
    hasActiveFilters = hasActiveFilters,
    clearFilter = clearFilter,
  ) {
    AppointmentDateField(
      fragmentManager,
      date = filtersState.date,
      onNewDateSelected = {
        filtersState =
          AppointmentFilterState.default().copy(date = it) // reset other filters to default
      },
    )

    AppointmentExposedDropdown(
      filter = filtersState.patients,
      onItemSelected = {
        filtersState =
          filtersState.copy(
            patients = it,
            patientCategory =
              AppointmentFilter(
                PatientCategory.ALL_PATIENT_CATEGORIES,
                PatientCategory.values().asList(),
              ),
            reason = AppointmentFilter(Reason.ALL_REASONS, Reason.values().asList()),
          )
      },
    )

    AppointmentExposedDropdown(
      filter = filtersState.patientCategory,
      onItemSelected = {
        val categoryReasons =
          Reason.values().filter { reason ->
            it.selected == PatientCategory.ALL_PATIENT_CATEGORIES ||
              reason.patientCategory.contains(it.selected)
          }
        filtersState =
          filtersState.copy(
            patientCategory = it,
            reason = AppointmentFilter(Reason.ALL_REASONS, categoryReasons),
          )
      },
    )

    AppointmentExposedDropdown(
      filter = filtersState.reason,
      onItemSelected = { filtersState = filtersState.copy(reason = it) },
    )
  }
}

@Composable
fun AppointmentDatePicker(
  fragmentManager: FragmentManager,
  selectedDate: DateFilterOption,
  onDatePicked: (DateFilterOption) -> Unit,
  onDateCancel: () -> Unit,
) {
  LocalDatePickerDialog(
    fragmentManager = fragmentManager,
    datePickerTag = "APPOINTMENT_DATE_PICKER",
    selectedDate = selectedDate.value,
    onDatePicked = { onDatePicked.invoke(DateFilterOption(it)) },
    onDateCancel = { onDateCancel.invoke() },
  )
}

@Composable
fun AppointmentDateField(
  fragmentManager: FragmentManager,
  date: DateFilterOption,
  onNewDateSelected: (DateFilterOption) -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  var showCalendarDialog by remember { mutableStateOf(false) }

  LaunchedEffect(isPressed) {
    if (isPressed) {
      showCalendarDialog = true
    }
  }

  OutlinedTextField(
    value = date.text(),
    onValueChange = { /*No-op*/},
    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.DarkGray),
    maxLines = 1,
    readOnly = true,
    singleLine = true,
    modifier = Modifier.fillMaxWidth(),
    trailingIcon = {
      IconButton(onClick = { showCalendarDialog = true }) {
        Icon(imageVector = Icons.Filled.CalendarToday, contentDescription = "date of appointment")
      }
    },
    interactionSource = interactionSource,
  )

  if (showCalendarDialog) {
    AppointmentDatePicker(
      fragmentManager,
      selectedDate = date,
      onDatePicked = {
        onNewDateSelected(it)
        showCalendarDialog = false
      },
      onDateCancel = { showCalendarDialog = false },
    )
  }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T : AppointmentFilterOption> AppointmentExposedDropdown(
  filter: AppointmentFilter<T>,
  onItemSelected: (AppointmentFilter<T>) -> Unit,
) {
  LocalExposedDropdownMenuBox(
    selectedItem = filter.selected,
    options = filter.options,
    onItemSelected = { onItemSelected.invoke(filter.copy(selected = it)) },
  )
}

@Preview
@Composable
fun PreviewExposedDropdown() {
  val patientsFilter =
    AppointmentFilter(PatientAssignment.MY_PATIENTS, options = PatientAssignment.values().asList())
  AppointmentExposedDropdown(patientsFilter, onItemSelected = { /*No-op*/})
}

@Composable
@Preview
fun PreviewFilterAppointmentsModal() {
  val activity = LocalContext.current as AppCompatActivity

  FilterAppointmentsModal(
    currentFilterState = AppointmentFilterState.default(),
    fragmentManager = activity.supportFragmentManager,
    onDismissAction = {},
    onFiltersApply = {},
    hasActiveFilters = false,
    clearFilter = {},
  )
}
