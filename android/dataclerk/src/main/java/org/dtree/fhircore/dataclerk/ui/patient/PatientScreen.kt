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

@file:OptIn(ExperimentalMaterial3Api::class)

package org.dtree.fhircore.dataclerk.ui.patient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.fhir.sync.SyncJobStatus
import kotlinx.coroutines.flow.MutableStateFlow
import org.dtree.fhircore.dataclerk.ui.main.AppMainViewModel
import org.dtree.fhircore.dataclerk.util.extractName
import org.hl7.fhir.r4.model.Practitioner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientScreen(
  navController: NavController,
  appMainViewModel: AppMainViewModel,
  patientViewModel: PatientViewModel = hiltViewModel()
) {
  val state by patientViewModel.screenState.collectAsState()
  val list by patientViewModel.resourceMapStatus
  val syncState by appMainViewModel.syncSharedFlow.collectAsState(initial = null)
  val refreshKey by appMainViewModel.refreshHash

  LaunchedEffect(syncState) {
    if (syncState is SyncJobStatus.Finished) {
      patientViewModel.fetchPatient()
    }
  }

  LaunchedEffect(refreshKey) { if (refreshKey.isNotBlank()) patientViewModel.fetchPatient() }

  Scaffold(
    topBar = {
      TopAppBar(
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "")
          }
        },
        title = {
          if (state is PatientDetailScreenState.Success) {
            Text(text = (state as PatientDetailScreenState.Success).patientDetail.name)
          }
        },
        actions = {}
      )
    }
  ) { paddingValues ->
    Column(Modifier.padding(paddingValues).fillMaxSize()) {
      val context = LocalContext.current
      if (state is PatientDetailScreenState.Success) {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(8.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items((state as PatientDetailScreenState.Success).detailsData) { data ->
            when (data) {
              is PatientDetailHeader -> PatientDetailsCardViewBinding(data)
              is PatientDetailProperty -> PatientListItemViewBinding(data)
              is PatientDetailOverview ->
                PatientDetailsHeaderBinding(data) { patientViewModel.editPatient(context) }
              is PatientReferenceProperty ->
                list[data.patientProperty.value]?.let { PatientReferencePropertyBinding(data, it) }
            }
          }
        }
      } else {
        CircularProgressIndicator()
      }
    }
  }
}

@Composable
fun PatientDetailsCardViewBinding(data: PatientDetailHeader) {
  Text(text = data.header, modifier = Modifier.fillMaxWidth())
}

@Composable
fun PatientDetailsHeaderBinding(data: PatientDetailOverview, editPatient: () -> Unit = {}) {

  Card(modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(Constants.defaultCardPadding)) {
      Text(
        text = data.patient.name,
        style =
          MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontStyle = FontStyle.Normal)
      )
      Row(Modifier.padding(8.dp)) {
        Text(text = "Type", style = MaterialTheme.typography.bodyMedium)
        Box(modifier = Modifier.width(8.dp))
        Text(text = data.patient.healthStatus.display, style = MaterialTheme.typography.bodyMedium)
      }
      Box(modifier = Modifier.height(12.dp))
      Button(onClick = editPatient, modifier = Modifier.fillMaxWidth()) {
        Text(text = "Edit Profile")
      }
    }
  }
}

@Composable
fun PatientListItemViewBinding(data: PatientDetailProperty) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(Constants.defaultCardPadding)) {
      Text(
        text = data.patientProperty.header,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
      Box(modifier = Modifier.height(8.dp))
      Text(
        text = data.patientProperty.value,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1
      )
    }
  }
}

@Composable
fun PatientReferencePropertyBinding(
  data: PatientReferenceProperty,
  value: MutableStateFlow<ResourcePropertyState>
) {
  val state by value.collectAsState()

  Card(modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(Constants.defaultCardPadding)) {
      Text(
        text = data.patientProperty.header,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
      Box(modifier = Modifier.height(8.dp))
      if (state is ResourcePropertyState.Error) {
        Text(
          text = (state as ResourcePropertyState.Error).message,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 1
        )
      } else if (state is ResourcePropertyState.Success) {
        val resource = (state as ResourcePropertyState.Success).resource
        if (resource is Practitioner) {
          Text(
            text = resource.extractName(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1
          )
        }
      } else {
        CircularProgressIndicator()
      }
    }
  }
}

object Constants {
  val defaultCardPadding = 12.dp
}
