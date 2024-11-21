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

package org.smartregister.fhircore.quest.integration.ui.register.components

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.testing.TestNavHostController
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.flowOf
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.register.RegisterCardConfig
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.ui.register.RegisterUiCountState
import org.smartregister.fhircore.quest.ui.register.RegisterUiState
import org.smartregister.fhircore.quest.ui.register.components.REGISTER_CARD_LIST_TEST_TAG
import org.smartregister.fhircore.quest.ui.register.components.RegisterCardList

@HiltAndroidTest
class RegisterCardListTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testRegisterCardListShouldHaveProgressNodeIfNotData() {
    composeTestRule.setContent {
      val pagingItems = flowOf(PagingData.from(listOf<ResourceData>())).collectAsLazyPagingItems()

      RegisterCardList(
        registerCardConfig = RegisterCardConfig(),
        pagingItems = pagingItems,
        navController = TestNavHostController(LocalContext.current),
        lazyListState = rememberLazyListState(),
        onEvent = {},
        registerUiState = RegisterUiState(),
        registerUiCountState = RegisterUiCountState(),
        currentPage = mutableStateOf(1),
        onSearchByQrSingleResultAction = {},
        decodeImage = null,
      )
    }

    composeTestRule.onNodeWithTag(REGISTER_CARD_LIST_TEST_TAG).onChildren().assertCountEquals(1)
  }

  @Test
  fun testRegisterCardListShouldHaveThreeItems() {
    composeTestRule.setContent {
      val config =
        RegisterCardConfig(views = listOf(CompoundTextProperties(primaryText = "Patient 1")))

      val data = listOf(ResourceData("1", ResourceType.Patient, emptyMap()))

      val pagingItems = flowOf(PagingData.from(data)).collectAsLazyPagingItems()

      RegisterCardList(
        registerCardConfig = config,
        pagingItems = pagingItems,
        navController = TestNavHostController(LocalContext.current),
        lazyListState = rememberLazyListState(),
        onEvent = {},
        registerUiState = RegisterUiState(),
        registerUiCountState = RegisterUiCountState(),
        currentPage = mutableStateOf(1),
        onSearchByQrSingleResultAction = {},
        decodeImage = null,
      )
    }

    composeTestRule.onNodeWithTag(REGISTER_CARD_LIST_TEST_TAG).onChildren().assertCountEquals(2)

    composeTestRule
      .onNodeWithTag(REGISTER_CARD_LIST_TEST_TAG)
      .onChildren()
      .onFirst()
      .assert(hasText("Patient 1"))
  }

  @Test
  fun testRegisterCardListWithPaginationShouldHaveFourItems() {
    composeTestRule.setContent {
      val config =
        RegisterCardConfig(views = listOf(CompoundTextProperties(primaryText = "Patient 1")))

      val data = listOf(ResourceData("1", ResourceType.Patient, emptyMap()))

      val pagingItems = flowOf(PagingData.from(data)).collectAsLazyPagingItems()

      RegisterCardList(
        registerCardConfig = config,
        pagingItems = pagingItems,
        navController = TestNavHostController(LocalContext.current),
        lazyListState = rememberLazyListState(),
        onEvent = {},
        registerUiState = RegisterUiState(),
        registerUiCountState = RegisterUiCountState(),
        currentPage = mutableStateOf(1),
        showPagination = true,
        onSearchByQrSingleResultAction = {},
        decodeImage = null,
      )
    }

    composeTestRule.onNodeWithTag(REGISTER_CARD_LIST_TEST_TAG).onChildren().assertCountEquals(3)

    composeTestRule
      .onNodeWithTag(REGISTER_CARD_LIST_TEST_TAG)
      .onChildren()
      .onFirst()
      .assert(hasText("Patient 1"))
  }

  @Test
  fun testRegisterCardListShouldRenderLoadingIndicatorIfPagingItemsIsEmpty() {
    composeTestRule.setContent {
      val pagingItems = flowOf(PagingData.empty<ResourceData>()).collectAsLazyPagingItems()

      RegisterCardList(
        registerCardConfig = RegisterCardConfig(),
        pagingItems = pagingItems,
        navController = TestNavHostController(LocalContext.current),
        lazyListState = rememberLazyListState(),
        onEvent = {},
        registerUiState = RegisterUiState(),
        registerUiCountState = RegisterUiCountState(),
        currentPage = mutableStateOf(1),
        onSearchByQrSingleResultAction = {},
        decodeImage = null,
      )
    }

    composeTestRule
      .onNodeWithTag(REGISTER_CARD_LIST_TEST_TAG)
      .onChildren()
      .onFirst()
      .onChildren()
      .onFirst()
  }
}
