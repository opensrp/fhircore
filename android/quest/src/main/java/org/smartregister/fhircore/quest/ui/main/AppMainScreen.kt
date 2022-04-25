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

@file:OptIn(ExperimentalMaterialApi::class)

package org.smartregister.fhircore.quest.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.domain.model.SideMenuOption
import org.smartregister.fhircore.engine.ui.userprofile.UserProfileScreen
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.navigation.NavigationScreen
import org.smartregister.fhircore.quest.ui.family.profile.FamilyProfileScreen
import org.smartregister.fhircore.quest.ui.main.components.AppDrawer
import org.smartregister.fhircore.quest.ui.patient.profile.PatientProfileScreen
import org.smartregister.fhircore.quest.ui.patient.register.PatientRegisterScreen

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
      // TODO Activate bottom nav via view configuration
      /* BottomScreenSection(
        navController = navController,
        navigationScreens = NavigationScreen.appScreens
      )*/
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
      val commonNavArgs =
        NavigationArg.commonNavArgs(
          firstSideMenuOption.appFeatureName,
          firstSideMenuOption.healthModule
        )

      when (it) {
        is NavigationScreen.Home -> {
          composable(
            route = "${it.route}${NavigationArg.HOME_ROUTE_PATH}",
            arguments =
              commonNavArgs.plus(
                navArgument(NavigationArg.SCREEN_TITLE) {
                  type = NavType.StringType
                  nullable = true
                  defaultValue = firstScreenTitle
                }
              )
          ) { stackEntry ->
            val appFeatureName = stackEntry.retrieveAppFeatureNameArg()
            val healthModule = stackEntry.retrieveHealthModuleArg()
            val screenTitle: String =
              stackEntry.arguments?.getString(NavigationArg.SCREEN_TITLE)
                ?: stringResource(R.string.all_clients)
            PatientRegisterScreen(
              navController = navController,
              openDrawer = openDrawer,
              appFeatureName = appFeatureName,
              healthModule = healthModule,
              screenTitle = screenTitle
            )
          }
        }
        NavigationScreen.Tasks -> composable(NavigationScreen.Tasks.route) {}
        NavigationScreen.Reports -> composable(NavigationScreen.Reports.route) {}
        NavigationScreen.Settings ->
          composable(NavigationScreen.Settings.route) { UserProfileScreen() }
        NavigationScreen.PatientProfile ->
          composable(
            route = "${it.route}${NavigationArg.PATIENT_ROUTE_PATH}",
            arguments = commonNavArgs.plus(patientIdNavArgument())
          ) { stackEntry ->
            val patientId = stackEntry.arguments?.getString(NavigationArg.PATIENT_ID)
            PatientProfileScreen(
              navController = navController,
              appFeatureName = stackEntry.retrieveAppFeatureNameArg(),
              healthModule = stackEntry.retrieveHealthModuleArg(),
              patientId = patientId
            )
          }
        NavigationScreen.FamilyProfile ->
          composable(
            route = "${it.route}${NavigationArg.PATIENT_ROUTE_PATH}",
            arguments = commonNavArgs.plus(patientIdNavArgument())
          ) { stackEntry ->
            val patientId = stackEntry.arguments?.getString(NavigationArg.PATIENT_ID)
            FamilyProfileScreen(patientId, navController)
          }
      }
    }
  }
}

private fun NavBackStackEntry.retrieveAppFeatureNameArg() =
  this.arguments?.getString(NavigationArg.FEATURE)

private fun NavBackStackEntry.retrieveHealthModuleArg(): HealthModule =
  (this.arguments?.get(NavigationArg.HEALTH_MODULE) ?: HealthModule.DEFAULT) as HealthModule

private fun patientIdNavArgument() =
  navArgument(NavigationArg.PATIENT_ID) {
    type = NavType.StringType
    nullable = true
    defaultValue = null
  }
