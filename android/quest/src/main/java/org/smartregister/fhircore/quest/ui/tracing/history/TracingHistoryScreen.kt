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

package org.smartregister.fhircore.quest.ui.tracing.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import org.smartregister.fhircore.engine.domain.model.TracingHistory
import org.smartregister.fhircore.engine.ui.theme.SuccessColor
import org.smartregister.fhircore.engine.util.extension.asDdMmYyyy
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.tracing.components.OutlineCard
import org.smartregister.fhircore.quest.ui.tracing.components.TracingDataScaffoldList

@Composable
fun TracingHistoryScreen(
  navController: NavHostController,
  viewModel: TracingHistoryViewModel = hiltViewModel()
) {
  val context = LocalContext.current
  TracingDataScaffoldList(
    title = stringResource(id = R.string.tracing_history),
    navController = navController,
    viewModel = viewModel,
    content = { history ->
      TracingHistoryCard(
        history = history,
        onClick = {
          viewModel.onEvent(
            TracingHistoryEvent.OpenOutComesScreen(
              context = context,
              navController = navController,
              historyId = history.historyId
            )
          )
        }
      )
    }
  )
}

@Composable
fun TracingHistoryCard(history: TracingHistory, onClick: () -> Unit) {
  OutlineCard(modifier = Modifier.clickable(onClick = onClick)) {
    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
      Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Start Date: ${history.startDate.asDdMmYyyy()}")
      }
      Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = "End Date: ${history.endDate?.asDdMmYyyy() ?: ""}")
      }
      Text(
        text = if (history.isActive) "Active" else "Completed",
        style =
          MaterialTheme.typography.subtitle1.copy(
            color = if (history.isActive) SuccessColor else MaterialTheme.colors.onSurface,
            fontWeight = FontWeight.Bold
          ),
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}
