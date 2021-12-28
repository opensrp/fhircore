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
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.util.Date
import javax.inject.Inject
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.patient.register.PatientItemMapper

@HiltAndroidTest
class QuestPatientDetailScreenTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val composeRule = createComposeRule()

  @Inject lateinit var patientItemMapper: PatientItemMapper

  val application = ApplicationProvider.getApplicationContext<Application>()

  val patientRepository: PatientRepository = mockk()

  lateinit var questPatientDetailViewModel: QuestPatientDetailViewModel

  private val patientId = "5583145"

  @Before
  fun setUp() {
    hiltRule.inject()
    Faker.initPatientRepositoryMocks(patientRepository)
    questPatientDetailViewModel =
      spyk(
        QuestPatientDetailViewModel(
          patientRepository = patientRepository,
          patientItemMapper = patientItemMapper,
          mockk()
        )
      )
    // Simulate retrieval of data from repository
    questPatientDetailViewModel.run {
      getDemographics(patientId)
      getAllResults(patientId)
      getAllForms(application)
    }
  }

  @Test
  fun testToolbarComponents() {
    initMocks()
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
    initMocks()
    composeRule.onNodeWithTag(TOOLBAR_MENU).assertDoesNotExist()
    composeRule.onNodeWithTag(TOOLBAR_MENU_BUTTON).performClick()

    composeRule
      .onNodeWithTag(TOOLBAR_MENU)
      .assertExists()
      .assertIsDisplayed()
      .onChildren()
      .assertCountEquals(2)

    composeRule.onNodeWithTag(TOOLBAR_MENU_BUTTON).performClick()
    composeRule.onNodeWithTag(TOOLBAR_MENU).assertDoesNotExist()
  }

  @Test
  fun testToolbarTestResultsMenuItemShouldCallMenuItemClickListener() {
    initMocks()
    composeRule.onNodeWithTag(TOOLBAR_MENU_BUTTON).performClick()
    composeRule.onNodeWithTag(TOOLBAR_MENU).onChildAt(0).performClick()
    verify { questPatientDetailViewModel.onMenuItemClickListener(R.string.test_results) }
  }

  @Test
  fun testToolbarRunCqlMenuItemShouldCallMenuItemClickListener() {
    initMocks()
    composeRule.onNodeWithTag(TOOLBAR_MENU_BUTTON).performClick()
    composeRule.onNodeWithTag(TOOLBAR_MENU).onChildAt(1).performClick()
    verify { questPatientDetailViewModel.onMenuItemClickListener(R.string.run_cql) }
  }

  @Test
  fun testToolbarBackPressedButtonShouldCallBackPressedClickListener() {
    initMocks()
    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).performClick()
    verify { questPatientDetailViewModel.onBackPressed(true) }
  }

  @Test
  fun testPatientDetailsCardShouldHaveCorrectData() {
    initMocks()
    composeRule
      .onNodeWithTag(PATIENT_NAME)
      .assertExists()
      .assertIsDisplayed()
      .assertTextEquals("John Doe, M, 22y")
  }

  @Test
  fun testFormsListShouldDisplayAllFormWithCorrectData() {
    initMocks()
    composeRule.onAllNodesWithTag(FORM_ITEM).assertCountEquals(2)
    composeRule.onAllNodesWithTag(FORM_ITEM)[0].assertTextEquals("SAMPLE ORDER RESULT")
    composeRule.onAllNodesWithTag(FORM_ITEM)[1].assertTextEquals("SAMPLE TEST RESULT")
  }

  @Test
  fun testFormsListItemClickShouldCallFormItemClickListener() {
    initMocks()
    val formItemClicks = mutableListOf<QuestionnaireConfig>()
    every { questPatientDetailViewModel.onFormItemClickListener(any()) } answers
      {
        formItemClicks.add(this.invocation.args[0] as QuestionnaireConfig)
      }

    composeRule.onAllNodesWithTag(FORM_ITEM).assertCountEquals(2)
    composeRule.onAllNodesWithTag(FORM_ITEM)[0].performClick()
    composeRule.onAllNodesWithTag(FORM_ITEM)[1].performClick()

    verify { questPatientDetailViewModel.onFormItemClickListener(any()) }

    assertEquals("SAMPLE ORDER RESULT", formItemClicks[0].title.uppercase())
    assertEquals("SAMPLE TEST RESULT", formItemClicks[1].title.uppercase())
  }

  @Test
  fun testResultsListShouldHaveAllResultsWithCorrectData() {
    initMocks()

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
  fun testResultsListItemShouldCallResultItemListener() {
    initMocks()
    val resultItemClicks = mutableListOf<QuestionnaireResponse>()
    every { questPatientDetailViewModel.onTestResultItemClickListener(any()) } answers
      {
        resultItemClicks.add(this.invocation.args[0] as QuestionnaireResponse)
      }

    composeRule.onAllNodesWithTag(RESULT_ITEM).assertCountEquals(2)
    composeRule.onAllNodesWithTag(RESULT_ITEM)[0].performClick()
    composeRule.onAllNodesWithTag(RESULT_ITEM)[1].performClick()

    verify { questPatientDetailViewModel.onTestResultItemClickListener(any()) }

    assertEquals("Sample Order", resultItemClicks[0].meta.tagFirstRep.display)
    assertEquals("Sample Test", resultItemClicks[1].meta.tagFirstRep.display)
  }

  @Test
  fun testQuestPatientDetailScreenShouldHandleMissingData() {
    initEmptyMocks()
    composeRule
      .onNodeWithTag(TOOLBAR_TITLE)
      .assertTextEquals(application.getString(R.string.back_to_clients))
    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).assertHasClickAction()
    composeRule.onNodeWithTag(PATIENT_NAME).assertExists().assertTextEquals(", , ")
    composeRule.onNodeWithTag(FORM_CONTAINER_ITEM).assert(hasAnyChild(hasText("Loading forms ...")))
    composeRule
      .onNodeWithTag(RESULT_CONTAINER_ITEM)
      .assert(hasAnyChild(hasText("Loading responses ...")))
  }

  private fun initMocks() {
    Faker.initPatientRepositoryMocks(patientRepository)
    questPatientDetailViewModel =
      spyk(
        QuestPatientDetailViewModel(
          patientRepository = patientRepository,
          patientItemMapper = patientItemMapper,
          mockk()
        )
      )
    // Simulate retrieval of data from repository
    questPatientDetailViewModel.run {
      getDemographics(patientId)
      getAllResults(patientId)
      getAllForms(application)
    }

    composeRule.setContent { QuestPatientDetailScreen(questPatientDetailViewModel) }
  }

  private fun initEmptyMocks() {
    Faker.initPatientRepositoryEmptyMocks(patientRepository)

    questPatientDetailViewModel =
      spyk(
        QuestPatientDetailViewModel(
          patientRepository = patientRepository,
          patientItemMapper = patientItemMapper,
          mockk()
        )
      )
    composeRule.setContent { QuestPatientDetailScreen(questPatientDetailViewModel) }
  }
}
