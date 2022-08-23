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

package org.smartregister.fhircore.quest.ui.profile.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.profile.ManagingEntityConfig
import org.smartregister.fhircore.engine.domain.model.ExtractedResource
import org.smartregister.fhircore.quest.ui.profile.model.EligibleManagingEntity

class ChangeManagingEntityViewTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    val eligibleManagingEntities =
      listOf(
        EligibleManagingEntity(
          groupId = "group-1",
          logicalId = "patient-1",
          memberInfo = "Jane Doe"
        )
      )
    composeTestRule.setContent {
      ChangeManagingEntityView(
        onSaveClick = {},
        eligibleManagingEntities = eligibleManagingEntities,
        onDismiss = {},
        managingEntity =
          ManagingEntityConfig(
            infoFhirPathExpression = "Patient.name",
            fhirPathResource =
              ExtractedResource(resourceType = "Patient", fhirPathExpression = "Patient.active"),
            dialogTitle = "Assign new family head",
            dialogWarningMessage = "Are you sure you want to abort this operation?",
            dialogContentMessage = "Select a new family head"
          )
      )
    }
  }

  @Test
  fun testChangeManagingEntityViewDisplaysHeader() {
    composeTestRule.onNodeWithText("Assign new family head").assertExists().assertIsDisplayed()
  }

  @Test
  fun testChangeManagingEntityViewDisplaysAbortOperationMessage() {
    composeTestRule
      .onNodeWithText("Are you sure you want to abort this operation?")
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun testChangeManagingEntityViewDisplaysSelectNewFamilyHeadTitle() {
    composeTestRule.onNodeWithText("Select a new family head").assertExists().assertIsDisplayed()
  }

  @Test
  fun testChangeManagingEntityViewDisplaysCancelAndSaveButtons() {
    composeTestRule.onNodeWithTag(TEST_TAG_CANCEL).assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_SAVE).assertExists().assertIsDisplayed()
  }

  @Test
  fun testChangeManagingEntityViewDisplaysManagingEntityListItem() {
    composeTestRule.onNodeWithText("Jane Doe").assertExists().assertIsDisplayed()
  }
}
