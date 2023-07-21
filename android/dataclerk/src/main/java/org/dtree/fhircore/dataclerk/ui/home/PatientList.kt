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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import org.dtree.fhircore.dataclerk.ui.main.PatientItem
import org.dtree.fhircore.dataclerk.ui.patient.Constants
import org.dtree.fhircore.dataclerk.util.getFormattedAge
import org.smartregister.fhircore.engine.ui.components.ErrorMessage
import timber.log.Timber

@Composable
fun PatientList(viewModel: HomeViewModel, navigate: (PatientItem) -> Unit) {
  val source by viewModel.patientsPaging.collectAsState()
  val patients = source.collectAsLazyPagingItems()

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(8.dp)
  ) {
    items(items = patients.itemSnapshotList, key = { it?.resourceId ?: "" }) { patient ->
      if (patient != null) {
        PatientItemCard(patient, onClick = { navigate(patient) })
      }
    }
    when (val state = patients.loadState.refresh) { // FIRST LOAD
      is LoadState.Error -> {
        item {
          ErrorMessage(
            message = state.error.also { Timber.e(it) }.localizedMessage!!,
            onClickRetry = { patients.retry() }
          )
        }
      }
      is LoadState.Loading -> { // Loading UI
        item {
          Column(
            modifier = Modifier.fillParentMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Text(modifier = Modifier.padding(8.dp), text = "Refresh Loading")

            CircularProgressIndicator(color = Color.Black)
          }
        }
      }
      else -> {}
    }
    when (val state = patients.loadState.append) { // Pagination
      is LoadState.Error -> {
        item {
          ErrorMessage(
            message = state.error.also { Timber.e(it) }.localizedMessage!!,
            onClickRetry = { patients.retry() }
          )
        }
      }
      is LoadState.Loading -> { // Pagination Loading UI
        item {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Text(text = "Pagination Loading")

            CircularProgressIndicator(color = Color.Black)
          }
        }
      }
      else -> {}
    }
  }
}

@Composable
fun PatientItemCard(patient: PatientItem, onClick: () -> Unit) {
  OutlinedCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
    Column(Modifier.padding(Constants.defaultCardPadding).fillMaxWidth()) {
      Text(
        text = patient.name,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
      )
      Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier =
            Modifier.background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
              )
              .padding(8.dp)
        ) {
          Text(
            text = "Id: #${patient.id}",
            style =
              MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onPrimary)
          )
        }
        Text(text = getFormattedAge(patient, LocalContext.current.resources))
      }
    }
  }
}
