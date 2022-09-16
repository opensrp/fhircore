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

package org.smartregister.fhircore.quest.ui.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.quest.ui.main.AppMainUiState
import org.smartregister.fhircore.quest.ui.main.AppMainViewModel
import org.smartregister.fhircore.quest.ui.main.components.AppDrawer

@ExperimentalMaterialApi
@AndroidEntryPoint
class RegisterFragment : Fragment() {

  val appMainViewModel by activityViewModels<AppMainViewModel>()

  val registerFragmentArgs by navArgs<RegisterFragmentArgs>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    appMainViewModel.retrieveRemoteIconsBitmap()
    return ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        val scope = rememberCoroutineScope()
        val scaffoldState = rememberScaffoldState()
        val uiState: AppMainUiState = appMainViewModel.appMainUiState.value
        val openDrawer: (Boolean) -> Unit = { open: Boolean ->
          scope.launch {
            if (open) scaffoldState.drawerState.open() else scaffoldState.drawerState.close()
          }
        }
        AppTheme {
          // Register screen provides access to the side navigation
          Scaffold(
            drawerGesturesEnabled = scaffoldState.drawerState.isOpen,
            scaffoldState = scaffoldState,
            drawerContent = {
              AppDrawer(
                appUiState = uiState,
                openDrawer = openDrawer,
                onSideMenuClick = appMainViewModel::onEvent,
                navController = findNavController()
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
            Box(modifier = Modifier.padding(innerPadding)) {
              RegisterScreen(
                navController = findNavController(),
                openDrawer = openDrawer,
                screenTitle = registerFragmentArgs.screenTitle,
                registerId = registerFragmentArgs.registerId,
                refreshDataState = appMainViewModel.refreshDataState
              )
            }
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    appMainViewModel.run {
      refreshDataState.value = true
      retrieveAppMainUiState()
    }
  }
}
