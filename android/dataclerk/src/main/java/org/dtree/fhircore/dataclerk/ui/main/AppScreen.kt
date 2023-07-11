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

package org.dtree.fhircore.dataclerk.ui.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.fhir.sync.SyncJobStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(appMainViewModel: AppMainViewModel) {
  val appState by appMainViewModel.appMainUiState
  val context = LocalContext.current
  val patientRegistrationLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.StartActivityForResult(),
      onResult = {}
    )
  val syncState by appMainViewModel.syncSharedFlow.collectAsState(initial = SyncJobStatus.Started())

  Scaffold(
    topBar = { TopAppBar(title = { Text(text = appState.appTitle) }) },
    bottomBar = {
      Button(
        onClick = { patientRegistrationLauncher.launch(appMainViewModel.openForm(context)) },
        enabled = syncState is SyncJobStatus.Finished,
        modifier = Modifier.fillMaxWidth()
      ) { Text(text = "Create New Patient") }
    }
  ) { paddingValues ->
    AppScreenBody(
      paddingValues = paddingValues,
      appState = appState,
      syncState = syncState,
      sync = { appMainViewModel.sync() }
    )
  }
}

@Composable
fun AppScreenBody(
  paddingValues: PaddingValues,
  appState: AppMainUiState,
  syncState: SyncJobStatus,
  sync: (() -> Unit)
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceAround,
    modifier = Modifier.padding(paddingValues).fillMaxSize()
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      when (syncState) {
        is SyncJobStatus.InProgress, is SyncJobStatus.Started -> {
          Text(
            text =
              if (syncState is SyncJobStatus.Started)
                stringResource(org.smartregister.fhircore.engine.R.string.syncing_initiated)
              else stringResource(org.smartregister.fhircore.engine.R.string.syncing_in_progress)
          )
          CircularProgressIndicator()
          if (syncState is SyncJobStatus.InProgress) {
            Text(text = "Synced ${syncState.completed}% - ${syncState.total}")
          }
        }
        is SyncJobStatus.Finished -> {
          Text(text = "Sync finished")
          Button(onClick = sync) { Text(text = "Run Sync") }
        }
        is SyncJobStatus.Glitch, is SyncJobStatus.Failed -> {
          Text(text = "Sync failed")
          Button(onClick = sync) { Text(text = "Run Sync") }
        }
        else -> {
          Text(text = "Synced")
        }
      }
    }

    Text(text = "Last Sync: ${appState.lastSyncTime}")
  }
}
