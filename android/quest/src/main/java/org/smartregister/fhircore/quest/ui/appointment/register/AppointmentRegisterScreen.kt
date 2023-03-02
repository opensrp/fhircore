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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.PageRegisterScreen

@Composable
fun AppointmentRegisterScreen(
  modifier: Modifier = Modifier,
  screenTitle: String,
  navController: NavHostController,
  registerViewModel: AppointmentRegisterViewModel = hiltViewModel()
) {
  PageRegisterScreen(
    modifier = modifier,
    screenTitle = screenTitle,
    navController = navController,
    registerViewModel = registerViewModel,
    filterNavClickAction = { TODO("Show modal dialog for filters") }
  )
}

@Composable
fun FilterAppointmentsModal(onDismissAction: () -> Unit, onFiltersApply: () -> Unit) {
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
          OutlinedTextField(
            value = "",
            onValueChange = {},
            colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.DarkGray),
            maxLines = 1,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
              IconButton(onClick = { /*TODO*/}) {
                Icon(
                  imageVector = Icons.Filled.CalendarToday,
                  contentDescription = "Date",
                  tint = Color.Gray
                )
              }
            },
            placeholder = { Text(text = stringResource(id = R.string.filter_date_of_appointment)) }
          )
          OutlinedTextField(
            value = "",
            onValueChange = {},
            colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.DarkGray),
            maxLines = 1,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
              IconButton(onClick = { /*TODO*/}) {
                Icon(
                  imageVector = Icons.Filled.ExpandMore,
                  contentDescription = "dropdown",
                  tint = Color.Gray
                )
              }
            },
            placeholder = { Text(text = stringResource(id = R.string.filter_patients_assigned)) }
          )
          OutlinedTextField(
            value = "",
            onValueChange = {},
            colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.DarkGray),
            maxLines = 1,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
              IconButton(onClick = { /*TODO*/}) {
                Icon(
                  imageVector = Icons.Filled.ExpandMore,
                  contentDescription = "dropdown",
                  tint = Color.Gray
                )
              }
            },
            placeholder = { Text(text = stringResource(id = R.string.filter_patient_category)) }
          )
          OutlinedTextField(
            value = "",
            onValueChange = {},
            colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.DarkGray),
            maxLines = 1,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
              IconButton(onClick = { /*TODO*/}) {
                Icon(
                  imageVector = Icons.Filled.ExpandMore,
                  contentDescription = "dropdown",
                  tint = Color.Gray
                )
              }
            },
            placeholder = { Text(text = stringResource(id = R.string.filter_appointment_reason)) }
          )
        }

        Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
          TextButton(onClick = { onDismissAction() }, modifier = Modifier.wrapContentWidth()) {
            Text(text = stringResource(id = R.string.cancel).uppercase())
          }

          TextButton(onClick = { onFiltersApply() }, modifier = Modifier.wrapContentWidth()) {
            Text(text = stringResource(id = R.string.apply).uppercase())
          }
        }
      }
    }
  }
}

@Composable
@Preview
fun PreviewFilterAppointmentsModal() {
  FilterAppointmentsModal(onDismissAction = {}, onFiltersApply = {})
}
