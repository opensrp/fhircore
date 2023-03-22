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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.LocalDatePickerDialog
import org.smartregister.fhircore.quest.ui.LocalExposedDropdownMenuBox
import org.smartregister.fhircore.quest.ui.PageRegisterScreen
import org.smartregister.fhircore.quest.ui.StandardRegisterEvent

@Composable
fun AppointmentRegisterScreen(
  modifier: Modifier = Modifier,
  screenTitle: String,
  navController: NavHostController,
  registerViewModel: AppointmentRegisterViewModel = hiltViewModel()
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
    val activity = LocalContext.current as? AppCompatActivity ?: return
    FilterAppointmentsModal(
      currentFilterState,
      fragmentManager = activity.supportFragmentManager,
      onDismissAction = { showFiltersDialog = false },
      onFiltersApply = {
        registerViewModel.onEvent(StandardRegisterEvent.ApplyFilter(it))
        showFiltersDialog = false
      }
    )
  }
}

@Composable
fun FilterAppointmentsModal(
  currentFilterState: AppointmentFilterState,
  fragmentManager: FragmentManager,
  onDismissAction: () -> Unit,
  onFiltersApply: (AppointmentFilterState) -> Unit
) {
  var filtersState by remember { mutableStateOf(currentFilterState) }

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
          AppointmentDateField(
            fragmentManager,
            date = filtersState.date,
            onNewDateSelected = {
              filtersState =
                AppointmentFilterState.default().copy(date = it) // reset other filters to default
            }
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
                      PatientCategory.values().asList()
                    ),
                  reason = AppointmentFilter(Reason.ALL_REASONS, Reason.values().asList())
                )
            }
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
                  reason = AppointmentFilter(Reason.ALL_REASONS, categoryReasons)
                )
            }
          )

          AppointmentExposedDropdown(
            filter = filtersState.reason,
            onItemSelected = { filtersState = filtersState.copy(reason = it) }
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
            onClick = { onFiltersApply(filtersState) },
            modifier = Modifier.wrapContentWidth()
          ) { Text(text = stringResource(id = R.string.apply).uppercase()) }
        }
      }
    }
  }
}

@Composable
fun AppointmentDatePicker(
  fragmentManager: FragmentManager,
  selectedDate: AppointmentDate,
  onDatePicked: (AppointmentDate) -> Unit,
  onDateCancel: () -> Unit
) {
  LocalDatePickerDialog(
    fragmentManager = fragmentManager,
    datePickerTag = "APPOINTMENT_DATE_PICKER",
    selectedDate = selectedDate.value,
    onDatePicked = { onDatePicked.invoke(AppointmentDate(it)) },
    onDateCancel = { onDateCancel.invoke() }
  )
}

@Composable
fun AppointmentDateField(
  fragmentManager: FragmentManager,
  date: AppointmentDate,
  onNewDateSelected: (AppointmentDate) -> Unit
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
    value = date.formmatted().toString(),
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
    interactionSource = interactionSource
  )

  if (showCalendarDialog) {
    AppointmentDatePicker(
      fragmentManager,
      selectedDate = date,
      onDatePicked = {
        onNewDateSelected(it)
        showCalendarDialog = false
      },
      onDateCancel = { showCalendarDialog = false }
    )
  }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T : AppointmentFilterOption> AppointmentExposedDropdown(
  filter: AppointmentFilter<T>,
  onItemSelected: (AppointmentFilter<T>) -> Unit
) {
  LocalExposedDropdownMenuBox(
    selectedItem = filter.selected,
    options = filter.options,
    onItemSelected = { onItemSelected.invoke(filter.copy(selected = it)) }
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
    onFiltersApply = {}
  )
}
