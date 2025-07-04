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

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.rememberFragmentState
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.geowidget.GeoWidgetConfiguration
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ToolBarHomeNavigation
import org.smartregister.fhircore.engine.ui.components.register.LoaderDialog
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.geowidget.model.GeoJsonFeature
import org.smartregister.fhircore.geowidget.screens.GeoWidgetFragment
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.event.ToolbarClickEvent
import org.smartregister.fhircore.quest.ui.bottomsheet.SummaryBottomSheetFragment
import org.smartregister.fhircore.quest.ui.main.AppMainEvent
import org.smartregister.fhircore.quest.ui.main.components.TopScreenSection
import org.smartregister.fhircore.quest.ui.shared.components.SyncBottomBar
import org.smartregister.fhircore.quest.ui.shared.models.AppDrawerUIState
import org.smartregister.fhircore.quest.ui.shared.models.SearchQuery
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent

@Composable
fun GeoWidgetLauncherScreen(
  modifier: Modifier = Modifier,
  openDrawer: (Boolean) -> Unit,
  navController: NavController,
  toolBarHomeNavigation: ToolBarHomeNavigation = ToolBarHomeNavigation.OPEN_DRAWER,
  geoWidgetConfiguration: GeoWidgetConfiguration,
  searchQuery: MutableState<SearchQuery>,
  search: (String) -> Unit,
  isFirstTimeSync: Boolean,
  appDrawerUIState: AppDrawerUIState,
  clearMapLiveData: MutableLiveData<Boolean>,
  geoJsonFeatures: MutableLiveData<List<GeoJsonFeature>>,
  launchQuestionnaire: (QuestionnaireConfig, GeoJsonFeature, Context) -> Unit,
  decodeImage: ((String) -> Bitmap?)?,
  onAppMainEvent: (AppMainEvent) -> Unit,
  isSyncing: LiveData<Boolean>,
) {
  val context = LocalContext.current
  val syncing by isSyncing.observeAsState()

  Scaffold(
    topBar = {
      Column {
        TopScreenSection(
          title = geoWidgetConfiguration.topScreenSection?.title ?: "",
          searchQuery = searchQuery.value,
          isSearchBarVisible = geoWidgetConfiguration.topScreenSection?.searchBar?.visible ?: true,
          searchPlaceholder = geoWidgetConfiguration.topScreenSection?.searchBar?.display,
          showSearchByQrCode =
            geoWidgetConfiguration.topScreenSection?.searchBar?.searchByQrCode ?: false,
          toolBarHomeNavigation = toolBarHomeNavigation,
          performSearchOnValueChanged = false,
          onSearchTextChanged = { searchedQuery: SearchQuery, performSearchOnValueChanged ->
            searchQuery.value = searchedQuery
            if (performSearchOnValueChanged) {
              val computedRules = geoWidgetConfiguration.topScreenSection?.searchBar?.computedRules
              if (!computedRules.isNullOrEmpty()) {
                search(searchQuery.value.query)
              } else {
                context.showToast(context.getString(R.string.no_search_coonfigs_provided))
              }
            }
          },
          isFilterIconEnabled = false,
          topScreenSection = geoWidgetConfiguration.topScreenSection,
          navController = navController,
          decodeImage = decodeImage,
        ) { event ->
          when (event) {
            ToolbarClickEvent.Navigate ->
              when (toolBarHomeNavigation) {
                ToolBarHomeNavigation.OPEN_DRAWER -> openDrawer(true)
                ToolBarHomeNavigation.NAVIGATE_BACK -> navController.popBackStack()
              }
            ToolbarClickEvent.FilterData -> {}
            is ToolbarClickEvent.Actions ->
              event.actions.handleClickEvent(navController = navController)
          }
        }
      }
    },
    bottomBar = {
      SyncBottomBar(
        totalSyncCount = appDrawerUIState.totalSyncCount,
        isFirstTimeSync = isFirstTimeSync,
        appDrawerUIState = appDrawerUIState,
        onAppMainEvent = onAppMainEvent,
        openDrawer = openDrawer,
      )
    },
  ) { innerPadding ->
    val fragmentState = rememberFragmentState()
    Box(
      modifier = modifier.padding(innerPadding).fillMaxSize(),
    ) {
      AndroidFragment<GeoWidgetFragment>(fragmentState = fragmentState) { fragment ->
        fragment
          .setUseGpsOnAddingLocation(false)
          .setAddLocationButtonVisibility(geoWidgetConfiguration.showAddLocation)
          .setOnAddLocationListener { feature: GeoJsonFeature ->
            if (feature.geometry?.coordinates == null) return@setOnAddLocationListener
            launchQuestionnaire(
              geoWidgetConfiguration.registrationQuestionnaire,
              feature,
              context,
            )
          }
          .setOnCancelAddingLocationListener {
            context.showToast(context.getString(R.string.on_cancel_adding_location))
          }
          .setOnClickLocationListener {
            feature: GeoJsonFeature,
            parentFragmentManager: FragmentManager,
            ->
            SummaryBottomSheetFragment(
                geoWidgetConfiguration.summaryBottomSheetConfig!!,
                ResourceData(
                  baseResourceId = feature.id,
                  baseResourceType = ResourceType.Location,
                  computedValuesMap = feature.properties?.mapValues { it.value.content } ?: emptyMap(),
                ),
              )
              .run { show(parentFragmentManager, SummaryBottomSheetFragment.TAG) }
          }
          .setMapLayers(geoWidgetConfiguration.mapLayers)
          .showCurrentLocationButtonVisibility(geoWidgetConfiguration.showLocation)
          .setPlaneSwitcherButtonVisibility(geoWidgetConfiguration.showPlaneSwitcher)

        fragment.apply {
          observerMapReset(clearMapLiveData)
          observerGeoJsonFeatures(geoJsonFeatures)
        }
      }
      if (syncing == true) {
        Box(
          modifier =
            Modifier.fillMaxSize().padding(16.dp).pointerInput(Unit) { detectTapGestures {} },
          contentAlignment = Alignment.Center,
        ) {
          LoaderDialog(
            boxWidth = 100.dp,
            boxHeight = 100.dp,
            progressBarSize = 130.dp,
            showBackground = true,
            showLineSpinIndicator = true,
            showOverlay = false,
            modifier = Modifier.align(Alignment.Center),
          )
        }
      }
    }
  }
}
