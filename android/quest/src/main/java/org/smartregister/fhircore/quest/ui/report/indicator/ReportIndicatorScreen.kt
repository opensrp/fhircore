/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.report.indicator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.report.indicator.ReportIndicator
import org.smartregister.fhircore.quest.navigation.ReportIndicatorNavigation

@Composable
fun ReportIndicatorScreen(
  reportId: String,
  reportIndicatorViewModel: ReportIndicatorViewModel,
  mainNavController: NavController,
) {
  LaunchedEffect(Unit) { reportIndicatorViewModel.retrieveIndicators(reportId) }

  // Use a different navController internally for navigating Report Indicator Composable screens
  NavHost(
    navController = rememberNavController(),
    startDestination = ReportIndicatorNavigation.ReportIndicatorList.route,
  ) {
    composable(ReportIndicatorNavigation.ReportIndicatorList.route) {
      ReportIndicatorList(
        navController = mainNavController,
        reportIndicatorViewModel.reportIndicatorsMap,
      )
    }
  }
}

@Composable
fun ReportIndicatorList(
  navController: NavController,
  indicatorsMap: SnapshotStateMap<String, Pair<ReportIndicator, Long>>,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(text = stringResource(R.string.reports)) },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
          }
        },
        contentColor = Color.White,
        backgroundColor = MaterialTheme.colors.primary,
      )
    },
  ) { innerPadding ->
    Box(modifier = Modifier.padding(innerPadding)) {
      LazyColumn {
        items(
          items = indicatorsMap.values.toList(),
          key = { pair -> pair.first.id },
        ) { pair ->
          val (indicator, count) = pair
          IndicatorListItem(indicator, count)
        }
      }
    }
  }
}

@Composable
private fun IndicatorListItem(
  indicator: ReportIndicator,
  count: Long,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column {
      Text(
        text = indicator.title,
        fontSize = 16.sp,
        style = MaterialTheme.typography.h5,
      )
      Spacer(modifier = Modifier.height(8.dp))
      if (indicator.subtitle?.isNotBlank() == true) {
        Text(
          text = indicator.subtitle!!,
          fontSize = 14.sp,
          fontWeight = FontWeight.Thin,
        )
      }
    }
    Text(
      text = count.toString(),
      fontSize = 18.sp,
      style = MaterialTheme.typography.h5,
      fontWeight = FontWeight.Bold,
    )
  }
}
