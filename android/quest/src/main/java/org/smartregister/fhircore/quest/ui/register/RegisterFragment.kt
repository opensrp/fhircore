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
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
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
import org.smartregister.fhircore.quest.ui.shared.components.SnackBarMessage
import org.smartregister.fhircore.quest.ui.shared.models.QuestionnaireSubmission
import org.smartregister.fhircore.quest.util.extensions.showSnackBar

@ExperimentalMaterialApi
@AndroidEntryPoint
class RegisterFragment : Fragment(), OnSyncListener, Observer<QuestionnaireSubmission?> {

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

    with(registerFragmentArgs) {
      lifecycleScope.launchWhenCreated {
        registerViewModel.retrieveRegisterUiState(registerId, screenTitle)
      }
    }
    return ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        val appConfig = appMainViewModel.applicationConfiguration
        val scope = rememberCoroutineScope()
        val scaffoldState = rememberScaffoldState()
        val uiState: AppMainUiState = appMainViewModel.appMainUiState.value
        val openDrawer: (Boolean) -> Unit = { open: Boolean ->
          scope.launch {
            if (open) scaffoldState.drawerState.open() else scaffoldState.drawerState.close()
          }
        }
        LaunchedEffect(Unit) {
          registerViewModel.snackBarStateFlow.showSnackBar(
            scaffoldState = scaffoldState,
            resourceData = null,
            navController = findNavController()
          )
        }

        AppTheme {
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
            },
            snackbarHost = { snackBarHostState ->
              SnackBarMessage(
                snackBarHostState = snackBarHostState,
                backgroundColorHex = appConfig.snackBarTheme.backgroundColor,
                actionColorHex = appConfig.snackBarTheme.actionTextColor,
                contentColorHex = appConfig.snackBarTheme.messageTextColor
              )
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
    registerViewModel.searchText.value = "" // Clear the search term
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

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    appMainViewModel.questionnaireSubmissionLiveData.observe(viewLifecycleOwner, this)
  }

  /**
   * Overridden method for [Observer] class used to address [QuestionnaireSubmission] triggered
   * while performing registration . A new [Observer] is needed for every fragment since the
   * [AppMainViewModel]'s questionnaireSubmissionLiveData outlives the Fragment. Cannot use Kotlin
   * Observer { } as it is optimized to a singleton resulting to an exception using an observer from
   * a detached fragment.
   */
  override fun onChanged(questionnaireSubmission: QuestionnaireSubmission?) {
    lifecycleScope.launch {
      questionnaireSubmission?.let {
        appMainViewModel.onQuestionnaireSubmit(questionnaireSubmission)

        // Always refresh data when registration happens
        with(registerFragmentArgs) {
          registerViewModel.retrieveRegisterUiState(registerId, screenTitle)
        }

        // Display SnackBar message
        val (questionnaireConfig, _) = questionnaireSubmission
        questionnaireConfig.snackBarMessage?.let { snackBarMessageConfig ->
          registerViewModel.emitSnackBarState(snackBarMessageConfig)
        }

        // Reset activity livedata
        appMainViewModel.questionnaireSubmissionLiveData.postValue(null)
      }
    }
  }
}
