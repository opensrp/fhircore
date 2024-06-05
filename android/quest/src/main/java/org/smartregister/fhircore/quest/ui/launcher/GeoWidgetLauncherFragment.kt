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

package org.smartregister.fhircore.quest.ui.launcher

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.fhir.datacapture.extensions.tryUnwrapContext
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.geowidget.GeoWidgetConfiguration
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.geowidget.model.Feature
import org.smartregister.fhircore.geowidget.screens.GeoWidgetFragment
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.event.AppEvent
import org.smartregister.fhircore.quest.event.EventBus
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.ui.bottomsheet.SummaryBottomSheetFragment
import org.smartregister.fhircore.quest.ui.main.AppMainUiState
import org.smartregister.fhircore.quest.ui.main.AppMainViewModel
import org.smartregister.fhircore.quest.ui.main.components.AppDrawer
import org.smartregister.fhircore.quest.ui.shared.components.SnackBarMessage
import org.smartregister.fhircore.quest.util.extensions.hookSnackBar
import org.smartregister.fhircore.quest.util.extensions.rememberLifecycleEvent
import timber.log.Timber

@AndroidEntryPoint
class GeoWidgetLauncherFragment : Fragment() {
  @Inject lateinit var eventBus: EventBus

  @Inject lateinit var configurationRegistry: ConfigurationRegistry
  private lateinit var geoWidgetFragment: GeoWidgetFragment
  private val geoWidgetLauncherViewModel by viewModels<GeoWidgetLauncherViewModel>()
  private val args by navArgs<GeoWidgetLauncherFragmentArgs>()
  private val geoWidgetConfiguration: GeoWidgetConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(
      ConfigType.GeoWidget,
      args.geoWidgetId,
      emptyMap(),
    )
  }
  private val appMainViewModel by activityViewModels<AppMainViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Timber.i("GeoWidgetLauncherFragment onCreate")
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    Timber.i("GeoWidgetLauncherFragment onCreateView")
    buildGeoWidgetFragment()

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
          geoWidgetLauncherViewModel.snackBarStateFlow.hookSnackBar(
            scaffoldState = scaffoldState,
            resourceData = null,
            navController = findNavController(),
          )
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
                navController = findNavController(),
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
              val fragment = remember { geoWidgetFragment }

              GeoWidgetLauncherScreen(
                openDrawer = openDrawer,
                onEvent = geoWidgetLauncherViewModel::onEvent,
                navController = findNavController(),
                toolBarHomeNavigation = args.toolBarHomeNavigation,
                modifier = Modifier.fillMaxSize(), // Adjust the modifier as needed
                fragmentManager = childFragmentManager,
                fragment = fragment,
                geoWidgetConfiguration = geoWidgetConfiguration,
                searchText = geoWidgetLauncherViewModel.searchText,
                filterLocations = {geoWidgetFragment.onSearchMap(it) }
              )
            }
          }
        }
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    showSetLocationDialog()
    setOnQuestionnaireSubmissionListener()
    setLocationFromDbCollector()
    geoWidgetLauncherViewModel.checkSelectedLocation(geoWidgetConfiguration)
    Timber.i("GeoWidgetLauncherFragment onViewCreated")
  }

  private fun buildGeoWidgetFragment() {
    geoWidgetFragment =
      GeoWidgetFragment.builder()
        .setUseGpsOnAddingLocation(false)
        .setAddLocationButtonVisibility(geoWidgetConfiguration.showAddLocation)
        .setOnAddLocationListener { feature: Feature ->
          if (feature.geometry?.coordinates == null) return@setOnAddLocationListener
          geoWidgetLauncherViewModel.launchQuestionnaire(
            geoWidgetConfiguration.registrationQuestionnaire,
            feature,
            activity?.tryUnwrapContext() as Context,
          )
        }
        .setOnCancelAddingLocationListener {
          requireContext().showToast("on cancel adding location")
        }
        .setOnClickLocationListener { feature: Feature, parentFragmentManager: FragmentManager ->
          SummaryBottomSheetFragment(
              geoWidgetConfiguration.summaryBottomSheetConfig!!,
              ResourceData(feature.id, ResourceType.Location, feature.properties),
            )
            .run { show(parentFragmentManager, SummaryBottomSheetFragment.TAG) }
        }
        .setMapLayers(geoWidgetConfiguration.mapLayers)
        .showCurrentLocationButtonVisibility(geoWidgetConfiguration.showLocation)
        .setPlaneSwitcherButtonVisibility(geoWidgetConfiguration.showPlaneSwitcher)
        .build()
  }

  private fun setOnQuestionnaireSubmissionListener() {
    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        eventBus.events
          .getFor(MainNavigationScreen.GeoWidgetLauncher.eventId(geoWidgetConfiguration.id))
          .onEach { appEvent ->
            if (appEvent is AppEvent.OnSubmitQuestionnaire) {
              val extractedResourceIds = appEvent.questionnaireSubmission.extractedResourceIds
              geoWidgetLauncherViewModel.onQuestionnaireSubmission(
                extractedResourceIds,
              )
            }
          }
          .launchIn(lifecycleScope)
      }
    }
  }

  private fun setLocationFromDbCollector() {
    viewLifecycleOwner.lifecycleScope.launch {
      delay(1000)
      geoWidgetLauncherViewModel.locationsFlow
        .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
        .collect { locations -> geoWidgetFragment.addLocationsToMap(locations) }
    }
  }

  private fun showSetLocationDialog() {
    viewLifecycleOwner.lifecycleScope.launch {
      geoWidgetLauncherViewModel.locationDialog.observe(requireActivity()) {
        AlertDialogue.showConfirmAlert(
          context = requireContext(),
          message = R.string.message_location_set,
          title = R.string.title_no_location_set,
          confirmButtonListener = {},
          confirmButtonText = R.string.positive_button_location_set,
        )
      }
    }
  }

  companion object {
    const val GEO_WIDGET_FRAGMENT_TAG = "geo-widget-fragment-tag"
  }

  override fun onStop() {
    super.onStop()
    geoWidgetLauncherViewModel.searchText.value = "" // Clear the search term
  }
}
