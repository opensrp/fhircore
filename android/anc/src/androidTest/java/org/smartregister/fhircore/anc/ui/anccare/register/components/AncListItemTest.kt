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

package org.smartregister.fhircore.anc.ui.anccare.register.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import java.util.Date
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.model.VisitStatus
import org.smartregister.fhircore.anc.ui.anccare.register.AncRowClickListenerIntent
import org.smartregister.fhircore.anc.ui.anccare.register.OpenPatientProfile
import org.smartregister.fhircore.anc.ui.anccare.register.RecordAncVisit
import org.smartregister.fhircore.anc.ui.report.PATIENT_ANC_VISIT
import org.smartregister.fhircore.engine.util.extension.toAgeDisplay

class AncListItemTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun testVerifyDefaultTitleAndSubTitle() {

    composeRule.setContent { AncRow(patientItem = getPatientItem(), clickListener = { _, _ -> }) }

    composeRule
      .onNodeWithText("John, M, ${Date().toAgeDisplay()}")
      .assertExists()
      .assertIsDisplayed()
    composeRule.onNodeWithText("Nairobi").assertExists().assertIsDisplayed()
  }

  @Test
  fun testVerifySelectionTitleAndSubTitle() {

    composeRule.setContent {
      AncRow(
        patientItem = getPatientItem(),
        clickListener = { _, _ -> },
        displaySelectContentOnly = true
      )
    }

    composeRule.onNodeWithText("John").assertExists().assertIsDisplayed()
    composeRule.onNodeWithText("Doe").assertExists().assertIsDisplayed()
  }

  @Test
  fun testVerifyAncVisitButtonShouldNotShow() {

    composeRule.setContent {
      AncRow(
        patientItem = getPatientItem(),
        clickListener = { _, _ -> },
        showAncVisitButton = false
      )
    }

    composeRule.onNodeWithTag(PATIENT_ANC_VISIT).assertDoesNotExist()
  }

  @Test
  fun testVerifyAncVisitButtonShouldShow() {

    composeRule.setContent { AncRow(patientItem = getPatientItem(), clickListener = { _, _ -> }) }

    composeRule.onNodeWithTag(PATIENT_ANC_VISIT).assertExists().assertIsDisplayed()
  }

  @Test
  fun testVerifyRowClickListenerAction() {

    val listenerIntentType = mutableListOf<AncRowClickListenerIntent>()

    composeRule.setContent {
      AncRow(
        patientItem = getPatientItem(),
        clickListener = { ancRowClickListenerIntent, item ->
          listenerIntentType.add(ancRowClickListenerIntent)
        }
      )
    }

    composeRule.onNodeWithTag(ANC_ROW_ITEM_TAG).performClick()

    Assert.assertEquals(1, listenerIntentType.size)
    Assert.assertEquals(OpenPatientProfile, listenerIntentType[0])
  }

  @Test
  fun testVerifyVisitButtonClickListenerAction() {

    val listenerIntentType = mutableListOf<AncRowClickListenerIntent>()

    composeRule.setContent {
      AncRow(
        patientItem = getPatientItem(),
        clickListener = { ancRowClickListenerIntent, item ->
          listenerIntentType.add(ancRowClickListenerIntent)
        }
      )
    }

    composeRule.onNodeWithTag(PATIENT_ANC_VISIT).performClick()

    Assert.assertEquals(1, listenerIntentType.size)
    Assert.assertEquals(RecordAncVisit, listenerIntentType[0])
  }

  private fun getPatientItem() =
    PatientItem(
      patientIdentifier = "1",
      name = "John",
      familyName = "Doe",
      gender = "M",
      birthDate = Date(),
      address = "Nairobi",
      visitStatus = VisitStatus.DUE
    )
}
