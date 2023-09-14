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

package org.smartregister.fhircore.quest.integration.ui.profile.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavController
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.ui.profile.components.ICON_FIELD_TAG
import org.smartregister.fhircore.quest.ui.profile.components.MemberProfileBottomSheetView

class MemberProfileBottomSheetViewTest {

  @get:Rule val composeTestRule = createComposeRule()
  private val navController: NavController = mockk(relaxUnitFun = true)
  private val coroutineScope: CoroutineScope = mockk()

  @OptIn(ExperimentalMaterialApi::class)
  private val bottomSheetScaffoldState: BottomSheetScaffoldState = mockk()

  @OptIn(ExperimentalMaterialApi::class)
  @Before
  fun setUp() {
    val title = "Member Profile"
    val buttonProperties =
      listOf(
        ButtonProperties(
          text = "Button 1",
          status = "",
        ),
      )
    val resourceData = ResourceData("1", ResourceType.Patient, emptyMap())

    composeTestRule.setContent {
      MemberProfileBottomSheetView(
        modifier = Modifier.fillMaxWidth(),
        coroutineScope = coroutineScope,
        bottomSheetScaffoldState = bottomSheetScaffoldState,
        title = title,
        buttonProperties = buttonProperties,
        ResourceData = resourceData,
        navController = navController,
        onViewProfile = {},
      )
    }
  }

  @Test
  fun testMemberProfileTopSectionDisplayedCorrectly() {
    composeTestRule.onNodeWithText("What do you want to do?").assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithText("Member Profile").assertExists().assertIsDisplayed()
  }

  @Test
  fun testMemberProfileBottomSheetViewIconExistsAndHasClickAction() {
    composeTestRule.onNodeWithTag(ICON_FIELD_TAG).assertExists().assertHasClickAction()
  }

  @Test
  fun testViewProfileIsDisplayedAndHasClickAction() {
    composeTestRule
      .onNodeWithText("View profile")
      .assertExists()
      .assertIsDisplayed()
      .assertHasClickAction()
  }

  @Test
  fun testMemberProfileBottomSheetViewButtonsRendersCorrectly() {
    composeTestRule
      .onNodeWithText("Button 1")
      .assertExists()
      .assertIsDisplayed()
      .assertHasClickAction()
  }
}
