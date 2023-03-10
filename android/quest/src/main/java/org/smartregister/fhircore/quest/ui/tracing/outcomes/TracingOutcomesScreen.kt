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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import java.util.Date
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.tracing.components.OutlineCard

@Composable
fun TracingOutcomesScreen(
  navController: NavHostController,
  viewModel: TracingOutcomesViewModel = hiltViewModel()
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(id = R.string.tracing_history)) },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Filled.ArrowBack, null)
          }
        },
      )
    }
  ) { innerPadding ->
    TracingOutcomesScreenContainer(
      viewModel = viewModel,
      navController = navController,
      modifier = Modifier.padding(innerPadding)
    )
  }
}

@Composable
fun TracingOutcomesScreenContainer(
  viewModel: TracingOutcomesViewModel,
  navController: NavHostController,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier.fillMaxSize().padding(horizontal = 12.dp)
  ) {
    item { Spacer(modifier = Modifier.height(8.dp)) }
    items(listOf(1, 2, 3, 4)) {
      OutlineCard(
        modifier =
          Modifier.clickable {
            viewModel.onEvent(
              TracingOutcomesEvent.OpenHistoryDetailsScreen(
                context = context,
                navController = navController
              )
            )
          }
      ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
          Text(text = "Start Date:", Modifier.fillMaxWidth().padding(bottom = 8.dp))
          Text(text = Date().toString(), modifier = Modifier.fillMaxWidth())
        }
      }
    }
  }
}
