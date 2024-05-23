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

package org.smartregister.fhircore.engine.ui.settings.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import org.smartregister.fhircore.engine.ui.settings.DevViewModel
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

@ExcludeFromJacocoGeneratedReport
@Composable
fun DevMenu(viewModel: DevViewModel) {
  val context = LocalContext.current
  val missedTasks by viewModel.observeMissedTask(context).collectAsState(listOf())
  val appointmentList by viewModel.observeMissedAppointment(context).collectAsState(listOf())
  val interruptedList by viewModel.observeInterrupted(context).collectAsState(listOf())
  val resourcePurger by viewModel.observeResourcePurgerWorker(context).collectAsState(listOf())

  Column(
    modifier = Modifier.padding(16.dp).padding(vertical = 20.dp).fillMaxWidth(),
  ) {
    SectionTitle(text = "Developer Options")
    UserProfileRow(
      iconAlt = { WorkerStateIcon(states = missedTasks) },
      text = "Run missed task worker",
      clickListener = @ExcludeFromJacocoGeneratedReport { viewModel.missedTask(context) },
    )
    UserProfileRow(
      iconAlt = { WorkerStateIcon(states = appointmentList) },
      text = "Run missed appointments worker",
      clickListener = @ExcludeFromJacocoGeneratedReport { viewModel.missedAppointment(context) },
    )
    UserProfileRow(
      iconAlt = { WorkerStateIcon(states = interruptedList) },
      text = "Run interrupted treatment worker",
      clickListener = @ExcludeFromJacocoGeneratedReport { viewModel.interruptedResource(context) },
    )
    UserProfileRow(
      iconAlt = { WorkerStateIcon(states = resourcePurger) },
      text = "Run Resource Purger Worker",
      clickListener = @ExcludeFromJacocoGeneratedReport { viewModel.resourcePurger(context) },
    )
  }
}

@Composable
fun WorkerStateIcon(states: List<WorkInfo.State>) {
  val state = states.firstOrNull()

  when (state) {
    WorkInfo.State.RUNNING -> CircularProgressIndicator(modifier = Modifier.size(18.dp))
    WorkInfo.State.SUCCEEDED ->
      Icon(Icons.Outlined.CheckCircleOutline, contentDescription = "", tint = Color.Green)
    WorkInfo.State.FAILED ->
      Icon(Icons.Outlined.ErrorOutline, contentDescription = "", tint = Color.Red)
    else -> {}
  }
}
