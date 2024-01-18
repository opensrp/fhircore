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

package org.smartregister.fhircore.engine.ui.theme

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.ui.components.CircularPercentageIndicator

internal class ThemesTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun testThemes() {
    composeRule.setContent { AppTheme { CircularPercentageIndicator(percentage = "0") } }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun testThemesDarkTheme() {
    composeRule.setContent { AppTheme(true) { CircularPercentageIndicator(percentage = "0") } }
    composeRule.onRoot().assertIsDisplayed()
  }
}
