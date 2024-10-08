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

package org.smartregister.fhircore.quest.integration.ui.shared.components

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.testing.TestNavHostController
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_LOCAL
import org.smartregister.fhircore.engine.configuration.navigation.ImageConfig
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.ui.shared.components.ExtendedFab
import org.smartregister.fhircore.quest.ui.shared.components.FAB_BUTTON_ROW_ICON_TEST_TAG
import org.smartregister.fhircore.quest.ui.shared.components.FAB_BUTTON_ROW_TEST_TAG
import org.smartregister.fhircore.quest.ui.shared.components.FAB_BUTTON_ROW_TEXT_TEST_TAG
import org.smartregister.fhircore.quest.ui.shared.components.FAB_BUTTON_TEST_TAG

class ExtendedFabTest {
  @get:Rule val composeRule = createComposeRule()

  private fun init() {
    composeRule.setContent {
      ExtendedFab(
        fabActions =
          listOf(
            NavigationMenuConfig(
              id = "test",
              display = "Fab Button",
              menuIconConfig = ImageConfig(type = ICON_TYPE_LOCAL, reference = "ic_user"),
              actions =
                listOf(
                  ActionConfig(
                    trigger = ActionTrigger.ON_CLICK,
                    workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE.name,
                    questionnaire = QuestionnaireConfig(id = "23", title = "Add Family"),
                  ),
                ),
            ),
          ),
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
        navController = TestNavHostController(LocalContext.current),
        lazyListState = null,
        decodeImage = null,
      )
    }
  }

  @Test
  fun can_initializing_extended_fab_with_null_resource_data() {
    composeRule.setContent {
      ExtendedFab(
        fabActions =
          listOf(
            NavigationMenuConfig(
              id = "test",
              display = "Fab Button",
              menuIconConfig = ImageConfig(type = ICON_TYPE_LOCAL, reference = "ic_user"),
              actions =
                listOf(
                  ActionConfig(
                    trigger = ActionTrigger.ON_CLICK,
                    workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE.name,
                    questionnaire = QuestionnaireConfig(id = "23", title = "Add Family"),
                  ),
                ),
            ),
          ),
        resourceData = null,
        navController = TestNavHostController(LocalContext.current),
        lazyListState = null,
        decodeImage = null,
      )
    }
    composeRule
      .onNodeWithTag(FAB_BUTTON_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testFloatingButtonIsDisplayed() {
    init()
    composeRule
      .onNodeWithTag(FAB_BUTTON_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun extendedFabButtonRendersRowCorrectly() {
    init()
    composeRule
      .onNodeWithTag(FAB_BUTTON_ROW_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun extendedFabButtonRendersRowIconCorrectly() {
    init()
    composeRule
      .onNodeWithTag(FAB_BUTTON_ROW_ICON_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testFloatingButtonWhenAnimateIsFalse() {
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      ExtendedFab(
        fabActions =
          listOf(
            NavigationMenuConfig(
              id = "test",
              display = "Fab Button",
              menuIconConfig = ImageConfig(type = ICON_TYPE_LOCAL, reference = "ic_user"),
              animate = false,
              actions =
                listOf(
                  ActionConfig(
                    trigger = ActionTrigger.ON_CLICK,
                    workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE.name,
                    questionnaire = QuestionnaireConfig(id = "23", title = "Add Family"),
                  ),
                ),
            ),
          ),
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
        navController = TestNavHostController(LocalContext.current),
        lazyListState = null,
        decodeImage = null,
      )
    }
    composeRule.run {
      onNodeWithTag(FAB_BUTTON_ROW_ICON_TEST_TAG, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

      onNodeWithTag(FAB_BUTTON_ROW_TEXT_TEST_TAG, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
    }
  }

  @Test
  fun testFloatingButtonWhenAnimateIsTrue() {
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      ExtendedFab(
        fabActions =
          listOf(
            NavigationMenuConfig(
              id = "test",
              display = "Fab Button",
              menuIconConfig = ImageConfig(type = ICON_TYPE_LOCAL, reference = "ic_user"),
              animate = true,
              actions =
                listOf(
                  ActionConfig(
                    trigger = ActionTrigger.ON_CLICK,
                    workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE.name,
                    questionnaire = QuestionnaireConfig(id = "23", title = "Add Family"),
                  ),
                ),
            ),
          ),
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
        navController = TestNavHostController(LocalContext.current),
        lazyListState = null,
        decodeImage = null,
      )
    }
    composeRule.run {
      onNodeWithTag(FAB_BUTTON_ROW_ICON_TEST_TAG, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

      onNodeWithTag(FAB_BUTTON_ROW_TEXT_TEST_TAG, useUnmergedTree = true).assertDoesNotExist()
    }
  }
}
