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

package org.smartregister.fhircore.quest.ui.geowidget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.fhir.sync.CurrentSyncJobStatus
import com.google.android.fhir.sync.SyncJobStatus
import com.google.android.fhir.sync.SyncOperation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.geowidget.GeoWidgetConfiguration
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncListenerManager
import org.smartregister.fhircore.engine.sync.SyncState
import org.smartregister.fhircore.engine.ui.base.AlertDialogButton
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.AlertIntent
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.event.AppEvent
import org.smartregister.fhircore.quest.event.EventBus
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.ui.main.AppMainEvent
import org.smartregister.fhircore.quest.ui.main.AppMainUiState
import org.smartregister.fhircore.quest.ui.main.AppMainViewModel
import org.smartregister.fhircore.quest.ui.main.components.AppDrawer
import org.smartregister.fhircore.quest.ui.shared.components.SnackBarMessage
import org.smartregister.fhircore.quest.ui.shared.models.SearchMode
import org.smartregister.fhircore.quest.ui.shared.models.SearchQuery
import org.smartregister.fhircore.quest.ui.shared.viewmodels.SearchViewModel
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent
import org.smartregister.fhircore.quest.util.extensions.hookSnackBar
import org.smartregister.fhircore.quest.util.extensions.rememberLifecycleEvent
import timber.log.Timber

@AndroidEntryPoint
class GeoWidgetLauncherFragment : Fragment(), OnSyncListener {

  @Inject lateinit var eventBus: EventBus

  @Inject lateinit var syncListenerManager: SyncListenerManager

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  private lateinit var geoWidgetConfiguration: GeoWidgetConfiguration
  private val navArgs by navArgs<GeoWidgetLauncherFragmentArgs>()
  private val geoWidgetLauncherViewModel by viewModels<GeoWidgetLauncherViewModel>()
  private val appMainViewModel by activityViewModels<AppMainViewModel>()
  private val searchViewModel by activityViewModels<SearchViewModel>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    geoWidgetConfiguration =
      configurationRegistry.retrieveConfiguration<GeoWidgetConfiguration>(
        configType = ConfigType.GeoWidget,
        configId = navArgs.geoWidgetId,
      )
    if (geoWidgetConfiguration.resourceConfig.baseResource.resource != ResourceType.Location) {
      val message = getString(R.string.invalid_base_resource)
      requireContext().showToast(message)
      Timber.e(message, geoWidgetConfiguration.toString())
      requireContext().getActivity()?.finish()
    }

    return ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        val appConfig = appMainViewModel.applicationConfiguration
        val coroutineScope = rememberCoroutineScope()
        val scaffoldState = rememberScaffoldState()
        val uiState: AppMainUiState = appMainViewModel.appMainUiState.value
        val appDrawerUIState = appMainViewModel.appDrawerUiState.value
        val openDrawer: (Boolean) -> Unit = { open: Boolean ->
          coroutineScope.launch {
            if (open) scaffoldState.drawerState.open() else scaffoldState.drawerState.close()
          }
        }

        // Close side menu (drawer) when activity is not in foreground
        val lifecycleEvent = rememberLifecycleEvent()
        LaunchedEffect(lifecycleEvent) {
          if (lifecycleEvent == Lifecycle.Event.ON_PAUSE) scaffoldState.drawerState.close()
        }

        LaunchedEffect(Unit) {
          geoWidgetLauncherViewModel.snackBarStateFlow.hookSnackBar(
            scaffoldState = scaffoldState,
            resourceData = null,
            navController = findNavController(),
          )
        }

