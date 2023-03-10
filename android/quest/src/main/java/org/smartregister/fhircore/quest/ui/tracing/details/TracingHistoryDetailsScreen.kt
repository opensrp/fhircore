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

package org.smartregister.fhircore.quest.ui.tracing.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import java.util.Date
import org.smartregister.fhircore.quest.ui.tracing.components.InfoBoxItem
import org.smartregister.fhircore.quest.ui.tracing.components.OutlineCard

@Composable
fun TracingHistoryDetailsScreen(
  navController: NavHostController,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Phone Tracing Outcome 2") },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Filled.ArrowBack, null)
          }
        },
      )
    }
  ) { innerPadding ->
    Column(
      Modifier.padding(innerPadding).padding(horizontal = 12.dp, vertical = 8.dp).fillMaxSize()
    ) {
      OutlineCard() {
        Column(
          modifier = Modifier.padding(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          InfoBoxItem(title = "Date:", value = Date().toString())
          InfoBoxItem(title = "Reason for tracing:", value = "High Viral Load")
          InfoBoxItem(title = "Spoke to patient:", value = "No")
          InfoBoxItem(title = "Tracing Outcome:", value = "Number does not belong to client")
          InfoBoxItem(title = "Date of Clinic Appointment:", value = "N/A")
        }
      }
    }
  }
}
