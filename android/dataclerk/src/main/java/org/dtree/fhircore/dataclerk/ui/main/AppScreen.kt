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

package org.dtree.fhircore.dataclerk.ui.main

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.dtree.fhircore.dataclerk.ui.home.HomeScreen
import org.dtree.fhircore.dataclerk.ui.home.HomeViewModel
import org.dtree.fhircore.dataclerk.ui.patient.PatientScreen

@Composable
fun AppScreen(
  appMainViewModel: AppMainViewModel,
  homeViewModel: HomeViewModel = hiltViewModel(),
  sync: () -> Unit
) {
  val navController = rememberNavController()
  NavHost(navController = navController, startDestination = "home") {
    composable("home") {
      HomeScreen(appMainViewModel = appMainViewModel, homeViewModel = homeViewModel, sync = sync) {
        navController.navigate("patient/${it.resourceId}")
      }
    }
    composable(
      "patient/{patientId}",
      arguments = listOf(navArgument("patientId") { type = NavType.StringType })
    ) { PatientScreen(navController, appMainViewModel = appMainViewModel) }
  }
}
