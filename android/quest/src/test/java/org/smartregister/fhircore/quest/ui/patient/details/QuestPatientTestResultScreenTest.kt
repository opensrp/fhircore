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

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.patient.register.PatientItemMapper

@HiltAndroidTest
class QuestPatientTestResultScreenTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val composeRule = createComposeRule()

  @Inject lateinit var patientItemMapper: PatientItemMapper

  val patientRepository: PatientRepository = mockk()

  private lateinit var questPatientDetailViewModel: QuestPatientDetailViewModel

  private val patientId = "5583145"

  @Before
  fun setUp() {
    hiltRule.inject()
    Faker.initPatientRepositoryMocks(patientRepository)
    questPatientDetailViewModel =
      spyk(
        QuestPatientDetailViewModel(
          patientRepository = patientRepository,
          patientItemMapper = patientItemMapper
        )
      )
    questPatientDetailViewModel.getDemographics(patientId)

    composeRule.setContent { QuestPatientTestResultScreen(questPatientDetailViewModel) }
  }

  @Test
  fun testToolbarComponents() {
    composeRule
      .onNodeWithTag(TOOLBAR_TITLE)
      .assertTextEquals(
        ApplicationProvider.getApplicationContext<HiltTestApplication>()
          .getString(R.string.back_to_clients)
      )
    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).assertHasClickAction()
  }

  @Test
  fun testToolbarBackPressedButtonShouldCallBackPressedClickListener() {
    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).performClick()
    verify { questPatientDetailViewModel.onBackPressed(true) }
  }

  @Test
  fun testPatientDetailsCardShouldHaveCorrectData() {
    composeRule.onNodeWithText("John Doe").assertExists()
    composeRule.onNodeWithText("M - 21y").assertExists()
  }
}
