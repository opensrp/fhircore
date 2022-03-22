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

package org.smartregister.fhircore.engine.ui.main

import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.navigation.NavigationScreen
import org.smartregister.fhircore.engine.ui.main.component.AppDrawer
import org.smartregister.fhircore.engine.ui.main.component.BottomScreenSection
import org.smartregister.fhircore.engine.ui.patient.register.PatientRegisterScreen
import org.smartregister.fhircore.engine.ui.userprofile.UserProfileScreen

@Composable
fun MainScreen(appMainViewModel: AppMainViewModel = hiltViewModel()) {
  val navController = rememberNavController()
  val scope = rememberCoroutineScope()
  val scaffoldState = rememberScaffoldState()
  val openDrawer = { scope.launch { scaffoldState.drawerState.open() } }

  Scaffold(
    drawerGesturesEnabled = scaffoldState.drawerState.isOpen,
    scaffoldState = scaffoldState,
    drawerContent = {
      AppDrawer(
        appTitle = "MOH VTS",
        username = "Demo",
        lastSynTime = "05:30 PM, Mar 3",
        sideMenuOptions = appMainViewModel.retrieveSideMenuOptions(),
        onSideMenuClick = appMainViewModel::onSideMenuEvent
      )
    },
    bottomBar = {
      BottomScreenSection(
        navController = navController,
        navigationScreens = NavigationScreen.appScreens
      )
    }
  ) {
    AppMainNavigationGraph(
      navController = navController,
      navigationScreens = NavigationScreen.appScreens,
      openDrawer = openDrawer,
    )
  }
}

@Composable
private fun AppMainNavigationGraph(
  navController: NavHostController,
  navigationScreens: List<NavigationScreen>,
  openDrawer: () -> Job,
) {
  NavHost(navController = navController, startDestination = NavigationScreen.Home.route) {
    navigationScreens.forEach {
      when (it) {
        is NavigationScreen.Home ->
          composable(it.route) {
            PatientRegisterScreen(
              openDrawer = openDrawer
            )
          }
        NavigationScreen.Tasks -> composable(NavigationScreen.Tasks.route) {}
        NavigationScreen.Reports -> composable(NavigationScreen.Reports.route) {}
        NavigationScreen.Settings ->
          composable(NavigationScreen.Settings.route) { UserProfileScreen() }
      }
    }
  }
}
