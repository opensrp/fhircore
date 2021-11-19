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

package org.smartregister.fhircore.quest.ui.patient.register.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.data.patient.model.PatientItem
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

class PatientRowTest : RobolectricTest() {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun testPatientRowComponents() {
    val patientItem =
      PatientItem(
        id = "my-test-id",
        identifier = "10001",
        name = "John Doe",
        gender = "Male",
        age = "27",
        address = "Nairobi"
      )
    composeRule.setContent { PatientRow(patientItem = patientItem, { _, _ -> }) }

    composeRule.onNodeWithText("Male").assertExists()
    composeRule.onNodeWithText("Male").assertIsDisplayed()
    composeRule.onNodeWithText("John Doe, 27").assertExists()
    composeRule.onNodeWithText("John Doe, 27").assertIsDisplayed()
  }
}
