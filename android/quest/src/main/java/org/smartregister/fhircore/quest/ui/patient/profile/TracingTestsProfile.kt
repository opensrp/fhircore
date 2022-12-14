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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.smartregister.fhircore.engine.ui.components.CircularProgressBar

@Composable
fun TracingTestsProfile(viewModel: TracingTestsViewModel = hiltViewModel()) {
  val context = LocalContext.current
  val patient by viewModel.patientProfileViewData.collectAsState()
  val isOnTracing by viewModel.hasTracing.observeAsState(initial = false)

  if (viewModel.patientId.isBlank()) {
    CircularProgressBar()
  } else {
    Scaffold(
      modifier = Modifier.fillMaxWidth(),
      topBar = {
        TopAppBar(
          navigationIcon = {
            IconButton(onClick = { /*TODO*/ }) {
              Icon(Icons.Default.ArrowBack, contentDescription = "")
            }
          },
          title = { Text(text = "Tracing Stuff") }
        )
      },
      bottomBar = {
        Column(Modifier.fillMaxWidth()) {
          Button(onClick = { viewModel.clearAllTracingData() }) {
            Text(text = "Clear Tracing Data for All")
          }
          Box(modifier = Modifier
            .height(1.dp).background(Color.Blue)
            .fillMaxWidth())
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(
              onClick = { viewModel.updateUserWithTracing(isHomeTracing = true) },
              enabled = !isOnTracing
            ) {
              Text(text = "Add Home Tracing")
            }
            Button(
              onClick = { viewModel.updateUserWithTracing(isHomeTracing = false) },
              enabled = !isOnTracing
            ) {
              Text(text = "Add Phone Tracing")
            }
          }
        }
      }
    ) {
      LazyColumn(Modifier.fillMaxWidth()) {
        items(TracingTestsViewModel.testItems) { item ->
          Button(onClick = { viewModel.open(context, item) }, modifier = Modifier.fillMaxWidth()) {
            Text(text = item.title)
          }
        }
      }
    }
  }
}
