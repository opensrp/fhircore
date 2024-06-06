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

package org.smartregister.fhircore.quest.ui.counters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import org.smartregister.fhircore.quest.R

@Composable
fun CountersScreen(
  navController: NavHostController,
  modifier: Modifier = Modifier,
  countersViewModel: CountersViewModel = hiltViewModel(),
) {
  Scaffold(
    topBar = {
      Column(
        modifier = modifier
          .fillMaxWidth()
          .background(MaterialTheme.colors.primary),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = modifier.padding(vertical = 8.dp),
        ) {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(
              Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = Color.White,
            )
          }
          Text(
            text = stringResource(id = R.string.counters),
            fontSize = 20.sp,
            color = Color.White,
            modifier = Modifier.weight(1f),
          )
        }
      }
    },
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      val isRefreshing by countersViewModel.isRefreshing.collectAsState()

      val patientsCount by countersViewModel.patientsCountStateFlow.collectAsState()
      val isRefreshingPatientsCount by countersViewModel.isRefreshingPatientsCountStateFlow.collectAsState()

      val homeTracingCount by countersViewModel.homeTracingCountStateFlow.collectAsState()
      val isRefreshingHomeTracingCount by countersViewModel.isRefreshingHomeTracingCountStateFlow.collectAsState()

      val phoneTracingCount by countersViewModel.phoneTracingCountStateFlow.collectAsState()
      val isRefreshingPhoneTracingCount by countersViewModel.isRefreshingPhoneTracingCountStateFlow.collectAsState()

      val appointmentsCount by countersViewModel.appointmentsCountStateFlow.collectAsState()
      val isRefreshingAppointmentsCount by countersViewModel.isRefreshingAppointmentsCountStateFlow.collectAsState()

      SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = { countersViewModel.refresh() },
        //        indicator = { _, _ -> }
      ) {
        LazyColumn {
          item {
            Card(
              modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            ) {
              Box(
                modifier = Modifier.padding(8.dp),
              ) {
                Column {
                  Text(
                    text = stringResource(R.string.patients_counter_label),
                    style = MaterialTheme.typography.h4.copy(color = Color.Gray),
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = "$patientsCount",
                    style = if (isRefreshingPatientsCount) MaterialTheme.typography.h2.copy(color = Color.Gray.copy(alpha = 0.5F)) else MaterialTheme.typography.h2,
                  )
                }
              }
            }
          }

          item {
            Card(
              modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            ) {
              Box(
                modifier = Modifier.padding(8.dp),
              ) {
                Column {
                  Text(
                    text = stringResource(R.string.home_tracing_conter_label),
                    style = MaterialTheme.typography.h4.copy(color = Color.Gray),
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = "$homeTracingCount",
                    style = if (isRefreshingHomeTracingCount) MaterialTheme.typography.h2.copy(color = Color.Gray.copy(alpha = 0.5F)) else MaterialTheme.typography.h2,
                  )
                }
              }
            }
          }

          item {
            Card(
              modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            ) {
              Box(
                modifier = Modifier.padding(8.dp),
              ) {
                Column {
                  Text(
                    text = stringResource(R.string.phone_tracing_counter_label),
                    style = MaterialTheme.typography.h4.copy(color = Color.Gray),
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = "$phoneTracingCount",
                    style = if (isRefreshingPhoneTracingCount) MaterialTheme.typography.h2.copy(color = Color.Gray.copy(alpha = 0.5F)) else MaterialTheme.typography.h2,
                  )
                }
              }
            }
          }

          item {
            Card(
              modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            ) {
              Box(
                modifier = Modifier.padding(8.dp),
              ) {
                Column {
                  Text(
                    text = stringResource(R.string.appointments_counter_label),
                    style = MaterialTheme.typography.h4.copy(color = Color.Gray),
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = "$appointmentsCount",
                    style = if (isRefreshingAppointmentsCount) MaterialTheme.typography.h2.copy(color = Color.Gray.copy(alpha = 0.5F)) else MaterialTheme.typography.h2,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
