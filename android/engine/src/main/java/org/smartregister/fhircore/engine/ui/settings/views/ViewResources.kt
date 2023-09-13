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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.Chip
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.engine.ui.settings.DevViewModel
import org.smartregister.fhircore.engine.ui.settings.ResourceField

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun ViewResourceReport(viewModel: DevViewModel) {
  var data by remember { mutableStateOf(mapOf<String, List<ResourceField>>()) }

  LaunchedEffect(viewModel) { data = viewModel.getResourcesToReport() }

  Scaffold { paddingValues ->
    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(paddingValues).padding(horizontal = 12.dp)
    ) {
      data.entries.forEach { group ->
        run {
          stickyHeader { Text(text = group.key, style = MaterialTheme.typography.h5) }
          items(group.value) { item ->
            Card(Modifier.fillMaxWidth()) {
              Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(6.dp).fillMaxWidth()
              ) {
                Chip(onClick = {}, enabled = false) { Text(text = item.version) }

                Text(text = item.id)
                Text(text = item.date)
              }
            }
          }
        }
      }
    }
  }
}
