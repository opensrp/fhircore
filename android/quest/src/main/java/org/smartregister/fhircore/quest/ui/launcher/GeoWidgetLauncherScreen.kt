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
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import org.smartregister.fhircore.quest.ui.register.RegisterEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.commit
import androidx.navigation.NavController
import org.smartregister.fhircore.engine.domain.model.ToolBarHomeNavigation
import org.smartregister.fhircore.geowidget.screens.GeoWidgetFragment
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.event.ToolbarClickEvent
import org.smartregister.fhircore.quest.ui.launcher.GeoWidgetLauncherFragment.Companion.GEO_WIDGET_FRAGMENT_TAG
import org.smartregister.fhircore.quest.ui.main.components.TopScreenSection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager


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
    onEvent: (RegisterEvent) -> Unit,
    navController: NavController,
    toolBarHomeNavigation: ToolBarHomeNavigation = ToolBarHomeNavigation.OPEN_DRAWER,
    fragmentManager: FragmentManager,
    fragment: Fragment
) {

    Scaffold(
        topBar = {
            Column {
                /*
                * Top section has toolbar and a results counts view
                * by default isSearchBarVisible is visible
                * */
                TopScreenSection(
                    title = "Map",
                    searchText = "",
                    filteredRecordsCount = 1,
                    isSearchBarVisible = true,
                    searchPlaceholder = "Search",
                    toolBarHomeNavigation = toolBarHomeNavigation,
                    onSearchTextChanged = { searchText ->
                        onEvent(RegisterEvent.SearchRegister(searchText = searchText))
                    },
                    isFilterIconEnabled = false,
                ) { event ->
                    when (event) {
                        ToolbarClickEvent.Navigate ->
                            when (toolBarHomeNavigation) {
                                ToolBarHomeNavigation.OPEN_DRAWER -> openDrawer(true)
                                ToolBarHomeNavigation.NAVIGATE_BACK -> navController.popBackStack()
                            }
                        ToolbarClickEvent.FilterData -> {
                            onEvent(RegisterEvent.ResetFilterRecordsCount)

                        }
                    }
                }

            }
        }
    ) { innerPadding ->
        Box(modifier = modifier.padding(innerPadding)) {
            FragmentContainerView(modifier = modifier, fragmentManager = fragmentManager, fragment = fragment)
        }
    }
}

@Composable
fun FragmentContainerView(
    modifier: Modifier = Modifier,
    fragmentManager: FragmentManager,
    fragment: Fragment
) {
    val viewId = remember { View.generateViewId() }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            FrameLayout(context).apply {
                id = viewId
            }
        }
    )
    DisposableEffect(fragmentManager, fragment) {
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(viewId, fragment)
        transaction.commitNow()

        onDispose {
            fragmentManager.beginTransaction().remove(fragment).commitNowAllowingStateLoss()
        }
    }
}

