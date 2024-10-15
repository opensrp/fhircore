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
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.testing.TestNavHostController
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.view.CardViewProperties
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.quest.ui.shared.components.CardView

class CardViewTest {
  private val resourceData = ResourceData("id", ResourceType.Patient, emptyMap())

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testHeaderAndRecordAllIsDisplayedAndClickable() {
    val viewProperties = initTestCardViewProperties(clickable = "true")
    composeTestRule.setContent {
      CardView(
        viewProperties = viewProperties,
        resourceData = resourceData,
        navController = TestNavHostController(LocalContext.current),
        decodeImage = null,
      )
    }
    composeTestRule.onNodeWithText("IMMUNIZATIONS").assertIsDisplayed()
    composeTestRule.onNodeWithText("Record all").assertIsDisplayed().assertHasClickAction()
  }

  private fun initTestCardViewProperties(
    clickable: String = "false",
    visible: String = "true",
  ): CardViewProperties {
    return CardViewProperties(
      viewType = ViewType.CARD,
      header =
        CompoundTextProperties(
          primaryText = "IMMUNIZATIONS",
          fontSize = 18.0f,
          primaryTextColor = "#6F7274",
          visible = visible,
        ),
      headerAction =
        CompoundTextProperties(
          primaryText = "Record all",
          primaryTextColor = "infoColor",
          clickable = clickable,
          visible = visible,
        ),
    )
  }
}
