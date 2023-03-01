/*
 * Copyright 2021-2023 Ona Systems, Inc
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
import androidx.compose.material.SnackbarDuration
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
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.android.fhir.sync.SyncJobStatus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncListenerManager
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.main.AppMainUiState
import org.smartregister.fhircore.quest.ui.main.AppMainViewModel
import org.smartregister.fhircore.quest.ui.main.components.AppDrawer
import org.smartregister.fhircore.quest.ui.shared.components.SnackBarMessage
import org.smartregister.fhircore.quest.ui.shared.models.QuestionnaireSubmission
import org.smartregister.fhircore.quest.util.extensions.hookSnackBar
import org.smartregister.fhircore.quest.util.extensions.rememberLifecycleEvent
import retrofit2.HttpException
import timber.log.Timber

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

        // Close side menu (drawer) when activity is not in foreground
        val lifecycleEvent = rememberLifecycleEvent()
        LaunchedEffect(lifecycleEvent) {
          if (lifecycleEvent == Lifecycle.Event.ON_PAUSE) scaffoldState.drawerState.close()
        }

        LaunchedEffect(Unit) {
          registerViewModel.snackBarStateFlow.hookSnackBar(
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
                registerUiState = registerViewModel.registerUiState.value,
                toolBarHomeNavigation = registerFragmentArgs.toolBarHomeNavigation
              )
            }
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    syncListenerManager.registerSyncListener(this, lifecycle)
  }

  override fun onStop() {
    super.onStop()
    registerViewModel.searchText.value = "" // Clear the search term
  }

  override fun onSync(syncJobStatus: SyncJobStatus) {
    when (syncJobStatus) {
      is SyncJobStatus.Started ->
        lifecycleScope.launch {
          registerViewModel.emitSnackBarState(
            SnackBarMessageConfig(message = getString(R.string.syncing))
          )
        }
      is SyncJobStatus.Finished -> {
        refreshRegisterData()
        lifecycleScope.launch {
          registerViewModel.emitSnackBarState(
            SnackBarMessageConfig(
              message = getString(R.string.sync_completed),
              actionLabel = getString(R.string.ok).uppercase(),
              duration = SnackbarDuration.Long
            )
          )
        }
      }
      is SyncJobStatus.Failed -> {
        refreshRegisterData()
        // Show error message in snackBar message
        // syncJobStatus.exceptions may be null when worker fails; hence the null safety usage
        val hasAuthError =
          syncJobStatus.exceptions.any {
            it.exception is HttpException && (it.exception as HttpException).code() == 401
          }
        Timber.e(syncJobStatus.exceptions?.joinToString { it.exception.message.toString() })
        val messageResourceId =
          if (hasAuthError) R.string.sync_unauthorised else R.string.sync_failed
        lifecycleScope.launch {
          registerViewModel.emitSnackBarState(
            SnackBarMessageConfig(
              message = getString(messageResourceId),
              duration = SnackbarDuration.Long
            )
          )
        }
      }
      else -> {
        /* Do nothing*/
      }
    }
  }

  private fun refreshRegisterData() {
    with(registerFragmentArgs) {
      registerViewModel.run {
        // Clear pages cache to load new data
        pagesDataCache.clear()
        retrieveRegisterUiState(registerId, screenTitle)
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
        appMainViewModel.onQuestionnaireSubmission(questionnaireSubmission)

        // Always refresh data when registration happens
        registerViewModel.paginateRegisterData(
          registerId = registerFragmentArgs.registerId,
          loadAll = false,
          clearCache = true
        )
        appMainViewModel.retrieveAppMainUiState()

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
