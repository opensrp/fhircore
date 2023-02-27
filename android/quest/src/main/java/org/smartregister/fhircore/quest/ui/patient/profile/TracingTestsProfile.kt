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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.smartregister.fhircore.engine.ui.components.CircularProgressBar

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TracingTestsProfile(viewModel: TracingTestsViewModel = hiltViewModel()) {
  val context = LocalContext.current
  val isOnTracing by viewModel.hasTracing.observeAsState(initial = false)

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
          title = { Text(text = "Tracing and Appointments Debug") }
        )
      },
      bottomBar = {
        Column(Modifier.fillMaxWidth()) {
          Button(onClick = { viewModel.clearAllTracingData() }) {
            Text(text = "Clear Tracing Data for All")
          }
          Box(modifier = Modifier.height(1.dp).background(Color.Blue).fillMaxWidth())
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Button(
              onClick = { viewModel.updateUserWithTracing(isHomeTracing = true) },
              enabled = !isOnTracing
            ) { Text(text = "Add Home Tracing") }
            Button(
              onClick = { viewModel.updateUserWithTracing(isHomeTracing = false) },
              enabled = !isOnTracing
            ) { Text(text = "Add Phone Tracing") }
            Button(
                    onClick = { viewModel.addPregnancy() },
            ) { Text(text = "Make Pregnant") }
          }
        }
      }
    ) { paddingValues ->
      LazyColumn(
        modifier =
          Modifier.fillMaxWidth()
            .padding(paddingValues)
            .padding(horizontal = 12.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(TracingTestsViewModel.testItems) { item ->
          when (item) {
            is TestItem.DividerItem -> {
              Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                Box(Modifier.height(1.dp).fillMaxWidth().background(Color.Black)) {}
              }
            }
            is TestItem.QuestItem -> {
              Card(
                onClick = { viewModel.open(context, item) },
                border = BorderStroke(1.dp, MaterialTheme.colors.secondary),
                elevation = 0.dp,
                contentColor = MaterialTheme.colors.onSurface,
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(Modifier.padding(12.dp)) {
                  Text(text = item.title)
                  if (item.tracingList.isNotEmpty()) {
                    QuestContainer("Tracing", item.tracingList)
                  }
                  if (item.appointmentList.isNotEmpty()) {
                    QuestContainer("Appointments", item.appointmentList)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun QuestContainer(title: String, list: List<String>) {
  val annotatedString = buildAnnotatedString {
    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append("$title: ") }
    list.forEach { item ->
      withStyle(
        style =
          if (!item.contains("-"))
            SpanStyle(
              background = MaterialTheme.colors.primary,
              color = MaterialTheme.colors.onPrimary
            )
          else
            SpanStyle(background = MaterialTheme.colors.error, color = MaterialTheme.colors.onError)
      ) { append(item.replace("-", "")) }
      append(" ")
    }
  }
  Text(text = annotatedString, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
}
