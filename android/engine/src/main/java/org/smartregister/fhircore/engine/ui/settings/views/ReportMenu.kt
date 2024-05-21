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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.domain.util.DataLoadState
import org.smartregister.fhircore.engine.ui.settings.DevViewModel
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

@Composable
fun ReportBottomSheet(viewModel: DevViewModel, viewReport: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val state by viewModel.resourceSaveState.collectAsState()

  Column(
    modifier = Modifier.padding(16.dp).padding(vertical = 20.dp).fillMaxWidth(),
  ) {
    SectionTitle(text = "Application Reports")
    if (state !is DataLoadState.Idle) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        when (state) {
          is DataLoadState.Success -> {
            Icon(Icons.Outlined.CheckCircleOutline, "")
            Text(text = "Report Generated")
          }
          is DataLoadState.Error -> {
            Icon(Icons.Outlined.CheckCircleOutline, "")
            Text(text = "An error occurred")
          }
          else -> {
            CircularProgressIndicator()
            Text(text = "Generated Report")
          }
        }
      }
    }
    UserProfileRow(
      text = "Export Application Report",
      clickListener =
        @ExcludeFromJacocoGeneratedReport {
          scope.launch @ExcludeFromJacocoGeneratedReport { viewModel.createResourceReport(context) }
        },
    )
    UserProfileRow(
      text = "View Resources Version Report",
      clickListener = viewReport,
    )
  }
}