        AppTheme {
          Scaffold(
            drawerGesturesEnabled = scaffoldState.drawerState.isOpen,
            scaffoldState = scaffoldState,
            drawerContent = {
              AppDrawer(
                appUiState = uiState,
                appDrawerUIState = appDrawerUIState,
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
                decodeImage = { geoWidgetLauncherViewModel.getImageBitmap(it) },
              )
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
            Box(modifier = Modifier.padding(innerPadding)) {
              GeoWidgetLauncherScreen(
                modifier = Modifier.fillMaxSize(),
                openDrawer = openDrawer,
                navController = findNavController(),
                toolBarHomeNavigation = navArgs.toolBarHomeNavigation,
                geoWidgetConfiguration = geoWidgetConfiguration,
                searchQuery = searchViewModel.searchQuery,
                search = { searchText ->
                  geoWidgetLauncherViewModel.run {
                    onEvent(GeoWidgetEvent.ClearMap, context)
                    onEvent(
                      GeoWidgetEvent.RetrieveFeatures(
                        searchQuery = SearchQuery(searchText, SearchMode.KeyboardInput),
                        geoWidgetConfig = geoWidgetConfiguration,
                      ),
                      context,
                    )
                  }
                },
                isFirstTimeSync = geoWidgetLauncherViewModel.isFirstTime(),
                appDrawerUIState = appDrawerUIState,
                clearMapLiveData = geoWidgetLauncherViewModel.clearMapLiveData,
                geoJsonFeatures = geoWidgetLauncherViewModel.geoJsonFeatures,
                launchQuestionnaire = geoWidgetLauncherViewModel::launchQuestionnaire,
                decodeImage = geoWidgetLauncherViewModel::getImageBitmap,
                onAppMainEvent = appMainViewModel::onEvent,
                isSyncing = geoWidgetLauncherViewModel.isSyncing,
                fragmentActivityContext = this@GeoWidgetLauncherFragment.requireActivity(),
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

  override fun onSync(syncState: SyncState) {
    when (val syncJobStatus = syncState.currentSyncJobStatus) {
      is CurrentSyncJobStatus.Running -> {
        if (syncJobStatus.inProgressSyncJob is SyncJobStatus.InProgress) {
          val inProgressSyncJob = syncJobStatus.inProgressSyncJob as SyncJobStatus.InProgress
          val isSyncUpload = inProgressSyncJob.syncOperation == SyncOperation.UPLOAD
          val progressPercentage = appMainViewModel.calculatePercentageProgress(inProgressSyncJob)
          appMainViewModel.updateAppDrawerUIState(
            isSyncUpload = isSyncUpload,
            syncCounter = syncState.counter,
            currentSyncJobStatus = syncJobStatus,
            percentageProgress = progressPercentage,
          )
        }
      }
      is CurrentSyncJobStatus.Succeeded -> {
        appMainViewModel.updateAppDrawerUIState(
          syncCounter = syncState.counter,
          currentSyncJobStatus = syncJobStatus,
        )
      }
      is CurrentSyncJobStatus.Failed -> {
        appMainViewModel.updateAppDrawerUIState(
          syncCounter = syncState.counter,
          currentSyncJobStatus = syncJobStatus,
        )
        geoWidgetLauncherViewModel.onEvent(
          GeoWidgetEvent.RetrieveFeatures(
            geoWidgetConfig = geoWidgetConfiguration,
            searchQuery = searchViewModel.searchQuery.value,
          ),
          context = requireContext(),
        )
      }
      else ->
        appMainViewModel.updateAppDrawerUIState(
          syncCounter = syncState.counter,
          currentSyncJobStatus = syncJobStatus,
        )
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
        eventBus.events
          .getFor(MainNavigationScreen.GeoWidgetLauncher.eventId(navArgs.geoWidgetId))
          .onEach { appEvent ->
            when (appEvent) {
              is AppEvent.RefreshData,
              is AppEvent.OnSubmitQuestionnaire, -> {
                appMainViewModel.countRegisterData()
                geoWidgetLauncherViewModel.run {
                  onEvent(GeoWidgetEvent.ClearMap, context = requireContext())
                  onEvent(
                    GeoWidgetEvent.RetrieveFeatures(
                      geoWidgetConfig = geoWidgetConfiguration,
                      searchQuery = searchViewModel.searchQuery.value,
                    ),
                    context = requireContext(),
                  )
                }
              }
            }
          }
          .launchIn(this)
      }
    }
    geoWidgetLauncherViewModel.noLocationFoundDialog.observe(viewLifecycleOwner) { show ->
      if (show) {
        AlertDialogue.showAlert(
          context = requireContext(),
          alertIntent = AlertIntent.INFO,
          message = geoWidgetConfiguration.noResults?.message!!,
          title = geoWidgetConfiguration.noResults?.title!!,
          confirmButton =
            AlertDialogButton(
              listener = {
                geoWidgetConfiguration.noResults
                  ?.actionButton
                  ?.actions
                  ?.handleClickEvent(findNavController())
              },
              text = R.string.positive_button_location_set,
            ),
          cancellable = true,
          neutralButton =
            AlertDialogButton(
              listener = {},
            ),
        )
      }
    }
    geoWidgetLauncherViewModel.onEvent(
      GeoWidgetEvent.RetrieveFeatures(
        geoWidgetConfig = geoWidgetConfiguration,
        searchQuery = searchViewModel.searchQuery.value,
      ),
      context = requireContext(),
    )
  }

  override fun onPause() {
    super.onPause()
    appMainViewModel.updateAppDrawerUIState(
      isSyncUpload = false,
      syncCounter = null,
      currentSyncJobStatus = null,
      percentageProgress = 0,
    )
  }

  override fun onDestroy() {
    super.onDestroy()
    appMainViewModel.updateAppDrawerUIState(
      isSyncUpload = false,
      syncCounter = null,
      currentSyncJobStatus = null,
      percentageProgress = 0,
    )
  }
}
