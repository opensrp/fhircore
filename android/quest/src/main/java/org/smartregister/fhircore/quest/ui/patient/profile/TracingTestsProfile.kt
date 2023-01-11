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

package org.smartregister.fhircore.quest.ui.patient.profile

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.ui.components.CircularProgressBar
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.quest.ui.report.measure.components.showDateRangePicker

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TracingTestsProfile(viewModel: TracingTestsViewModel = hiltViewModel()) {
  val context = LocalContext.current
  val isOnTracing by viewModel.hasTracing.observeAsState(initial = false)
  var showOverflowMenu by remember { mutableStateOf(false) }

  val activeTracingTaskReasons by viewModel.activeTracingTasksFlow.collectAsState()

  val coroutineScope = rememberCoroutineScope()
  val modalBottomSheetState =
    rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
  val datePickerBuilder = MaterialDatePicker.Builder.datePicker()
    .apply {
      setCalendarConstraints(CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now()).build())
      setTitleText("Change dates")
    }

  ModalBottomSheetLayout(
    sheetState = modalBottomSheetState,
    sheetContent = {
      LazyColumn {
        itemsIndexed(activeTracingTaskReasons) { index, elem ->
          Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(elem.first, style = MaterialTheme.typography.body1)
              if (elem.second > 1)
                Text(
                  "${elem.second} times",
                  style = MaterialTheme.typography.caption,
                  fontStyle = FontStyle.Italic,
                  fontWeight = FontWeight.Light
                )
            }
            IconButton(onClick = { viewModel.removeTracingTask(context, elem.third) }) {
              Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Remove",
                tint = MaterialTheme.colors.primary
              )
            }
          }
          if (index < activeTracingTaskReasons.lastIndex) Divider()
        }
      }
    }
  ) {
    if (viewModel.patientId.isBlank()) {
      CircularProgressBar()
    } else {
      Scaffold(
        modifier = Modifier.fillMaxWidth(),
        topBar = {
          TopAppBar(
            navigationIcon = {
              IconButton(onClick = { /*TODO*/}) {
                Icon(Icons.Default.ArrowBack, contentDescription = "")
              }
            },
            title = { Text(text = "Tracing Stuff") },
            actions = {
              IconButton(onClick = { showOverflowMenu = !showOverflowMenu }) {
                Icon(
                  imageVector = Icons.Outlined.MoreVert,
                  contentDescription = null,
                  tint = Color.White
                )
              }
              DropdownMenu(
                expanded = showOverflowMenu,
                onDismissRequest = { showOverflowMenu = !showOverflowMenu }
              ) {
                DropdownMenuItem(
                  onClick = {
                    showOverflowMenu = false
                    viewModel.updateUserWithTracing(isHomeTracing = false)
                  },
                  enabled = !isOnTracing
                ) { Text(text = "Add Home Tracing") }
                DropdownMenuItem(
                  onClick = {
                    showOverflowMenu = false
                    viewModel.updateUserWithTracing(isHomeTracing = true)
                  },
                  enabled = !isOnTracing
                ) { Text(text = ("Add Phone Tracing")) }
                DropdownMenuItem(
                  onClick = {
                    showOverflowMenu = false
                    viewModel.clearAllTracingData()
                  }
                ) { Text(text = "Clear Tracing Data for All") }
                DropdownMenuItem(
                  onClick = {
                    showOverflowMenu = false
                    datePickerBuilder
                      .apply {
                        setSelection(viewModel.currentDate().time)
                      }
                      .build()
                      .apply {
                        addOnPositiveButtonClickListener {
                          viewModel.setUpperDate(it)
                        }
                      }
                      .show((context as AppCompatActivity).supportFragmentManager, "TRACING_DATE_PICKER_DIALOG_TAG")
                  }
                ) { Text(text = "Change Date") }
              }
            },
          )
        },
        bottomBar = {
          if (activeTracingTaskReasons.isNotEmpty()) {
            OutlinedButton(
              modifier = Modifier.fillMaxWidth().padding(8.dp),
              border = BorderStroke(1.dp, color = MaterialTheme.colors.primary),
              onClick = { coroutineScope.launch { modalBottomSheetState.show() } }
            ) { Text(text = "Show Active Reasons (${activeTracingTaskReasons.size})") }
          }
        }
      ) {
        LazyColumn(Modifier.fillMaxWidth().padding(it)) {
          items(TracingTestsViewModel.testItems) { item ->
            Button(
              onClick = { viewModel.open(context, item) },
              modifier = Modifier.fillMaxWidth()
            ) { Text(text = item.title) }
          }
        }
      }
    }
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PreviewTestTracing() {
  Column {
    val numOfReasons = 7
    OutlinedButton(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      border = BorderStroke(1.dp, color = MaterialTheme.colors.primary),
      onClick = {}
    ) { Text(text = "Show Active Reasons ($numOfReasons)") }

    val reasons = listOf<Int>(1, 2)

    LazyColumn {
      itemsIndexed(reasons) { index, elem ->
        Row(
          modifier = Modifier.fillMaxWidth().padding(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Missed Routine Appointment", style = MaterialTheme.typography.body1)
            Text(
              "5 times",
              style = MaterialTheme.typography.caption,
              fontStyle = FontStyle.Italic,
              fontWeight = FontWeight.Light
            )
          }
          IconButton(onClick = { /*TODO*/}) {
            Icon(
              imageVector = Icons.Outlined.Delete,
              contentDescription = "Remove",
              tint = MaterialTheme.colors.primary
            )
          }
        }
        if (index < reasons.lastIndex) Divider()
      }
    }
  }
}
