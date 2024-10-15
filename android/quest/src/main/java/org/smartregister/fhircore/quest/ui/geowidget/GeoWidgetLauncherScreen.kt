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

import android.graphics.Bitmap
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import org.smartregister.fhircore.engine.configuration.geowidget.GeoWidgetConfiguration
import org.smartregister.fhircore.engine.domain.model.ToolBarHomeNavigation
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.geowidget.screens.GeoWidgetFragment
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.event.ToolbarClickEvent
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
  fragmentManager: FragmentManager,
  geoWidgetFragment: GeoWidgetFragment,
  geoWidgetConfiguration: GeoWidgetConfiguration,
  searchQuery: MutableState<SearchQuery>,
  search: (String) -> Unit,
  isFirstTimeSync: Boolean,
  appDrawerUIState: AppDrawerUIState,
  decodeImage: ((String) -> Bitmap?)?,
  onAppMainEvent: (AppMainEvent) -> Unit,
) {
  val context = LocalContext.current
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
        isFirstTimeSync = isFirstTimeSync,
        appDrawerUIState = appDrawerUIState,
        onAppMainEvent = onAppMainEvent,
        openDrawer = openDrawer,
      )
    },
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      GeoWidgetFragmentView(
        modifier = modifier,
        fragmentManager = fragmentManager,
        fragment = geoWidgetFragment,
      )
    }
  }
}

@Composable
fun GeoWidgetFragmentView(
  modifier: Modifier = Modifier,
  fragmentManager: FragmentManager,
  fragment: Fragment,
) {
  val viewId = rememberSaveable { View.generateViewId() }

  AndroidView(
    modifier = modifier,
    factory = { context -> FrameLayout(context).apply { id = viewId } },
  )
  DisposableEffect(fragmentManager, fragment) {
    fragmentManager.beginTransaction().run {
      replace(viewId, fragment)
      commitNow()
    }
    onDispose { fragmentManager.beginTransaction().remove(fragment).commitNowAllowingStateLoss() }
  }
}
