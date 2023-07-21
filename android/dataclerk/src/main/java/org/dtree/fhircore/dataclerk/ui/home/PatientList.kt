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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dtree.fhircore.dataclerk.ui.main.AppMainViewModel
import org.dtree.fhircore.dataclerk.ui.main.PatientItem
import org.dtree.fhircore.dataclerk.ui.patient.localizedString

@Composable
fun PatientList(viewModel: AppMainViewModel, navigate: (PatientItem) -> Unit) {
  val state by viewModel.patientListState

  if (state.loading) {
    CircularProgressIndicator()
  }
  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(8.dp)
  ) {
    items(state.patients) { patient -> PatientItemCard(patient, onClick = { navigate(patient) }) }
  }
}

@Composable
fun PatientItemCard(patient: PatientItem, onClick: () -> Unit) {
  Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
    Column(Modifier.padding(8.dp).fillMaxWidth()) {
      Text(text = patient.name)
      Text(text = patient.dob?.localizedString ?: "")
      Text(text = patient.id)
    }
  }
}
