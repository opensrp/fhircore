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

package org.dtree.fhircore.dataclerk.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.fhir.sync.SyncJobStatus
import org.dtree.fhircore.dataclerk.ui.main.AppMainViewModel
import org.dtree.fhircore.dataclerk.ui.main.PatientItem
import org.smartregister.fhircore.engine.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  appMainViewModel: AppMainViewModel,
  homeViewModel: HomeViewModel = hiltViewModel(),
  sync: () -> Unit,
  openPatient: (PatientItem) -> Unit
) {
  val appState by appMainViewModel.appMainUiState
  val context = LocalContext.current
  val patientRegistrationLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.StartActivityForResult(),
      onResult = {}
    )
  val syncState by appMainViewModel.syncSharedFlow.collectAsState(initial = null)
  val refreshKey by appMainViewModel.refreshHash

  LaunchedEffect(syncState) {
    if (syncState is SyncJobStatus.Finished) {
      homeViewModel.refresh()
    }
  }

  LaunchedEffect(refreshKey) { if (refreshKey.isNotBlank()) homeViewModel.refresh() }

  Scaffold(
    topBar = {
      Column(Modifier.fillMaxWidth()) {
        TopAppBar(
          title = { Text(text = appState.appTitle) },
          actions = {
            AppScreenBody(
              syncState = syncState,
              sync = sync,
            )
            IconButton(onClick = { homeViewModel.refresh() }) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
              )
            }
          }
        )
        SyncStatusBar(
          syncState = syncState,
        )
      }
    },
    bottomBar = {
      if (!appState.isInitialSync)
        Button(
          onClick = { patientRegistrationLauncher.launch(appMainViewModel.openForm(context)) },
          modifier = Modifier.fillMaxWidth()
        ) { Text(text = appState.registrationButton) }
    }
  ) { paddingValues ->
    Column(Modifier.padding(paddingValues)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val count by homeViewModel.patientCount
        Text(text = "Patients: $count")
        Text(text = "Last Sync: ${appState.lastSyncTime}")
      }
      PatientList(viewModel = homeViewModel, navigate = openPatient)
    }
  }
}

@Composable
fun SyncStatusBar(
  syncState: SyncJobStatus?,
) {
  if (syncState is SyncJobStatus.InProgress) {
    val progress =
      syncState
        .let { it.completed.toDouble().div(it.total) }
        .let { if (it.isNaN()) 0.0 else it }
        .times(100)
        .div(100)
        .toFloat()
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = progress)
  } else if (syncState is SyncJobStatus.Started) {
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
  }
}

@Composable
fun AppScreenBody(syncState: SyncJobStatus?, sync: () -> Unit) {
  Row() {
    Button(
      onClick = sync,
      enabled = !(syncState is SyncJobStatus.InProgress || syncState is SyncJobStatus.Started)
    ) {
      when (syncState) {
        is SyncJobStatus.InProgress, is SyncJobStatus.Started -> {
          Text(
            text =
              if (syncState is SyncJobStatus.Started) stringResource(R.string.syncing_initiated)
              else
                "${(syncState as SyncJobStatus.InProgress).syncOperation.name.lowercase()}ing...",
          )
        }
        is SyncJobStatus.Finished -> {
          Text(text = "Sync (finished)")
        }
        is SyncJobStatus.Glitch, is SyncJobStatus.Failed -> {
          Text(text = "Sync (failed)")
        }
        else -> {
          Text(text = "Run Sync")
        }
      }
    }
  }
}
