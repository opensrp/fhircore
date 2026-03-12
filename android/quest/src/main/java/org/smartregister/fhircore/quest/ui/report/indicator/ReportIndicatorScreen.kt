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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.smartregister.fhircore.engine.configuration.report.indicator.ReportIndicator
import org.smartregister.fhircore.quest.navigation.ReportIndicatorNavigation

@Composable
fun ReportIndicatorScreen(
  reportId: String,
  reportIndicatorViewModel: ReportIndicatorViewModel,
  mainNavController: NavController,
) {
  val navController = rememberNavController()

  NavHost(
    navController = navController,
    startDestination = ReportIndicatorNavigation.DateSelector.route,
  ) {
    composable(ReportIndicatorNavigation.DateSelector.route) {
      ReportIndicatorDateSelectorScreen(
        reportId = reportId,
        reportIndicatorViewModel = reportIndicatorViewModel,
        navController = navController,
        mainNavController = mainNavController,
      )
    }

    composable(
      route = ReportIndicatorNavigation.ReportIndicatorList.route,
      arguments =
        listOf(
          navArgument("reportId") { type = NavType.StringType },
          navArgument("startDate") { type = NavType.StringType },
          navArgument("endDate") { type = NavType.StringType },
          navArgument("periodLabel") { type = NavType.StringType },
        ),
    ) { backStackEntry ->
      val reportId = backStackEntry.arguments?.getString("reportId") ?: ""
      val startDate = backStackEntry.arguments?.getString("startDate") ?: ""
      val endDate = backStackEntry.arguments?.getString("endDate") ?: ""
      val periodLabel = backStackEntry.arguments?.getString("periodLabel") ?: ""

      ReportIndicatorList(
        navController = navController,
        reportIndicatorViewModel = reportIndicatorViewModel,
        reportId = reportId,
        startDate = startDate,
        endDate = endDate,
        periodLabel = periodLabel,
      )
    }
  }
}

@Composable
fun ReportIndicatorList(
  navController: NavController,
  reportIndicatorViewModel: ReportIndicatorViewModel,
  reportId: String,
  startDate: String,
  endDate: String,
  periodLabel: String,
) {
  LaunchedEffect(reportId, startDate, endDate) {
    reportIndicatorViewModel.retrieveIndicators(reportId, startDate, endDate, periodLabel)
  }

  val indicatorsMap = reportIndicatorViewModel.reportIndicatorsMap
  val isLoading = reportIndicatorViewModel.isLoading.value

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(text = periodLabel) },
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
      Column {
        if (isLoading) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator()
          }
        } else if (indicatorsMap.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text =
                stringResource(
                  org.smartregister.fhircore.quest.R.string.no_encounter_data_available,
                ),
              textAlign = TextAlign.Center,
              fontSize = 16.sp,
              modifier = Modifier.padding(16.dp),
            )
          }
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(
              items = indicatorsMap.values.toList(),
              key = { pair -> pair.first.id },
            ) { pair ->
              val (indicator, count) = pair
              IndicatorListItem(indicator, count)
              Divider()
            }
          }
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
