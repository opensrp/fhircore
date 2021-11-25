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
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class QuestPatientTestResultScreenTest : RobolectricTest() {

  @BindValue val patientRepository: PatientRepository = Faker.patientRepository

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val composeRule = createComposeRule()

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private lateinit var questPatientDetailViewModel: QuestPatientDetailViewModel

  @Before
  fun setUp() {
    hiltRule.inject()
    questPatientDetailViewModel = spyk(QuestPatientDetailViewModel(patientRepository))
  }

  @Test
  fun testToolbarComponents() {
    composeRule.setContent { QuestPatientTestResultScreen(questPatientDetailViewModel) }
    composeRule
      .onNodeWithTag(TOOLBAR_TITLE)
      .assertTextEquals(application.getString(R.string.back_to_clients))
    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).assertHasClickAction()
  }

  @Test
  fun testToolbarBackPressedButtonShouldCallBackPressedClickListener() {
    composeRule.setContent { QuestPatientTestResultScreen(questPatientDetailViewModel) }
    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).performClick()
    verify { questPatientDetailViewModel.onBackPressed(true) }
  }

  @Test
  @Ignore("Fix assertion after fixing bug for listing questionnaires")
  fun testPatientDetailsCardShouldHaveCorrectData() {
    composeRule.setContent { QuestPatientTestResultScreen(questPatientDetailViewModel) }
    composeRule
      .onNodeWithTag(PATIENT_BIO_INFO)
      .assert(hasAnyChild(hasText("John Doe")))
      .assert(hasAnyChild(hasText("Male - 21y")))
  }
}
