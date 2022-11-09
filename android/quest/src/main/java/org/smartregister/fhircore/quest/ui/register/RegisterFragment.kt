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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.android.fhir.sync.State
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncListenerManager
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.quest.ui.main.AppMainUiState
import org.smartregister.fhircore.quest.ui.main.AppMainViewModel
import org.smartregister.fhircore.quest.ui.main.components.AppDrawer
import org.smartregister.fhircore.quest.util.extensions.rememberLifecycleEvent

@ExperimentalMaterialApi
@AndroidEntryPoint
class RegisterFragment : Fragment(), OnSyncListener {

  @Inject lateinit var syncListenerManager: SyncListenerManager

  val appMainViewModel by activityViewModels<AppMainViewModel>()

  val registerFragmentArgs by navArgs<RegisterFragmentArgs>()

  val registerViewModel by viewModels<RegisterViewModel>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    appMainViewModel.retrieveIconsAsBitmap()
    syncListenerManager.registerSyncListener(this, lifecycle)
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
          // Retrieve data when Lifecycle state is resuming
          val lifecycleEvent = rememberLifecycleEvent()
          LaunchedEffect(lifecycleEvent) {
            if (lifecycleEvent == Lifecycle.Event.ON_RESUME) {
              appMainViewModel.retrieveAppMainUiState()
              with(registerFragmentArgs) {
                registerViewModel.retrieveRegisterUiState(registerId, screenTitle)
              }
            }
          }

          val pagingItems =
            registerViewModel
              .paginatedRegisterData
              .collectAsState(emptyFlow())
              .value
              .collectAsLazyPagingItems()

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
                searchText = registerViewModel.searchText,
                currentPage = registerViewModel.currentPage,
                onEvent = registerViewModel::onEvent,
                pagingItems = pagingItems,
                registerUiState = registerViewModel.registerUiState.value
              )
            }
          }
        }
      }
    }
  }

  override fun onStop() {
    super.onStop()
    // Clear the search term
    registerViewModel.searchText.value = ""
  }

  override fun onSync(state: State) {
    if (state is State.Finished || state is State.Failed) {
      with(registerFragmentArgs) {
        registerViewModel.run {
          // Clear pages cache to load new data
          pagesDataCache.clear()
          retrieveRegisterUiState(registerId, screenTitle)
        }
      }
    }
  }
}
