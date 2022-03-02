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

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import java.util.Date
import javax.inject.Inject
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.NavigationOption
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_FORM
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_TYPE
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.configuration.view.DataDetailsListViewConfiguration
import org.smartregister.fhircore.quest.configuration.view.ResultDetailsNavigationConfiguration
import org.smartregister.fhircore.quest.configuration.view.TestDetailsNavigationAction
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.data.patient.model.AdditionalData
import org.smartregister.fhircore.quest.data.patient.model.QuestResultItem
import org.smartregister.fhircore.quest.data.patient.model.QuestionnaireItem
import org.smartregister.fhircore.quest.data.patient.model.QuestionnaireResponseItem
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.patient.register.PatientItemMapper
import org.smartregister.fhircore.quest.util.QuestConfigClassification
import org.smartregister.fhircore.quest.util.QuestJsonSpecificationProvider

@HiltAndroidTest
class QuestPatientDetailActivityTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @BindValue val patientRepository: PatientRepository = mockk()
  @BindValue val libraryEvaluator: LibraryEvaluator = mockk()
  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk()
  @BindValue val secureSharedPreference: SecureSharedPreference = mockk()

  @BindValue var configurationRegistry: ConfigurationRegistry = mockk()
  @Inject lateinit var patientItemMapper: PatientItemMapper
  @Inject lateinit var questJsonSpecificationProvider: QuestJsonSpecificationProvider

  val defaultRepository: DefaultRepository = mockk()
  lateinit var questPatientDetailViewModel: ListDataDetailViewModel
  lateinit var fhirEngine: FhirEngine

  private val hiltTestApplication = ApplicationProvider.getApplicationContext<HiltTestApplication>()

  private lateinit var questPatientDetailActivity: QuestPatientDetailActivity

  private lateinit var questPatientDetailActivityController:
    ActivityController<QuestPatientDetailActivity>

  @Before
  fun setUp() {
    hiltRule.inject()

    every { sharedPreferencesHelper.read(any(), any<String>()) } returns ""
    fhirEngine = mockk()

    every { configurationRegistry.isAppIdInitialized() } returns true

    Faker.initConfigurationRegistry<ResultDetailsNavigationConfiguration>(
      configurationRegistry,
      questJsonSpecificationProvider,
      QuestConfigClassification.RESULT_DETAILS_NAVIGATION,
      "configs/quest/config_result_details_navigation.json".readFile()
    )

    Faker.initConfigurationRegistry<DataDetailsListViewConfiguration>(
      configurationRegistry,
      questJsonSpecificationProvider,
      QuestConfigClassification.PATIENT_DETAILS_VIEW,
      "configs/quest/config_patient_details_view.json".readFile()
    )

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

    val intent =
      Intent().apply { this.putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, "123") }
    questPatientDetailActivityController =
      Robolectric.buildActivity(QuestPatientDetailActivity::class.java, intent)
    questPatientDetailActivity = questPatientDetailActivityController.create().resume().get()
  }

  @After
  override fun tearDown() {
    super.tearDown()
    questPatientDetailActivityController.pause().stop().destroy()
  }

  @Test
  fun testOnBackPressListenerShouldFinishActivity() {
    questPatientDetailActivity.patientViewModel.onBackPressed(true)
    Assert.assertTrue(questPatientDetailActivity.isFinishing)
  }

  @Test
  fun testOnMenuItemClickListenerShouldStartQuestionnaireActivity() {
    Faker.initConfigurationRegistry<RegisterViewConfiguration>(
      configurationRegistry,
      questJsonSpecificationProvider,
      QuestConfigClassification.PATIENT_REGISTER,
      "configs/quest/config_register_view.json".readFile()
    )

    questPatientDetailActivity.patientViewModel.onMenuItemClickListener(R.string.edit_patient_info)

    val expectedIntent = Intent(questPatientDetailActivity, QuestionnaireActivity::class.java)
    val actualIntent = shadowOf(hiltTestApplication).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testOnFormItemClickListenerShouldStartQuestionnaireActivity() {
    questPatientDetailActivity.patientViewModel.onFormItemClickListener(
      QuestionnaireConfig(appId = "quest", form = "test-form", title = "Title", identifier = "1234")
    )

    val expectedIntent = Intent(questPatientDetailActivity, QuestionnaireActivity::class.java)
    val actualIntent = shadowOf(hiltTestApplication).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testOnTestResultItemClickListenerShouldStartQuestionnaireActivity() {
    questPatientDetailActivity.patientViewModel.onTestResultItemClickListener(
      QuestResultItem(
        Pair(
          QuestionnaireResponseItem(
            "12345",
            Date(),
            "12345",
            QuestionnaireResponse()
              .apply {
                this.id = "12345"
                this.authored = Date()
                this.contained = listOf(Encounter().apply { this.id = "12345" })
              }
              .encodeResourceToString()
          ),
          QuestionnaireItem("12345", "name", "title")
        ),
        listOf(listOf(AdditionalData("", "", "", "", "", null)))
      )
    )

    val expectedIntent = Intent(questPatientDetailActivity, QuestionnaireActivity::class.java)
    val actualIntent = shadowOf(hiltTestApplication).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
    Assert.assertEquals("12345", actualIntent.getStringExtra(QUESTIONNAIRE_ARG_FORM))
    Assert.assertEquals(
      QuestionnaireType.READ_ONLY.name,
      actualIntent.getStringExtra(QUESTIONNAIRE_ARG_TYPE)
    )
  }

  @Test
  fun testOnTestResultItemClickListenerEmptyQuestionnaireIdShouldShowAlertDialog() {
    val navigationOptions =
      listOf(
        NavigationOption(
          id = "open_questionnaire",
          title = "Questionnaire",
          icon = "",
          TestDetailsNavigationAction(form = "", readOnly = true)
        )
      )
    ResultDetailsNavigationConfiguration(
      appId = "quest",
      classification = "result_details_navigation",
      navigationOptions
    )

    ReflectionHelpers.callInstanceMethod<Any>(
      questPatientDetailActivity,
      "onTestResultItemClickListener",
      ReflectionHelpers.ClassParameter(
        QuestResultItem::class.java,
        QuestResultItem(
          Pair(
            QuestionnaireResponseItem("", Date(), "12345", ""),
            QuestionnaireItem("", "name", "title")
          ),
          listOf()
        )
      )
    )

    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    Assert.assertNotNull(dialog)
  }

  @Test
  fun testOnTestResultItemClickListenerShouldStartSimpleDetailsActivityForG6pd() {
    Faker.initConfigurationRegistry<ResultDetailsNavigationConfiguration>(
      configurationRegistry,
      questJsonSpecificationProvider,
      QuestConfigClassification.RESULT_DETAILS_NAVIGATION,
      "configs/g6pd/config_result_details_navigation.json".readFile()
    )
    val navigationOptions =
      listOf(
        NavigationOption(
          id = "open_test_details",
          title = "Test Details",
          icon = "",
          TestDetailsNavigationAction(form = "", readOnly = true)
        )
      )
    ResultDetailsNavigationConfiguration(
      appId = "g6pd",
      classification = "result_details_navigation",
      navigationOptions
    )

    ReflectionHelpers.callInstanceMethod<Any>(
      questPatientDetailActivity,
      "onTestResultItemClickListener",
      ReflectionHelpers.ClassParameter(
        QuestResultItem::class.java,
        QuestResultItem(
          Pair(
            QuestionnaireResponseItem("12345", Date(), "12345", ""),
            QuestionnaireItem("1", "name", "title")
          ),
          listOf()
        )
      )
    )

    val expectedIntent = Intent(questPatientDetailActivity, SimpleDetailsActivity::class.java)
    val actualIntent = shadowOf(hiltTestApplication).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testHandlePatientResources() {
    Faker.initConfigurationRegistry<ResultDetailsNavigationConfiguration>(
      configurationRegistry,
      questJsonSpecificationProvider,
      QuestConfigClassification.RESULT_DETAILS_NAVIGATION,
      "configs/g6pd/config_result_details_navigation.json".readFile()
    )
    val navigationOptions =
      listOf(
        NavigationOption(
          id = "open_test_details",
          title = "Test Details",
          icon = "",
          TestDetailsNavigationAction(form = "", readOnly = true)
        )
      )
    ResultDetailsNavigationConfiguration(
      appId = "g6pd",
      classification = "result_details_navigation",
      navigationOptions
    )

    ReflectionHelpers.callInstanceMethod<Any>(
      questPatientDetailActivity,
      "handlePatientResources",
      ReflectionHelpers.ClassParameter(ArrayList::class.java, arrayListOf("Condition"))
    )

    Assert.assertEquals("Condition", questPatientDetailActivity.patientResourcesList[0])
  }
}
