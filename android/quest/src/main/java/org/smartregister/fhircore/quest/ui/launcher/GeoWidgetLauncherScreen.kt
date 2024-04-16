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

import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import org.smartregister.fhircore.engine.configuration.geowidget.GeoWidgetConfiguration
import org.smartregister.fhircore.engine.domain.model.ToolBarHomeNavigation
import org.smartregister.fhircore.quest.event.ToolbarClickEvent
import org.smartregister.fhircore.quest.ui.main.components.TopScreenSection
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent

const val NO_REGISTER_VIEW_COLUMN_TEST_TAG = "noRegisterViewColumnTestTag"
const val NO_REGISTER_VIEW_TITLE_TEST_TAG = "noRegisterViewTitleTestTag"
const val NO_REGISTER_VIEW_MESSAGE_TEST_TAG = "noRegisterViewMessageTestTag"
const val NO_REGISTER_VIEW_BUTTON_TEST_TAG = "noRegisterViewButtonTestTag"
const val NO_REGISTER_VIEW_BUTTON_ICON_TEST_TAG = "noRegisterViewButtonIconTestTag"
const val NO_REGISTER_VIEW_BUTTON_TEXT_TEST_TAG = "noRegisterViewButtonTextTestTag"

@Composable
fun GeoWidgetLauncherScreen(
  modifier: Modifier = Modifier,
  openDrawer: (Boolean) -> Unit,
  onEvent: (GeoWidgetEvent) -> Unit,
  navController: NavController,
  toolBarHomeNavigation: ToolBarHomeNavigation = ToolBarHomeNavigation.OPEN_DRAWER,
  fragmentManager: FragmentManager,
  fragment: Fragment,
  geoWidgetConfiguration: GeoWidgetConfiguration,
) {
  Scaffold(
    topBar = {
      Column {
        /*
         * Top section has toolbar and a results counts view
         * by default isSearchBarVisible is visible
         * */
        TopScreenSection(
          title = geoWidgetConfiguration.topScreenSection?.title ?: "",
          searchText = "",
          filteredRecordsCount = 1,
          isSearchBarVisible = geoWidgetConfiguration.topScreenSection?.searchBar?.visible ?: true,
          searchPlaceholder = geoWidgetConfiguration.topScreenSection?.searchBar?.display,
          toolBarHomeNavigation = toolBarHomeNavigation,
          onSearchTextChanged = { searchText ->
            onEvent(GeoWidgetEvent.SearchServicePoints(searchText = searchText))
          },
          isFilterIconEnabled = false,
          topScreenSection = geoWidgetConfiguration.topScreenSection,
        ) { event ->
          when (event) {
            ToolbarClickEvent.Navigate ->
              when (toolBarHomeNavigation) {
                ToolBarHomeNavigation.OPEN_DRAWER -> openDrawer(true)
                ToolBarHomeNavigation.NAVIGATE_BACK -> navController.popBackStack()
              }
            ToolbarClickEvent.FilterData -> {}
            ToolbarClickEvent.Toggle -> {
              // TODO each menu icon should handle their own click events
              geoWidgetConfiguration.topScreenSection
                ?.menuIcons
                ?.first()
                ?.actions
                ?.handleClickEvent(
                  navController = navController,
                )
            }
          }
        }
      }
    },
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      FragmentContainerView(
        modifier = modifier,
        fragmentManager = fragmentManager,
        fragment = fragment,
      )
    }
  }
}

@Composable
fun FragmentContainerView(
  modifier: Modifier = Modifier,
  fragmentManager: FragmentManager,
  fragment: Fragment,
) {
  val viewId = remember { View.generateViewId() }
  AndroidView(
    modifier = modifier,
    factory = { context -> FrameLayout(context).apply { id = viewId } },
  )
  DisposableEffect(fragmentManager, fragment) {
    val transaction = fragmentManager.beginTransaction()
    transaction.replace(viewId, fragment)
    transaction.commitNow()

    onDispose { fragmentManager.beginTransaction().remove(fragment).commitNowAllowingStateLoss() }
  }
}
