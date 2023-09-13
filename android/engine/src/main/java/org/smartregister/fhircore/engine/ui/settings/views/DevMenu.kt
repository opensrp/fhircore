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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.ui.settings.DevViewModel

@Composable
fun DevMenu(viewModel: DevViewModel, viewRes: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  Column(
    verticalArrangement = Arrangement.spacedBy(6.dp),
    modifier = Modifier.padding(16.dp).fillMaxWidth()
  ) {
    Button(
      modifier = Modifier.fillMaxWidth(),
      onClick = { scope.launch { viewModel.createResourceReport(context) } }
    ) { Text(text = "Export Report Resources") }
    Button(modifier = Modifier.fillMaxWidth(), onClick = viewRes) {
      Text(text = "View Report Resources")
    }
    Button(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.fetchDetails() }) {
      Text(text = "Test Fetch")
    }
  }
}
