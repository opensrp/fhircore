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

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import java.util.Date
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class QuestPatientDetailScreenTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val composeRule = createComposeRule()

  val application = ApplicationProvider.getApplicationContext<HiltTestApplication>()

  @BindValue val patientRepository: PatientRepository = Faker.patientRepository

  lateinit var questPatientDetailViewModel: QuestPatientDetailViewModel

  @Before
  fun setUp() {
    hiltRule.inject()
    questPatientDetailViewModel = QuestPatientDetailViewModel(patientRepository)

    // Simulate retrieval of data from repository
    questPatientDetailViewModel.run {
      val patientId = "5583145"
      getDemographics(patientId)
      getAllResults(patientId)
      getAllForms(application)
    }
  }

  @Test
  fun testToolbarComponents() {
    composeRule.setContent { QuestPatientDetailScreen(questPatientDetailViewModel) }

    composeRule
      .onNodeWithTag(TOOLBAR_TITLE)
      .assertTextEquals(application.getString(R.string.back_to_clients))

    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).assertHasClickAction()
    composeRule.onNodeWithTag(TOOLBAR_MENU_BUTTON).assertHasClickAction().performClick()
    composeRule.onNodeWithTag(TOOLBAR_MENU).assertIsDisplayed()

    composeRule
      .onNodeWithTag(TOOLBAR_MENU)
      .onChildAt(0)
      .assertTextEquals(application.getString(R.string.test_results))
      .assertHasClickAction()
  }

  @Test
  fun testToolbarMenuButtonShouldToggleMenuItemsList() {
    composeRule.setContent { QuestPatientDetailScreen(questPatientDetailViewModel) }

    composeRule.onNodeWithTag(TOOLBAR_MENU).assertDoesNotExist()
    composeRule.onNodeWithTag(TOOLBAR_MENU_BUTTON).performClick()

    composeRule
      .onNodeWithTag(TOOLBAR_MENU)
      .assertExists()
      .assertIsDisplayed()
      .onChildren()
      .assertCountEquals(1)

    composeRule.onNodeWithTag(TOOLBAR_MENU_BUTTON).performClick()
    composeRule.onNodeWithTag(TOOLBAR_MENU).assertDoesNotExist()
  }

  @Test
  fun testToolbarTestResultsMenuItemShouldCallMenuItemClickListener() {
    val questPatientDetailViewModelSpy = spyk(questPatientDetailViewModel)

    composeRule.setContent { QuestPatientDetailScreen(questPatientDetailViewModelSpy) }

    composeRule.onNodeWithTag(TOOLBAR_MENU_BUTTON).performClick()

    composeRule.onNodeWithTag(TOOLBAR_MENU).onChildAt(0).performClick()

    verify { questPatientDetailViewModelSpy.onMenuItemClickListener(true) }
  }

  @Test
  fun testToolbarBackPressedButtonShouldCallBackPressedClickListener() {
    val questPatientDetailViewModelSpy = spyk(questPatientDetailViewModel)

    composeRule.setContent { QuestPatientDetailScreen(questPatientDetailViewModelSpy) }

    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).performClick()

    verify { questPatientDetailViewModelSpy.onBackPressed(true) }
  }

  @Test
  fun testPatientDetailsCardShouldHaveCorrectData() {
    composeRule.setContent { QuestPatientDetailScreen(questPatientDetailViewModel) }

    composeRule
      .onNodeWithTag(PATIENT_NAME)
      .assertExists()
      .assertIsDisplayed()
      .assertTextEquals("John Doe, M, 21y")
  }

  @Test
  fun testFormsListShouldDisplayAllFormWithCorrectData() {
    composeRule.setContent { QuestPatientDetailScreen(questPatientDetailViewModel) }

    composeRule.onAllNodesWithTag(FORM_ITEM).assertCountEquals(2)
    composeRule.onAllNodesWithTag(FORM_ITEM)[0].assertTextEquals("SAMPLE ORDER RESULT")
    composeRule.onAllNodesWithTag(FORM_ITEM)[1].assertTextEquals("SAMPLE TEST RESULT")
  }

  @Test
  fun testFormsListItemClickShouldCallFormItemClickListener() {
    val formItemClicks = mutableListOf<QuestionnaireConfig>()
    val questPatientDetailViewModelSpy = spyk(questPatientDetailViewModel)
    every { questPatientDetailViewModelSpy.onFormItemClickListener(any()) } answers
      {
        formItemClicks.add(this.invocation.args[0] as QuestionnaireConfig)
      }

    composeRule.setContent { QuestPatientDetailScreen(questPatientDetailViewModelSpy) }

    composeRule.onAllNodesWithTag(FORM_ITEM).assertCountEquals(2)
    composeRule.onAllNodesWithTag(FORM_ITEM)[0].performClick()
    composeRule.onAllNodesWithTag(FORM_ITEM)[1].performClick()

    verify { questPatientDetailViewModelSpy.onFormItemClickListener(any()) }

    assertEquals("SAMPLE ORDER RESULT", formItemClicks[0].title.uppercase())
    assertEquals("SAMPLE TEST RESULT", formItemClicks[1].title.uppercase())
  }

  @Test
  @Ignore("Fix assertions once questionnaire listing bug is resolved")
  fun testResultsListShouldHaveAllResultsWithCorrectData() {
    composeRule.setContent { QuestPatientDetailScreen(questPatientDetailViewModel) }

    // response heading should be exist and must be displayed
    composeRule.onNodeWithText("RESPONSES (2)").assertExists().assertIsDisplayed()

    // verify test result item(s) count and item title
    composeRule.onAllNodesWithTag(RESULT_ITEM).assertCountEquals(2)
    composeRule.onAllNodesWithTag(RESULT_ITEM, true)[0].assert(
      hasAnyChild(hasText("Sample Order (${Date().asDdMmmYyyy()}) "))
    )
    composeRule.onAllNodesWithTag(RESULT_ITEM, true)[1].assert(
      hasAnyChild(hasText("Sample Test (${Date().asDdMmmYyyy()}) "))
    )
  }

  @Test
  @Ignore("Fix assertions once questionnaire listing bug is resolved")
  fun testResultsListItemShouldCallResultItemListener() {
    val resultItemClicks = mutableListOf<QuestionnaireResponse>()
    val questPatientDetailViewModelSpy = spyk(questPatientDetailViewModel)
    every { questPatientDetailViewModelSpy.onTestResultItemClickListener(any()) } answers
      {
        resultItemClicks.add(this.invocation.args[0] as QuestionnaireResponse)
      }

    composeRule.setContent { QuestPatientDetailScreen(questPatientDetailViewModelSpy) }

    composeRule.onAllNodesWithTag(RESULT_ITEM).assertCountEquals(2)
    composeRule.onAllNodesWithTag(RESULT_ITEM)[0].performClick()
    composeRule.onAllNodesWithTag(RESULT_ITEM)[1].performClick()

    verify { questPatientDetailViewModelSpy.onTestResultItemClickListener(any()) }

    assertEquals("Sample Order", resultItemClicks[0].meta.tagFirstRep.display)
    assertEquals("Sample Test", resultItemClicks[1].meta.tagFirstRep.display)
  }

  @Test
  @Ignore("Fix assertions once questionnaire listing bug is resolved")
  fun testQuestPatientDetailScreenShouldHandleMissingData() {
    composeRule.setContent { QuestPatientDetailScreen(questPatientDetailViewModel) }
    composeRule
      .onNodeWithTag(TOOLBAR_TITLE)
      .assertTextEquals(application.getString(R.string.back_to_clients))
    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).assertHasClickAction()

    composeRule.onNodeWithTag(PATIENT_NAME).assertExists().assertTextEquals("")

    composeRule.onNodeWithTag(FORM_CONTAINER_ITEM).assert(hasAnyChild(hasText("Loading forms ...")))
    composeRule
      .onNodeWithTag(RESULT_CONTAINER_ITEM)
      .assert(hasAnyChild(hasText("Loading responses ...")))
  }
}
