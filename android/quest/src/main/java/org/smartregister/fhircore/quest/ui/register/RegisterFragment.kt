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
import androidx.compose.material.SnackbarDuration
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
import com.google.android.fhir.sync.SyncJobStatus
import com.google.android.fhir.sync.SyncOperation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncListenerManager
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.event.AppEvent
import org.smartregister.fhircore.quest.event.EventBus
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.ui.main.AppMainUiState
import org.smartregister.fhircore.quest.ui.main.AppMainViewModel
import org.smartregister.fhircore.quest.ui.main.components.AppDrawer
import org.smartregister.fhircore.quest.ui.shared.components.SnackBarMessage
import org.smartregister.fhircore.quest.ui.shared.models.QuestionnaireSubmission
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent
import org.smartregister.fhircore.quest.util.extensions.hookSnackBar
import org.smartregister.fhircore.quest.util.extensions.rememberLifecycleEvent
import retrofit2.HttpException
import timber.log.Timber

@ExperimentalMaterialApi
@AndroidEntryPoint
class RegisterFragment : Fragment(), OnSyncListener {

  @Inject lateinit var syncListenerManager: SyncListenerManager

  @Inject lateinit var eventBus: EventBus
  private val appMainViewModel by activityViewModels<AppMainViewModel>()
  private val registerFragmentArgs by navArgs<RegisterFragmentArgs>()
  private val registerViewModel by viewModels<RegisterViewModel>()

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
            registerViewModel.paginatedRegisterData
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
                navController = findNavController(),
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
                searchText = registerViewModel.searchText,
                currentPage = registerViewModel.currentPage,
                pagingItems = pagingItems,
                navController = findNavController(),
                toolBarHomeNavigation = registerFragmentArgs.toolBarHomeNavigation,
                registerViewModel = registerViewModel,
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
            SnackBarMessageConfig(message = getString(R.string.syncing)),
          )
        }
      is SyncJobStatus.InProgress ->
        emitPercentageProgress(syncJobStatus, syncJobStatus.syncOperation == SyncOperation.UPLOAD)
      is SyncJobStatus.Succeeded -> {
        refreshRegisterData()
        lifecycleScope.launch {
          registerViewModel.emitSnackBarState(
            SnackBarMessageConfig(
              message = getString(R.string.sync_completed),
              actionLabel = getString(R.string.ok).uppercase(),
              duration = SnackbarDuration.Long,
            ),
          )
        }
      }
      is SyncJobStatus.Failed -> {
        refreshRegisterData()
        // Show error message in snackBar message
        val hasAuthError =
          try {
            Timber.e(syncJobStatus.exceptions.joinToString { it.exception.message ?: "" })
            syncJobStatus.exceptions.any {
              it.exception is HttpException && (it.exception as HttpException).code() == 401
            }
          } catch (nullPointerException: NullPointerException) {
            false
          }

        lifecycleScope.launch {
          registerViewModel.emitSnackBarState(
            SnackBarMessageConfig(
              message =
                getString(
                  if (hasAuthError) {
                    R.string.sync_unauthorised
                  } else R.string.sync_completed_with_errors,
                ),
              duration = SnackbarDuration.Long,
              actionLabel = getString(R.string.ok).uppercase(),
            ),
          )
        }
      }
      else -> {
        // Do nothing
      }
    }
  }

  fun refreshRegisterData(questionnaireResponse: QuestionnaireResponse? = null) {
    with(registerFragmentArgs) {
      registerViewModel.run {
        if (questionnaireResponse != null) {
          updateRegisterFilterState(registerId, questionnaireResponse)
        }

        pagesDataCache.clear()

        retrieveRegisterUiState(
          registerId = registerId,
          screenTitle = screenTitle,
          params = params,
          clearCache = false,
        )
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
        // Each register should have unique eventId
        eventBus.events
          .getFor(MainNavigationScreen.Home.eventId(registerFragmentArgs.registerId))
          .onEach { appEvent ->
            if (appEvent is AppEvent.OnSubmitQuestionnaire) {
              handleQuestionnaireSubmission(appEvent.questionnaireSubmission)
            } else if (appEvent is AppEvent.OnMigrateData) {
              registerViewModel.setOnMigrateDataInProgress(appEvent.inProgress)
            }
          }
          .launchIn(lifecycleScope)
      }
    }
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

  fun emitPercentageProgress(
    progressSyncJobStatus: SyncJobStatus.InProgress,
    isUploadSync: Boolean,
  ) {
    lifecycleScope.launch {
      val percentageProgress: Int = calculateActualPercentageProgress(progressSyncJobStatus)
      registerViewModel.emitPercentageProgressState(percentageProgress, isUploadSync)
    }
  }

  private fun getSyncProgress(completed: Int, total: Int) =
    completed * 100 / if (total > 0) total else 1

  private fun calculateActualPercentageProgress(
    progressSyncJobStatus: SyncJobStatus.InProgress,
  ): Int {
    val totalRecordsOverall =
      registerViewModel.sharedPreferencesHelper.read(
        SharedPreferencesHelper.PREFS_SYNC_PROGRESS_TOTAL +
          progressSyncJobStatus.syncOperation.name,
        1L,
      )
    val isProgressTotalLess = progressSyncJobStatus.total <= totalRecordsOverall
    val currentProgress: Int
    val currentTotalRecords =
      if (isProgressTotalLess) {
        currentProgress =
          totalRecordsOverall.toInt() - progressSyncJobStatus.total +
            progressSyncJobStatus.completed
        totalRecordsOverall.toInt()
      } else {
        registerViewModel.sharedPreferencesHelper.write(
          SharedPreferencesHelper.PREFS_SYNC_PROGRESS_TOTAL +
            progressSyncJobStatus.syncOperation.name,
          progressSyncJobStatus.total.toLong(),
        )
        currentProgress = progressSyncJobStatus.completed
        progressSyncJobStatus.total
      }

    return getSyncProgress(currentProgress, currentTotalRecords)
  }

  companion object {
    const val REGISTER_SCREEN_BOX_TAG = "fragmentRegisterScreenTestTag"
  }
}
