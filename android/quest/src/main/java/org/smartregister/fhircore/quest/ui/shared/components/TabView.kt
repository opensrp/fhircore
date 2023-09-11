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

package org.smartregister.fhircore.quest.ui.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.view.*
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.engine.util.extension.parseColor

@OptIn(ExperimentalPagerApi::class)
@Composable
fun TabView(
  modifier: Modifier = Modifier,
  viewProperties: TabViewProperties,
  resourceData: ResourceData,
  navController: NavController,
  selectedTabIndex: Int? = null,
  tabChangedEvent: ((Int) -> Unit)? = null
) {
  val pagerState = rememberPagerState(initialPage = selectedTabIndex ?: viewProperties.selectedTabIndex)

  LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.currentPage }.collect { page ->
      tabChangedEvent?.let { it(page) }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(viewProperties.tabBackgroundColor.parseColor())) {

    //tabs header
    Tabs(
      pagerState = pagerState,
      viewProperties = viewProperties,
    )

    //tabs content
    TabContents(
      pagerState = pagerState,
      viewProperties = viewProperties,
      resourceData = resourceData,
      navController = navController,
    )
  }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun Tabs(
  pagerState: PagerState,
  viewProperties: TabViewProperties
) {
  val scope = rememberCoroutineScope()

  if(viewProperties.tabs.size > 3) {
    ScrollableTabRow(
      selectedTabIndex = pagerState.currentPage,
      edgePadding = 10.dp,
      indicator = { tabPositions ->
        TabRowDefaults.Indicator(
          Modifier.pagerTabIndicatorOffset(pagerState, tabPositions),
          height = 5.dp,
          color = viewProperties.tabIndicatorColor.parseColor(),
        )
      }
    ) {
      viewProperties.tabs.forEachIndexed { index, _ ->
        Tab(
          selected = pagerState.currentPage == index,
          onClick = {
            scope.launch {
              pagerState.animateScrollToPage(index)
            }
          },
          text = {
            Text(
              viewProperties.tabs[index],
              color = if (pagerState.currentPage == index) Color.White else Color.LightGray,
              fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
            )
          }
        )
      }
    }
  } else {
    TabRow(
      selectedTabIndex = pagerState.currentPage,
      indicator = { tabPositions ->
        TabRowDefaults.Indicator(
          Modifier.pagerTabIndicatorOffset(pagerState, tabPositions),
          height = 5.dp,
          color = viewProperties.tabIndicatorColor.parseColor(),
        )
      }
    ) {
      viewProperties.tabs.forEachIndexed { index, _ ->
        Tab(
          selected = pagerState.currentPage == index,
          onClick = {
            scope.launch {
              pagerState.animateScrollToPage(index)
            }
          },
          text = {
            Text(
              viewProperties.tabs[index],
              color = if (pagerState.currentPage == index) Color.White else Color.LightGray,
              fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
            )
          }
        )
      }
    }
  }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun TabContents(
  pagerState: PagerState,
  viewProperties: TabViewProperties,
  resourceData: ResourceData,
  navController: NavController
) {
  HorizontalPager(
    verticalAlignment = Alignment.Top,
    count = viewProperties.tabs.size,
    state = pagerState,
    userScrollEnabled = false
  ) { pageIndex ->
    if(viewProperties.contentScrollable) {
      LazyColumn(
        state = rememberLazyListState(),
        contentPadding = PaddingValues(bottom = 20.dp)
      ) {
        item(key = resourceData.baseResourceId) {
          ViewRenderer(
            viewProperties = listOf(viewProperties.tabContents[pageIndex]),
            resourceData = resourceData,
            navController = navController
          )
        }
      }
    } else {
      ViewRenderer(
        viewProperties = listOf(viewProperties.tabContents[pageIndex]),
        resourceData = resourceData,
        navController = navController
      )
    }
  }
}

@OptIn(ExperimentalPagerApi::class)
@PreviewWithBackgroundExcludeGenerated
@Composable
private fun TabViewPreview() {
  val pagerState = rememberPagerState(
    initialPage = 0,
  )

  val viewProperties = TabViewProperties(
    viewType = ViewType.TABS,
    tabs = listOf("Tab1", "Tab2", "Tab3"),
    tabContents = listOf(
      CompoundTextProperties(
        primaryText = "Tab1",
        primaryTextColor = "#000000",
      ),
      CompoundTextProperties(
        primaryText = "Tab2",
        primaryTextColor = "#000000",
      ),
      CompoundTextProperties(
        primaryText = "Tab3",
        primaryTextColor = "#000000",
      ),
    ),
  )

  Column(modifier = Modifier.fillMaxWidth()) {
    //tabs header
    Tabs(
      pagerState = pagerState,
      viewProperties = viewProperties,
    )

    //tabs content
    TabContents(
      pagerState = pagerState,
      viewProperties = viewProperties,
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
      navController = rememberNavController(),
    )
  }
}
