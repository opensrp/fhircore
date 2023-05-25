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

package org.smartregister.fhircore.quest.ui.tracing.outcomes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import org.smartregister.fhircore.engine.domain.model.TracingOutcome
import org.smartregister.fhircore.engine.util.extension.asDdMmYyyy
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.tracing.components.OutlineCard
import org.smartregister.fhircore.quest.ui.tracing.components.TracingDataScaffoldList

@Composable
fun TracingOutcomesScreen(
  navController: NavHostController,
  viewModel: TracingOutcomesViewModel = hiltViewModel()
) {
  val context = LocalContext.current
  TracingDataScaffoldList(
    title = stringResource(id = R.string.tracing_outcomes),
    navController = navController,
    viewModel = viewModel,
    content = { outcome ->
      TracingOutcomeCard(outcome = outcome) {
        viewModel.onEvent(
          TracingOutcomesEvent.OpenHistoryDetailsScreen(
            context = context,
            navController = navController,
            historyId = it.historyId,
            encounterId = it.encounterId,
            title = it.title
          )
        )
      }
    }
  )
}

@Composable
private fun TracingOutcomeCard(outcome: TracingOutcome, onClick: (TracingOutcome) -> Unit) {
  OutlineCard(modifier = Modifier.clickable { onClick(outcome) }) {
    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
      Text(text = outcome.title, Modifier.fillMaxWidth().padding(bottom = 8.dp))
      Text(text = outcome.date?.asDdMmYyyy() ?: "PENDING", modifier = Modifier.fillMaxWidth())
    }
  }
}
