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
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.util.Date
import javax.inject.Inject
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.configuration.view.DataDetailsListViewConfiguration
import org.smartregister.fhircore.quest.configuration.view.dataDetailsListViewConfigurationOf
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.data.patient.model.AdditionalData
import org.smartregister.fhircore.quest.data.patient.model.PatientItem
import org.smartregister.fhircore.quest.data.patient.model.QuestResultItem
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.patient.register.PatientItemMapper

@HiltAndroidTest
class ListDataDetailScreenTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val composeRule = createComposeRule()

  @Inject lateinit var patientItemMapper: PatientItemMapper
  @BindValue
  var configurationRegistry: ConfigurationRegistry =
    Faker.buildTestConfigurationRegistry("g6pd", mockk())
  val application = ApplicationProvider.getApplicationContext<Application>()

  val patientRepository: PatientRepository = mockk()
  val defaultRepository: DefaultRepository = mockk()

  lateinit var questPatientDetailViewModel: ListDataDetailViewModel
  lateinit var patientDetailsViewConfig: DataDetailsListViewConfiguration
  lateinit var fhirEngine: FhirEngine

  private val patientId = "5583145"

  @Before
  fun setUp() {
    hiltRule.inject()
    fhirEngine = mockk()

    Faker.initPatientRepositoryMocks(patientRepository)

    questPatientDetailViewModel =
      spyk(
        ListDataDetailViewModel(
          patientRepository = patientRepository,
          defaultRepository = defaultRepository,
          patientItemMapper = patientItemMapper,
          mockk(),
          fhirEngine
        )
      )

    patientDetailsViewConfig =
      "configs/g6pd/config_patient_details_view.json".readFile(Faker.systemPath).decodeJson()
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
      .assertTextEquals(application.getString(R.string.edit_patient_info))
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
      .assertCountEquals(1)

    composeRule.onNodeWithTag(TOOLBAR_MENU_BUTTON).performClick()
    composeRule.onNodeWithTag(TOOLBAR_MENU).assertDoesNotExist()
  }

  @Test
  fun testToolbarEditInfoMenuItemShouldCallMenuItemClickListener() {
    initMocks()
    composeRule.onNodeWithTag(TOOLBAR_MENU_BUTTON).performClick()
    composeRule.onNodeWithTag(TOOLBAR_MENU).onChildAt(0).performClick()
    verify { questPatientDetailViewModel.onMenuItemClickListener(R.string.edit_patient_info) }
  }

  @Test
  fun testToolbarBackPressedButtonShouldCallBackPressedClickListener() {
    initMocks()
    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).performClick()
    verify { questPatientDetailViewModel.onBackPressed(true) }
  }

  @Test
  fun testPatientDetailsCardShouldHaveCorrectData() {
    initMocks(true)
    composeRule
      .onNodeWithTag(PATIENT_NAME)
      .assertExists()
      .assertIsDisplayed()
      .assertTextEquals("John Doe, M, 22y")

    composeRule.onNodeWithText("G6PD").assertExists().assertIsDisplayed()
    composeRule.onNodeWithText("G6PD Normal").assertExists().assertIsDisplayed()
  }

  @Test
  fun testPatientDetailsCardShouldNotShowAdditionalData() {
    initMocks()
    composeRule
      .onNodeWithTag(PATIENT_NAME)
      .assertExists()
      .assertIsDisplayed()
      .assertTextEquals("John Doe, M, 22y")

    composeRule.onNodeWithText("G6PD").assertDoesNotExist()
    composeRule.onNodeWithText("G6PD Normal").assertDoesNotExist()
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
    composeRule.onAllNodesWithTag(RESULT_ITEM, true)[0].assert(hasAnyChild(hasText("Label")))
    composeRule.onAllNodesWithTag(RESULT_ITEM, true)[0].assert(hasAnyChild(hasText("Sample Order")))
    composeRule.onAllNodesWithTag(RESULT_ITEM, true)[0].assert(
      hasAnyChild(hasText("(${Date().asDdMmmYyyy()})"))
    )
    composeRule.onAllNodesWithTag(RESULT_ITEM, true)[1].assert(hasAnyChild(hasText("Sample Test")))
    composeRule.onAllNodesWithTag(RESULT_ITEM, true)[1].assert(
      hasAnyChild(hasText("(${Date().asDdMmmYyyy()})"))
    )
  }

  @Test
  fun testResultsListItemShouldCallResultItemListener() {
    initMocks()
    val resultItemClicks = mutableListOf<QuestResultItem>()
    every { questPatientDetailViewModel.onTestResultItemClickListener(any()) } answers
      {
        resultItemClicks.add(this.invocation.args[0] as QuestResultItem)
      }

    composeRule.onAllNodesWithTag(RESULT_ITEM).assertCountEquals(2)
    composeRule.onAllNodesWithTag(RESULT_ITEM)[0].performClick()
    composeRule.onAllNodesWithTag(RESULT_ITEM)[1].performClick()

    verify { questPatientDetailViewModel.onTestResultItemClickListener(any()) }

    assertEquals("Sample Order", resultItemClicks[0].data[0][0].value)
    assertEquals("Sample Test", resultItemClicks[1].data[0][0].value)
  }

  @Test
  fun testQuestPatientDetailScreenShouldHandleMissingData() {
    initEmptyMocks()
    composeRule
      .onNodeWithTag(TOOLBAR_TITLE)
      .assertTextEquals(application.getString(R.string.back_to_clients))
    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).assertHasClickAction()
    composeRule.onNodeWithTag(PATIENT_NAME).assertDoesNotExist()
    composeRule.onNodeWithTag(FORM_CONTAINER_ITEM).assert(hasAnyChild(hasText("Loading forms ...")))
    composeRule
      .onNodeWithTag(RESULT_CONTAINER_ITEM)
      .assert(hasAnyChild(hasText("Loading responses ...")))
  }

  private fun initMocks(shouldLoadAdditionalData: Boolean = false) {
    Faker.initPatientRepositoryMocks(patientRepository)
    questPatientDetailViewModel =
      spyk(
        ListDataDetailViewModel(
          patientRepository = patientRepository,
          defaultRepository = defaultRepository,
          patientItemMapper = patientItemMapper,
          mockk(),
          fhirEngine
        )
      )

    if (shouldLoadAdditionalData) {
      coEvery { patientRepository.fetchDemographicsWithAdditionalData(any()) } answers
        {
          PatientItem(
            id = firstArg(),
            name = "John Doe",
            gender = "M",
            age = "22y",
            additionalData = listOf(AdditionalData(label = "G6PD", value = "Normal"))
          )
        }
    }

    // Simulate retrieval of data from repository
    questPatientDetailViewModel.run {
      updateViewConfigurations(dataDetailsListViewConfigurationOf(contentTitle = "RESPONSES"))
      getDemographicsWithAdditionalData(patientId, patientDetailsViewConfig)
      getAllResults(
        patientId,
        ResourceType.Patient,
        patientDetailsViewConfig.questionnaireFilter!!,
        patientDetailsViewConfig
      )
      getAllForms(patientDetailsViewConfig.questionnaireFilter!!)
    }

    composeRule.setContent { QuestPatientDetailScreen(questPatientDetailViewModel) }
  }

  private fun initEmptyMocks() {
    Faker.initPatientRepositoryEmptyMocks(patientRepository)

    questPatientDetailViewModel =
      spyk(
        ListDataDetailViewModel(
          patientRepository = patientRepository,
          defaultRepository = defaultRepository,
          patientItemMapper = patientItemMapper,
          mockk(),
          fhirEngine
        )
      )
    composeRule.setContent { QuestPatientDetailScreen(questPatientDetailViewModel) }
  }
}
