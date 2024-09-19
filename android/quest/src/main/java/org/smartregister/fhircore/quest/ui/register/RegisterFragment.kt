/*
 * Copyright 2021-2024 Ona Systems, Inc
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
import androidx.compose.ui.platform.testTag
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.android.fhir.sync.CurrentSyncJobStatus
import com.google.android.fhir.sync.SyncJobStatus
import com.google.android.fhir.sync.SyncOperation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncListenerManager
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.quest.event.AppEvent
import org.smartregister.fhircore.quest.event.EventBus
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.ui.main.AppMainEvent
import org.smartregister.fhircore.quest.ui.main.AppMainUiState
import org.smartregister.fhircore.quest.ui.main.AppMainViewModel
import org.smartregister.fhircore.quest.ui.main.components.AppDrawer
import org.smartregister.fhircore.quest.ui.shared.components.SnackBarMessage
import org.smartregister.fhircore.quest.ui.shared.models.QuestionnaireSubmission
import org.smartregister.fhircore.quest.ui.shared.models.SearchQuery
import org.smartregister.fhircore.quest.ui.shared.viewmodels.SearchViewModel
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent
import org.smartregister.fhircore.quest.util.extensions.hookSnackBar
import org.smartregister.fhircore.quest.util.extensions.rememberLifecycleEvent

@ExperimentalMaterialApi
@AndroidEntryPoint
class RegisterFragment : Fragment(), OnSyncListener {

  @Inject lateinit var syncListenerManager: SyncListenerManager

  @Inject lateinit var eventBus: EventBus
  private val registerFragmentArgs by navArgs<RegisterFragmentArgs>()
  private val registerViewModel by viewModels<RegisterViewModel>()
  private val appMainViewModel by activityViewModels<AppMainViewModel>()
  private val searchViewModel by activityViewModels<SearchViewModel>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    appMainViewModel.retrieveIconsAsBitmap()

    with(registerFragmentArgs) {
      lifecycleScope.launchWhenCreated {
        registerViewModel.retrieveRegisterUiState(
          registerId = registerId,
          screenTitle = screenTitle,
          params = params,
          clearCache = false,
        )
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
            navController = findNavController(),
          )
        }

        AppTheme {
          val pagingItems =
            registerViewModel.registerData
              .collectAsState(emptyFlow())
              .value
              .collectAsLazyPagingItems()

          Scaffold(
            drawerGesturesEnabled = scaffoldState.drawerState.isOpen,
            scaffoldState = scaffoldState,
            drawerContent = {
              AppDrawer(
                appUiState = uiState,
                appDrawerUIState = appMainViewModel.appDrawerUiState.value,
                openDrawer = openDrawer,
                onSideMenuClick = {
                  if (it is AppMainEvent.TriggerWorkflow) {
                    searchViewModel.searchQuery.value = SearchQuery.emptyText
                  }
                  appMainViewModel.onEvent(it)
                },
                navController = findNavController(),
                unSyncedResourceCount = appMainViewModel.unSyncedResourcesCount,
                onCountUnSyncedResources = appMainViewModel::updateUnSyncedResourcesCount,
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
                contentColorHex = appConfig.snackBarTheme.messageTextColor,
              )
            },
          ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).testTag(REGISTER_SCREEN_BOX_TAG)) {
              RegisterScreen(
                openDrawer = openDrawer,
                onEvent = registerViewModel::onEvent,
                registerUiState = registerViewModel.registerUiState.value,
                appDrawerUIState = appMainViewModel.appDrawerUiState.value,
                onAppMainEvent = { appMainViewModel.onEvent(it) },
                searchQuery = searchViewModel.searchQuery,
                currentPage = registerViewModel.currentPage,
                pagingItems = pagingItems,
                navController = findNavController(),
                toolBarHomeNavigation = registerFragmentArgs.toolBarHomeNavigation,
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

  override fun onSync(syncJobStatus: CurrentSyncJobStatus) {
    when (syncJobStatus) {
      is CurrentSyncJobStatus.Running -> {
        if (syncJobStatus.inProgressSyncJob is SyncJobStatus.InProgress) {
          val inProgressSyncJob = syncJobStatus.inProgressSyncJob as SyncJobStatus.InProgress
          val isSyncUpload = inProgressSyncJob.syncOperation == SyncOperation.UPLOAD
          val progressPercentage = appMainViewModel.calculatePercentageProgress(inProgressSyncJob)
          lifecycleScope.launch {
            appMainViewModel.updateAppDrawerUIState(
              isSyncUpload = isSyncUpload,
              currentSyncJobStatus = syncJobStatus,
              percentageProgress = progressPercentage,
            )
          }
        }
      }
      is CurrentSyncJobStatus.Succeeded -> {
        refreshRegisterData()
        appMainViewModel.updateAppDrawerUIState(currentSyncJobStatus = syncJobStatus)
      }
      is CurrentSyncJobStatus.Failed -> {
        refreshRegisterData()
        appMainViewModel.updateAppDrawerUIState(currentSyncJobStatus = syncJobStatus)
      }
      else -> appMainViewModel.updateAppDrawerUIState(currentSyncJobStatus = syncJobStatus)
    }
  }

  fun refreshRegisterData(questionnaireResponse: QuestionnaireResponse? = null) {
    with(registerFragmentArgs) {
      registerViewModel.run {
        if (questionnaireResponse != null) {
          updateRegisterFilterState(registerId, questionnaireResponse)
        }

        retrieveRegisterUiState(
          registerId = registerId,
          screenTitle = screenTitle,
          params = params,
          clearCache = true,
        )
      }
    }

    appMainViewModel.updateUnSyncedResourcesCount()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
        // Each register should have unique eventId
        eventBus.events
          .getFor(MainNavigationScreen.Home.eventId(registerFragmentArgs.registerId))
          .onEach { appEvent ->
            when (appEvent) {
              is AppEvent.OnSubmitQuestionnaire ->
                handleQuestionnaireSubmission(appEvent.questionnaireSubmission)
              is AppEvent.RefreshRegisterData -> {
                appMainViewModel.countRegisterData()
                refreshRegisterData()
              }
            }
          }
          .launchIn(lifecycleScope)
      }
    }

    appMainViewModel.resetRegisterFilters.observe(viewLifecycleOwner) { resetFilters ->
      if (resetFilters) {
        registerViewModel.registerFilterState.value = RegisterFilterState()
        refreshRegisterData()
        appMainViewModel.resetRegisterFilters.value = false
      }
    }
  }

  override fun onPause() {
    super.onPause()
    appMainViewModel.updateAppDrawerUIState(false, null, 0)
  }

  override fun onDestroy() {
    super.onDestroy()
    appMainViewModel.updateAppDrawerUIState(false, null, 0)
  }

  suspend fun handleQuestionnaireSubmission(questionnaireSubmission: QuestionnaireSubmission) {
    if (questionnaireSubmission.questionnaireConfig.saveQuestionnaireResponse) {
      appMainViewModel.run {
        onQuestionnaireSubmission(questionnaireSubmission)
        retrieveAppMainUiState(refreshAll = false) // Update register counts
      }

      val (questionnaireConfig, _) = questionnaireSubmission

      refreshRegisterData()

      questionnaireConfig.snackBarMessage?.let { snackBarMessageConfig ->
        registerViewModel.emitSnackBarState(snackBarMessageConfig)
      }

      questionnaireConfig.onSubmitActions?.handleClickEvent(navController = findNavController())
    } else {
      refreshRegisterData(questionnaireSubmission.questionnaireResponse)
    }
  }

  companion object {
    const val REGISTER_SCREEN_BOX_TAG = "fragmentRegisterScreenTestTag"
  }
}
