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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.estimateAnimationDurationMillis
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.navigation.NavController
import io.mockk.mockk
import org.checkerframework.checker.units.qual.A
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_LOCAL
import org.smartregister.fhircore.engine.configuration.navigation.MenuIconConfig
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.util.extensions.isScrollingUp

class ExtendedFabTest {
  private val navController = mockk<NavController>(relaxed = true, relaxUnitFun = true)

  @get:Rule val composeRule = createComposeRule()
  @get:Rule val rule = createComposeRule()
  @Before
  fun init() {
    composeRule.setContent {
      ExtendedFab(
        fabActions =
          listOf(
            NavigationMenuConfig(
              id = "test",
              display = "Fab Button",
              menuIconConfig = MenuIconConfig(type = ICON_TYPE_LOCAL, reference = "ic_user"),
              actions =
                listOf(
                  ActionConfig(
                    trigger = ActionTrigger.ON_CLICK,
                    workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
                    questionnaire = QuestionnaireConfig(id = "23", title = "Add Family"),
                  )
                )
            )
          ),
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
        navController = navController,
        lazyListState = rememberLazyListState()
      )
    }
  }

  @Test
  fun testFloatingButtonIsDisplayed() {
    composeRule
      .onNodeWithTag(FAB_BUTTON_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun extendedFabButtonRendersRowCorrectly() {
    composeRule
      .onNodeWithTag(FAB_BUTTON_ROW_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun extendedFabButtonRendersRowIconCorrectly() {
    composeRule
      .onNodeWithTag(FAB_BUTTON_ROW_ICON_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }
  data class FabAction(val animate: Boolean)
  @Test
  fun animationAppearsWhenAnimateIsFalse(){
    rule.setContent {
        FabAction(animate = false)
    }
    rule.onNodeWithTag(FAB_BUTTON_ROW_ICON_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
    }
  @Test
  fun animationAppearsWhenAnimateIsTrue(){
    var animate by mutableStateOf(false)
    rule.mainClock.autoAdvance = true
    rule.setContent {
      if (animate) {
        rule.onNodeWithTag(FAB_BUTTON_ROW_ICON_TEST_TAG, useUnmergedTree = true)
          .assertIsDisplayed()
      }
    }
    rule.mainClock.advanceTimeBy(50L)

  }
    }
