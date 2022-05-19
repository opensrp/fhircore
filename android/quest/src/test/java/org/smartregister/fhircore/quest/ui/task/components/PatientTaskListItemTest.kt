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

package org.smartregister.fhircore.quest.ui.task.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import io.mockk.spyk
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.util.DateUtils.getDate
import org.smartregister.fhircore.quest.data.task.model.PatientTaskItem
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.task.PatientTaskListenerIntent

class PatientTaskListItemTest : RobolectricTest() {

  @get:Rule val composeRule = createComposeRule()

  private val listenerObjectSpy =
    spyk(
      object {
        fun onRowClick(): (PatientTaskListenerIntent, PatientTaskItem) -> Unit {
          // Imitate row click action by doing nothing
          return { _: PatientTaskListenerIntent, _: PatientTaskItem -> }
        }
      }
    )

  @Test
  fun testPatientTaskListItemWhenIdIsNotEmptyShouldDisplayLabel() {
    val patientTaskItem =
      PatientTaskItem(
        id = "1",
        name = "Eve",
        gender = "F",
        birthdate = "2020-03-10".getDate("yyyy-MM-dd"),
        address = "Nairobi",
        description = "Sick Visit",
        overdue = true
      )
    composeRule.setContent {
      PatientTaskRow(
        patientItem = patientTaskItem,
        useLabel = true,
        clickListener = listenerObjectSpy.onRowClick(),
        modifier = Modifier
      )
    }

    composeRule.onNodeWithTag(TEXT_TITLE).assertExists()
    composeRule.onNodeWithTag(TEXT_TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(TEXT_TITLE).assertTextEquals(patientTaskItem.demographics())

    composeRule.onNodeWithTag(TEXT_SUBTITLE_ADDRESS).assertExists()
    composeRule.onNodeWithTag(TEXT_SUBTITLE_ADDRESS).assertIsDisplayed()
    composeRule.onNodeWithTag(TEXT_SUBTITLE_ADDRESS).assertTextEquals("Nairobi")

    composeRule.onNodeWithTag(TEXT_SUBTITLE_ID).assertExists()
    composeRule.onNodeWithTag(TEXT_SUBTITLE_ID).assertIsDisplayed()
    composeRule.onNodeWithTag(TEXT_SUBTITLE_ID).assertTextEquals("#1")

    composeRule.onNodeWithTag(LABEL_MAIN).assertExists()
    composeRule.onNodeWithTag(LABEL_MAIN).assertIsDisplayed()
  }

  @Test
  fun testPatientTaskListItemWhenIdIsEmptyShouldDisplayLabel() {
    val patientTaskItem =
      PatientTaskItem(
        id = "",
        name = "Eve",
        gender = "F",
        birthdate = "2020-03-10".getDate("yyyy-MM-dd"),
        address = "Nairobi",
        description = "Sick Visit",
        overdue = false
      )
    composeRule.setContent {
      PatientTaskRow(
        patientItem = patientTaskItem,
        useLabel = true,
        clickListener = listenerObjectSpy.onRowClick(),
        modifier = Modifier
      )
    }

    composeRule.onNodeWithTag(TEXT_TITLE).assertExists()
    composeRule.onNodeWithTag(TEXT_TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(TEXT_TITLE).assertTextEquals(patientTaskItem.demographics())

    composeRule.onNodeWithTag(TEXT_SUBTITLE_ADDRESS).assertExists()
    composeRule.onNodeWithTag(TEXT_SUBTITLE_ADDRESS).assertIsDisplayed()
    composeRule.onNodeWithTag(TEXT_SUBTITLE_ADDRESS).assertTextEquals("Nairobi")

    composeRule.onNodeWithTag(TEXT_SUBTITLE_ID).assertDoesNotExist()

    composeRule.onNodeWithTag(LABEL_MAIN).assertExists()
    composeRule.onNodeWithTag(LABEL_MAIN).assertIsDisplayed()
  }

  @Test
  fun testPatientTaskListItemShouldDisplayIcon() {
    val patientTaskItem =
      PatientTaskItem(
        id = "1",
        name = "Eve",
        gender = "F",
        birthdate = "2020-03-10".getDate("yyyy-MM-dd"),
        address = "Nairobi",
        description = "Sick Visit",
        overdue = true
      )
    composeRule.setContent {
      PatientTaskRow(
        patientItem = patientTaskItem,
        useLabel = false,
        clickListener = listenerObjectSpy.onRowClick(),
        modifier = Modifier
      )
    }

    composeRule.onNodeWithTag(TEXT_TITLE).assertExists()
    composeRule.onNodeWithTag(TEXT_TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(TEXT_TITLE).assertTextEquals(patientTaskItem.demographics())

    composeRule.onNodeWithTag(ICON_SUBTITLE).assertExists()

    composeRule.onNodeWithTag(TEXT_SUBTITLE_DISTANCE).assertExists()
    composeRule.onNodeWithTag(TEXT_SUBTITLE_DISTANCE).assertIsDisplayed()
    composeRule.onNodeWithTag(TEXT_SUBTITLE_DISTANCE).assertTextEquals("2.4 km")

    composeRule.onNodeWithTag(TEXT_SUBTITLE_DESCRIPTION).assertExists()
    composeRule.onNodeWithTag(TEXT_SUBTITLE_DESCRIPTION).assertIsDisplayed()
    composeRule.onNodeWithTag(TEXT_SUBTITLE_DESCRIPTION).assertTextEquals("Sick Visit")

    composeRule.onNodeWithTag(ICON_MAIN).assertExists()
    composeRule.onNodeWithTag(ICON_MAIN).assertIsDisplayed()
  }

  @Test
  fun testPatientTaskListItemWhenDisplaySelectContentOnlyIsTrue() {
    composeRule.setContent {
      PatientTaskRow(
        patientItem =
          PatientTaskItem(
            id = "1",
            name = "Eve",
            gender = "F",
            birthdate = "2020-03-10".getDate("yyyy-MM-dd"),
            address = "Nairobi",
            description = "Sick Visit",
            overdue = false
          ),
        useLabel = false,
        displaySelectContentOnly = true,
        clickListener = listenerObjectSpy.onRowClick(),
        modifier = Modifier
      )
    }

    composeRule.onNodeWithTag(TEXT_TITLE).assertExists()
    composeRule.onNodeWithTag(TEXT_TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(TEXT_TITLE).assertTextEquals("Eve")
  }
}
