/*
 * Copyright 2021 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

internal class CircularPercentageIndicatorKtTest {

  private val textPercentage = "25"

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun testCircularPercentageIndicatorWithText() {
    composeRule.setContent { CircularPercentageIndicator(percentage = textPercentage) }
    composeRule.onNodeWithTag(CIRCULAR_PERCENTAGE_INDICATOR).assertExists()
    composeRule.onNodeWithTag(CIRCULAR_CANVAS_CIRCLE_TAG).assertExists()
    composeRule.onNodeWithTag(CIRCULAR_PERCENTAGE_TEXT_TAG).assertExists()
  }
}
