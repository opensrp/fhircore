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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.configuration.navigation.NavigationConfiguration
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.ui.userprofile.UserProfileScreen
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.navigation.NavigationArg.routePathsOf
import org.smartregister.fhircore.quest.ui.main.components.AppDrawer
import org.smartregister.fhircore.quest.ui.profile.ProfileScreen
import org.smartregister.fhircore.quest.ui.profile.ProfileViewModel
import org.smartregister.fhircore.quest.ui.register.RegisterScreen
import org.smartregister.fhircore.quest.ui.report.measure.MeasureReportViewModel
import org.smartregister.fhircore.quest.ui.report.measure.measureReportNavigationGraph

@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  appMainViewModel: AppMainViewModel = hiltViewModel(),
  navController: NavController
) {
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
        navController = navController
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
        openDrawer = openDrawer,
        appMainViewModel = appMainViewModel
      )
    }
  }
}

/**
 * The main screen composable function that includes the [NavHost] which provides a place for self
 * contained navigation. The screens are provided via the list of [MainNavigationScreen] and are
 * configured using the [NavigationConfiguration]. The main screen is composed of three parts
 * including: the side menu (navigation drawer), mid content section and the bottom navigation bar.
 * [MeasureReportViewModel] and [AppMainViewModel] are provided to this composable function to
 * handle any business logic. The [openDrawer] function is used to toggle between opening and
 * closing the navigation drawer.
 */
@Composable
private fun AppMainNavigationGraph(
  navController: NavController,
  openDrawer: (Boolean) -> Unit,
  measureReportViewModel: MeasureReportViewModel = hiltViewModel(),
  appMainViewModel: AppMainViewModel
) {
  val homeUrl =
    MainNavigationScreen.Home.route +
      routePathsOf(NavigationArg.SCREEN_TITLE, NavigationArg.REGISTER_ID)

  NavHost(navController = (navController as NavHostController), startDestination = homeUrl) {
    val topMenuConfig = appMainViewModel.navigationConfiguration.clientRegisters.first()
    val topMenuConfigId =
      topMenuConfig.actions?.find { it.trigger == ActionTrigger.ON_CLICK }?.id ?: topMenuConfig.id

    composable(
      route = homeUrl,
      arguments =
        listOf(
          navArgument(NavigationArg.SCREEN_TITLE) {
            type = NavType.StringType
            nullable = false
          },
          navArgument(NavigationArg.REGISTER_ID) {
            type = NavType.StringType
            nullable = false
          }
        )
    ) { stackEntry ->
      val screenTitle: String =
        stackEntry.arguments?.getString(NavigationArg.SCREEN_TITLE) ?: topMenuConfig.display
      val registerId: String =
        stackEntry.arguments?.getString(NavigationArg.REGISTER_ID) ?: topMenuConfigId

      RegisterScreen(
        navController = navController,
        openDrawer = openDrawer,
        screenTitle = screenTitle,
        registerId = registerId,
        refreshDataState = appMainViewModel.refreshDataState
      )
    }

    measureReportNavigationGraph(navController, measureReportViewModel)

    composable(MainNavigationScreen.Settings.route) { UserProfileScreen() }

    composable(
      route =
        MainNavigationScreen.Profile.route +
          routePathsOf(NavigationArg.PROFILE_ID, NavigationArg.RESOURCE_ID),
      arguments =
        listOf(
          navArgument(NavigationArg.PROFILE_ID) {
            type = NavType.StringType
            nullable = false
          },
          navArgument(NavigationArg.RESOURCE_ID) {
            type = NavType.StringType
            nullable = false
          }
        )
    ) { stackEntry ->
      val profileId = stackEntry.arguments?.getString(NavigationArg.PROFILE_ID)
      val resourceId = stackEntry.arguments?.getString(NavigationArg.RESOURCE_ID)

      if (!profileId.isNullOrEmpty() && !resourceId.isNullOrEmpty()) {
        val profileViewModel = hiltViewModel<ProfileViewModel>()
        LaunchedEffect(Unit) {
          profileViewModel.retrieveProfileUiState(profileId = profileId, resourceId = resourceId)
        }
        ProfileScreen(
          navController = navController,
          profileUiState = profileViewModel.profileUiState.value,
          onEvent = profileViewModel::onEvent
        )
      }
    }
  }
}
