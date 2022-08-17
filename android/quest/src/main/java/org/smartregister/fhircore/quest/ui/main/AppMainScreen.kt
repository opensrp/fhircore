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

package org.smartregister.fhircore.quest.ui.main

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
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
import org.smartregister.fhircore.engine.configuration.navigation.NavigationConfiguration
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.ui.userprofile.UserProfileScreen
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.navigation.NavigationArg.routePathsOf
import org.smartregister.fhircore.quest.ui.family.profile.FamilyProfileScreen
import org.smartregister.fhircore.quest.ui.main.components.AppDrawer
import org.smartregister.fhircore.quest.ui.profile.ProfileScreen
import org.smartregister.fhircore.quest.ui.register.RegisterScreen
import org.smartregister.fhircore.quest.ui.report.measure.MeasureReportViewModel
import org.smartregister.fhircore.quest.ui.report.measure.measureReportNavigationGraph

@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  appMainViewModel: AppMainViewModel = hiltViewModel()
) {
  val navController = rememberNavController()
  val scope = rememberCoroutineScope()
  val scaffoldState = rememberScaffoldState()
  val uiState: AppMainUiState = appMainViewModel.appMainUiState.value
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
        appUiState = uiState,
        openDrawer = openDrawer,
        onSideMenuClick = appMainViewModel::onEvent,
        navController = navController,
        getLocationPos = appMainViewModel.getLocationPos
      )
    },
    bottomBar = {
      // TODO Activate bottom nav via view configuration
      /* BottomScreenSection(
        navController = navController,
        mainNavigationScreens = MainNavigationScreen.appScreens
      )*/
    }
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      AppMainNavigationGraph(
        navController = navController,
        mainNavigationScreens = MainNavigationScreen.appScreens,
        openDrawer = openDrawer,
        appMainViewModel = appMainViewModel,
        navigationConfiguration = uiState.navigationConfiguration
      )
    }
  }
}

/**
 * The main screen composable function that includes the [NavHost] which provides a place for self
 * contained navigation. The screens are provided via the list of [MainNavigationScreen] and are
 * configured using the [NavigationConfiguration]. The main screen is composed of three parts
 * including: the side menu (navigation drawer), mid content section and the bottom navigation bar.
 * The [mainNavigationScreens] are rendered in the content section of the main screen depending on
 * the [MainNavigationScreen.route]. [MeasureReportViewModel] and [AppMainViewModel] are provided to
 * this composable function to handle any business logic. The [openDrawer] function is used to
 * toggle between opening and closing the navigation drawer.
 */
@Composable
private fun AppMainNavigationGraph(
  navController: NavHostController,
  mainNavigationScreens: List<MainNavigationScreen>,
  openDrawer: (Boolean) -> Unit,
  navigationConfiguration: NavigationConfiguration,
  measureReportViewModel: MeasureReportViewModel = hiltViewModel(),
  appMainViewModel: AppMainViewModel
) {
  val firstNavigationMenu = navigationConfiguration.clientRegisters.first()

  // The id of the register's click action, if it exists, otherwise the first navigation menu id
  val firstRegisterId =
    firstNavigationMenu.actions?.find { it.trigger == ActionTrigger.ON_CLICK }?.id
      ?: firstNavigationMenu.id

  val homeUrlParams = routePathsOf(NavigationArg.SCREEN_TITLE, NavigationArg.REGISTER_ID)
  NavHost(
    navController = navController,
    startDestination = MainNavigationScreen.Home.route + homeUrlParams
  ) {
    mainNavigationScreens.forEach {
      when (it) {
        is MainNavigationScreen.Home ->
          composable(
            route = it.route + homeUrlParams,
            arguments =
              listOf(
                navArgument(NavigationArg.SCREEN_TITLE) {
                  type = NavType.StringType
                  nullable = false
                  defaultValue = firstNavigationMenu.display
                },
                navArgument(NavigationArg.REGISTER_ID) {
                  type = NavType.StringType
                  nullable = false
                  defaultValue = firstRegisterId
                }
              )
          ) { stackEntry ->
            val screenTitle: String =
              stackEntry.arguments?.getString(NavigationArg.SCREEN_TITLE)
                ?: stringResource(R.string.all_clients)
            val registerId: String =
              stackEntry.arguments?.getString(NavigationArg.REGISTER_ID) ?: ""

            RegisterScreen(
              navController = navController,
              openDrawer = openDrawer,
              screenTitle = screenTitle,
              registerId = registerId,
              refreshDataState = appMainViewModel.refreshDataState
            )
          }
        MainNavigationScreen.Reports ->
          measureReportNavigationGraph(navController, measureReportViewModel)
        MainNavigationScreen.Settings ->
          composable(MainNavigationScreen.Settings.route) { UserProfileScreen() }
        MainNavigationScreen.Profile ->
          composable(
            route =
              "${it.route}${
            routePathsOf(
              NavigationArg.PATIENT_ID,
              NavigationArg.FAMILY_ID
            )
            }",
            arguments = patientIdNavArgument()
          ) { stackEntry ->
            val profileId = stackEntry.arguments?.getString(NavigationArg.PATIENT_ID) ?: ""
            val patientId = stackEntry.arguments?.getString(NavigationArg.PATIENT_ID) ?: ""
            val familyId = stackEntry.arguments?.getString(NavigationArg.FAMILY_ID)
            ProfileScreen(
              navController = navController,
              profileId = profileId,
              patientId = patientId,
              familyId = familyId,
              refreshDataState = appMainViewModel.refreshDataState
            )
          }
        MainNavigationScreen.FamilyProfile ->
          composable(
            route = "${it.route}${routePathsOf(NavigationArg.PATIENT_ID)}",
            arguments = patientIdNavArgument()
          ) { stackEntry ->
            val patientId = stackEntry.arguments?.getString(NavigationArg.PATIENT_ID)
            FamilyProfileScreen(
              familyId = patientId,
              navController = navController,
              refreshDataState = appMainViewModel.refreshDataState
            )
          }
      }
    }
  }
}

private fun patientIdNavArgument() =
  listOf(
    navArgument(NavigationArg.PATIENT_ID) {
      type = NavType.StringType
      nullable = true
      defaultValue = null
    },
    navArgument(NavigationArg.FAMILY_ID) {
      type = NavType.StringType
      nullable = true
      defaultValue = null
    }
  )
