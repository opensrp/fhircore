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

package org.smartregister.fhircore.quest.ui.patient.details

import android.app.Application
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import java.util.Date
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.shadow.QuestApplicationShadow

@Config(shadows = [QuestApplicationShadow::class])
class QuestPatientDetailScreenTest : RobolectricTest() {

  @get:Rule val composeRule = createComposeRule()
  private val app = ApplicationProvider.getApplicationContext<Application>()

  @Test
  fun testQuestPatientDetailScreenComponents() {
    composeRule.setContent { QuestPatientDetailScreen(dummyQuestPatientDetailDataProvider()) }

    // toolbar should have valid title and icon
    composeRule
      .onNodeWithTag(TOOLBAR_TITLE)
      .assertTextEquals(app.getString(R.string.back_to_clients))
    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).assertHasClickAction()

    // check and perform click action in toolbar menu
    composeRule.onNodeWithTag(TOOLBAR_MENU_BUTTON).assertHasClickAction()
    composeRule.onNodeWithTag(TOOLBAR_MENU_BUTTON).performClick()

    // toolbar menu should be shown on menu clicked
    composeRule.onNodeWithTag(TOOLBAR_MENU).assertIsDisplayed()

    // verify patient name with gender and age
    composeRule.onNodeWithTag(PATIENT_NAME).assertExists()
    composeRule.onNodeWithTag(PATIENT_NAME).assertIsDisplayed()
    composeRule.onNodeWithTag(PATIENT_NAME).assertTextEquals("John Doe, M, 21y")

    // verify form item(s) count and item title
    composeRule.onAllNodesWithTag(FORM_ITEM).assertCountEquals(1)
    composeRule.onAllNodesWithTag(FORM_ITEM)[0].assertTextEquals("+ SAMPLE TEST RESULT")

    // response heading should be exist and must be displayed
    composeRule.onNodeWithText("RESPONSES").assertExists()
    composeRule.onNodeWithText("RESPONSES").assertIsDisplayed()

    // verify test result item(s) count and item title
    composeRule.onAllNodesWithTag(RESULT_ITEM).assertCountEquals(1)
    composeRule.onAllNodesWithTag(RESULT_ITEM, true)[0].assert(
      hasAnyChild(hasText("Sample Test (${Date().asDdMmmYyyy()}) "))
    )
  }
}
