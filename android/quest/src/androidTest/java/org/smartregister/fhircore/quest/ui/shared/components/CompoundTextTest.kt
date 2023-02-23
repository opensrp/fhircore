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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavController
import io.mockk.mockk
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.TextFontWeight
import org.smartregister.fhircore.engine.domain.model.ResourceData

class CompoundTextTest {

  @get:Rule val composeRule = createComposeRule()

  private val navController = mockk<NavController>(relaxed = true, relaxUnitFun = true)

  @Test
  fun primaryTextIsRenderedCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          CompoundTextProperties(
            primaryText = "Full Name, Age",
            primaryTextColor = "#000000",
            primaryTextFontWeight = TextFontWeight.SEMI_BOLD
          ),
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Full Name, Age").assertExists().assertIsDisplayed()
  }

  @Test
  fun secondaryTextRenderedCorrectly() {
    composeRule.setContent {
      GenerateView(
        properties =
          CompoundTextProperties(
            primaryText = "Last visited",
            primaryTextColor = "#5A5A5A",
            secondaryText = "Yesterday",
            secondaryTextColor = "#FFFFFF",
            separator = ".",
            secondaryTextBackgroundColor = "#FFA500"
          ),
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
        navController = navController
      )
    }
    composeRule.onNodeWithText("Yesterday").assertExists().assertIsDisplayed()
  }
}
