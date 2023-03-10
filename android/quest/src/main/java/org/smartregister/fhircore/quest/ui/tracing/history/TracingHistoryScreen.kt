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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
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
import org.smartregister.fhircore.engine.ui.theme.StatusTextColor
import org.smartregister.fhircore.quest.R

@Composable
fun TracingHistoryScreen(
  navController: NavHostController,
  viewModel: TracingHistoryViewModel = hiltViewModel()
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
    TracingHistoryScreenContainer(
      navController = navController,
      modifier = Modifier.padding(innerPadding),
      viewModel = viewModel
    )
  }
}

@Composable
fun TracingHistoryScreenContainer(
  navController: NavHostController,
  modifier: Modifier = Modifier,
  viewModel: TracingHistoryViewModel,
) {
  val context = LocalContext.current
  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier.fillMaxSize().padding(horizontal = 12.dp)
  ) {
    item { Spacer(modifier = Modifier.height(8.dp)) }
    items(listOf(1, 2, 3, 4)) {
      Card(
        elevation = 0.dp,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(width = 2.dp, color = StatusTextColor),
        modifier =
          Modifier.fillMaxWidth()
            .clickable(
              onClick = {
                viewModel.onEvent(
                  TracingHistoryEvent.OpenOutComesScreen(
                    context = context,
                    navController = navController
                  )
                )
              }
            )
      ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
          Row(modifier = Modifier.fillMaxWidth()) { Text(text = "Start Date:") }
          Row(modifier = Modifier.fillMaxWidth()) { Text(text = "End Date:") }
          Text(text = "Active", modifier = Modifier.fillMaxWidth())
        }
      }
    }
  }
}
