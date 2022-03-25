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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.domain.model.SideMenuOption
import org.smartregister.fhircore.engine.navigation.NavigationArg
import org.smartregister.fhircore.engine.navigation.NavigationScreen
import org.smartregister.fhircore.engine.ui.main.component.AppDrawer
import org.smartregister.fhircore.engine.ui.main.component.BottomScreenSection
import org.smartregister.fhircore.engine.ui.patient.register.PatientRegisterScreen
import org.smartregister.fhircore.engine.ui.userprofile.UserProfileScreen

@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  appMainViewModel: AppMainViewModel = hiltViewModel()
) {
  val navController = rememberNavController()
  val scope = rememberCoroutineScope()
  val scaffoldState = rememberScaffoldState()
  val uiState: AppMainUiState = appMainViewModel.appMainUiState
  val openDrawer: (Boolean) -> Unit = { open: Boolean ->
    scope.launch {
      if (open) scaffoldState.drawerState.open() else scaffoldState.drawerState.close()
    }
  }

  Scaffold(
    drawerGesturesEnabled = scaffoldState.drawerState.isOpen,
    scaffoldState = scaffoldState,
    drawerContent = {
      AppDrawer(
        appTitle = uiState.appTitle,
        username = uiState.username,
        lastSyncTime = uiState.lastSyncTime,
        currentLanguage = uiState.currentLanguage,
        languages = uiState.languages,
        openDrawer = openDrawer,
        sideMenuOptions = uiState.sideMenuOptions,
        onSideMenuClick = appMainViewModel::onEvent,
        navController = navController
      )
    },
    bottomBar = {
      BottomScreenSection(
        navController = navController,
        navigationScreens = NavigationScreen.appScreens
      )
    }
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      AppMainNavigationGraph(
        navController = navController,
        navigationScreens = NavigationScreen.appScreens,
        openDrawer = openDrawer,
        sideMenuOptions = uiState.sideMenuOptions
      )
    }
  }
}

@Composable
private fun AppMainNavigationGraph(
  navController: NavHostController,
  navigationScreens: List<NavigationScreen>,
  openDrawer: (Boolean) -> Unit,
  sideMenuOptions: List<SideMenuOption>,
) {

  val firstSideMenuOption = sideMenuOptions.first()
  val firstScreenTitle = stringResource(firstSideMenuOption.titleResource)

  NavHost(
    navController = navController,
    startDestination = NavigationScreen.Home.route + NavigationArg.HOME_ROUTE_PATH
  ) {
    navigationScreens.forEach {
      when (it) {
        is NavigationScreen.Home -> {
          composable(
            route = "${it.route}${NavigationArg.HOME_ROUTE_PATH}",
            arguments =
              listOf(
                navArgument(NavigationArg.FEATURE) {
                  type = NavType.StringType
                  nullable = true
                  defaultValue = firstSideMenuOption.appFeatureName
                },
                navArgument(NavigationArg.HEALTH_MODULE) {
                  type = NavType.StringType
                  nullable = true
                  defaultValue = firstSideMenuOption.healthModule?.name
                },
                navArgument(NavigationArg.SCREEN_TITLE) {
                  type = NavType.StringType
                  nullable = true
                  defaultValue = firstScreenTitle
                }
              )
          ) { backStackEntry ->
            val appFeatureName = backStackEntry.arguments?.getString(NavigationArg.FEATURE)
            val healthModule: String? =
              backStackEntry.arguments?.getString(NavigationArg.HEALTH_MODULE)
            val screenTitle: String =
              backStackEntry.arguments?.getString(NavigationArg.SCREEN_TITLE)
                ?: stringResource(R.string.all_clients)
            PatientRegisterScreen(
              openDrawer = openDrawer,
              appFeatureName = appFeatureName,
              healthModule = if (healthModule != null) HealthModule.valueOf(healthModule) else null,
              screenTitle = screenTitle
            )
          }
        }
        NavigationScreen.Tasks -> composable(NavigationScreen.Tasks.route) {}
        NavigationScreen.Reports -> composable(NavigationScreen.Reports.route) {}
        NavigationScreen.Settings ->
          composable(NavigationScreen.Settings.route) { UserProfileScreen() }
      }
    }
  }
}
