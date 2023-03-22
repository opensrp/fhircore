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

package org.smartregister.fhircore.quest.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentManager
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.Date

@Composable
fun LocalDatePickerDialog(
  fragmentManager: FragmentManager,
  datePickerTag: String,
  selectedDate: Date,
  onDatePicked: (Date) -> Unit,
  onDateCancel: () -> Unit
) {
  val datePicker = MaterialDatePicker.Builder.datePicker().setSelection(selectedDate.time).build()

  datePicker.addOnPositiveButtonClickListener { onDatePicked.invoke(Date(it)) }
  datePicker.isCancelable = false
  datePicker.addOnNegativeButtonClickListener { onDateCancel.invoke() }
  if (fragmentManager.findFragmentByTag(datePickerTag) == null) {
    datePicker.show(fragmentManager, datePickerTag)
  }
}

typealias Text = String

@ExperimentalMaterialApi
@Composable
fun <T : FilterOption> LocalExposedDropdownMenuBox(
  selectedItem: T,
  options: Iterable<T>,
  onItemSelected: (T) -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  var isExpanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
    expanded = isExpanded,
    onExpandedChange = { isExpanded = !isExpanded },
    modifier = Modifier.fillMaxWidth()
  ) {
    OutlinedTextField(
      value = selectedItem.text(),
      onValueChange = { /* No-op */},
      readOnly = true,
      trailingIcon = {
        TrailingIcon(expanded = isExpanded, onIconClick = { isExpanded = !isExpanded })
      },
      colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(textColor = Color.DarkGray),
      maxLines = 1,
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
      interactionSource = interactionSource
    )

    ExposedDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
      options.forEach {
        DropdownMenuItem(
          onClick = {
            onItemSelected(it)
            isExpanded = false
          }
        ) { Text(text = it.text()) }
      }
    }
  }
}
