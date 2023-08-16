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

package org.smartregister.fhircore.quest.integration.ui.shared.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.view.SpacerProperties
import org.smartregister.fhircore.quest.ui.shared.components.HORIZONTAL_SPACER_TEST_TAG
import org.smartregister.fhircore.quest.ui.shared.components.SpacerView
import org.smartregister.fhircore.quest.ui.shared.components.VERTICAL_SPACER_TEST_TAG

class SpacerViewTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testSpacerVerticalSpacerIsRendered() {
    val spacerProperties = SpacerProperties(height = 16F, width = null)
    composeTestRule.setContent { SpacerView(spacerProperties = spacerProperties) }
    composeTestRule.onNodeWithTag(VERTICAL_SPACER_TEST_TAG).assertExists()
  }

  @Test
  fun testHorizontalSpacerIsRendered() {
    val spacerProperties = SpacerProperties(height = null, width = 16F)
    composeTestRule.setContent { SpacerView(spacerProperties = spacerProperties) }
    composeTestRule.onNodeWithTag(HORIZONTAL_SPACER_TEST_TAG, useUnmergedTree = true).assertExists()
  }
}
