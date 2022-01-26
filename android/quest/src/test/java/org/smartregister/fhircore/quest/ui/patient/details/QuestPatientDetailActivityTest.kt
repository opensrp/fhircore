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
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.mockk
import javax.inject.Inject
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_FORM
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_READ_ONLY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.configuration.parser.G6PDDetailConfigParser
import org.smartregister.fhircore.quest.configuration.parser.QuestDetailConfigParser
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.data.patient.model.QuestResultItem
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class QuestPatientDetailActivityTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @BindValue val patientRepository: PatientRepository = mockk()
  @BindValue val libraryEvaluator: LibraryEvaluator = mockk()

  @Inject lateinit var accountAuthenticator: AccountAuthenticator
  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  private val hiltTestApplication = ApplicationProvider.getApplicationContext<HiltTestApplication>()

  private lateinit var questPatientDetailActivity: QuestPatientDetailActivity

  private lateinit var questPatientDetailActivityController:
    ActivityController<QuestPatientDetailActivity>

  @Before
  fun setUp() {
    hiltRule.inject()
    configurationRegistry.loadAppConfigurations("g6pd", accountAuthenticator) {}
    Faker.initPatientRepositoryMocks(patientRepository)

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
    questPatientDetailActivity.configurationRegistry.appId = "quest"
    questPatientDetailActivity.configurationRegistry.configurationsMap.put(
      "quest|patient_register",
      RegisterViewConfiguration("", "", "", "", "", "", "")
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

    ReflectionHelpers.setField(
      questPatientDetailActivity,
      "parser",
      QuestDetailConfigParser(mockk())
    )

    ReflectionHelpers.callInstanceMethod<Any>(
      questPatientDetailActivity,
      "onTestResultItemClickListener",
      ReflectionHelpers.ClassParameter(
        QuestResultItem::class.java,
        QuestResultItem(
          Pair(QuestionnaireResponse().apply { questionnaire = "Questionnaire/12345" }, mockk()),
          listOf()
        )
      )
    )

    val expectedIntent = Intent(questPatientDetailActivity, QuestionnaireActivity::class.java)
    val actualIntent = shadowOf(hiltTestApplication).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
    Assert.assertEquals("12345", actualIntent.getStringExtra(QUESTIONNAIRE_ARG_FORM))
    Assert.assertEquals(true, actualIntent.getBooleanExtra(QUESTIONNAIRE_READ_ONLY, false))
  }

  @Test
  fun testOnTestResultItemClickListenerShouldStartSimpleDetailsActivityForG6pd() {
    ReflectionHelpers.setField(
      questPatientDetailActivity,
      "parser",
      G6PDDetailConfigParser(mockk())
    )

    ReflectionHelpers.callInstanceMethod<Any>(
      questPatientDetailActivity,
      "onTestResultItemClickListener",
      ReflectionHelpers.ClassParameter(
        QuestResultItem::class.java,
        QuestResultItem(
          Pair(QuestionnaireResponse().apply { questionnaire = "Questionnaire/12345" }, mockk()),
          listOf()
        )
      )
    )
    8
    val expectedIntent = Intent(questPatientDetailActivity, SimpleDetailsActivity::class.java)
    val actualIntent = shadowOf(hiltTestApplication).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }
}
