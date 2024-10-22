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

package org.smartregister.fhircore.quest.integration.ui.profile.components

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.ui.profile.components.ICON_FIELD_TAG
import org.smartregister.fhircore.quest.ui.profile.components.MemberProfileBottomSheetView
import org.smartregister.fhircore.quest.ui.profile.components.TOP_SECTION_ROW
import org.smartregister.fhircore.quest.ui.profile.components.VIEW_PROFILE_TAG

@OptIn(ExperimentalMaterialApi::class)
class MemberProfileBottomSheetViewTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    val buttonProperties =
      listOf(
        ButtonProperties(text = "Issue bednet", status = "OVERDUE"),
      )
    composeTestRule.setContent {
      MemberProfileBottomSheetView(
        title = "John Doe, M, 35y",
        buttonProperties = buttonProperties,
        onViewProfile = { /*Do nothing*/},
        resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
        decodeImage = null,
      )
    }
  }

  @Test
  fun testTopMemberProfileSectionDisplayedCorrectly() {
    composeTestRule.onNodeWithTag(TOP_SECTION_ROW).assertExists().assertIsDisplayed()
  }

  @Test
  fun testViewProfileIsDisplayedAndHasClickAction() {
    composeTestRule
      .onNodeWithTag(VIEW_PROFILE_TAG)
      .assertExists()
      .assertIsDisplayed()
      .assertHasClickAction()
  }

  @Test
  fun testMemberProfileBottomSheetViewIconExistsAndHasClickAction() {
    composeTestRule.onNodeWithTag(ICON_FIELD_TAG).assertExists().assertHasClickAction()
  }

  @Test
  fun testMemberProfileBottomSheetViewButtonsRendersCorrectly() {
    composeTestRule
      .onNodeWithText("Issue bednet")
      .assertExists()
      .assertIsDisplayed()
      .assertHasClickAction()
  }
}
