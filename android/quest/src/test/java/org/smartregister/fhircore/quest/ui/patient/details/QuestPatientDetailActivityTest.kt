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
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
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
import org.robolectric.shadows.ShadowToast
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_FORM
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_READ_ONLY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class QuestPatientDetailActivityTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @BindValue val patientRepository: PatientRepository = mockk()
  @BindValue val libraryEvaluator: LibraryEvaluator = mockk()

  private val hiltTestApplication = ApplicationProvider.getApplicationContext<HiltTestApplication>()

  private lateinit var questPatientDetailActivity: QuestPatientDetailActivity

  private lateinit var questPatientDetailActivityController:
    ActivityController<QuestPatientDetailActivity>

  @Before
  fun setUp() {
    hiltRule.inject()
    Faker.initPatientRepositoryMocks(patientRepository)

    val intent =
      Intent().apply { this.putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, "123") }
    questPatientDetailActivityController =
      Robolectric.buildActivity(QuestPatientDetailActivity::class.java, intent)
    questPatientDetailActivity = spyk(questPatientDetailActivityController.create().resume().get())
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
  fun testOnMenuItemClickListenerShouldStartQuestPatientTestResultActivity() {
    questPatientDetailActivity.patientViewModel.onMenuItemClickListener(R.string.test_results)
    val expectedIntent = Intent(questPatientDetailActivity, SimpleDetailsActivity::class.java)
    val actualIntent = shadowOf(hiltTestApplication).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
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
  fun testOnMenuItemClickListenerShouldShowProgressAlert() = runBlockingTest {
    Assert.assertNull(ShadowAlertDialog.getLatestAlertDialog())

    val fhirEngineMockk = mockk<FhirEngine>()
    every { patientRepository.fhirEngine } returns fhirEngineMockk
    coEvery { fhirEngineMockk.load(Patient::class.java, any()) } returns
      Patient().apply { id = "123" }
    coEvery { fhirEngineMockk.load(Library::class.java, any()) } returns Library()
    coEvery { fhirEngineMockk.search<Condition>(any()) } returns listOf()
    coEvery { fhirEngineMockk.search<Observation>(any()) } returns listOf()
    coEvery { libraryEvaluator.runCqlLibrary(any(), any(), any(), any()) } returns listOf("1", "2")

    questPatientDetailActivity.patientViewModel.onMenuItemClickListener(R.string.run_cql)

    Assert.assertNotNull(ShadowAlertDialog.getLatestAlertDialog())

    coVerify { libraryEvaluator.runCqlLibrary(any(), any(), any(), any()) }

    val lastAlert = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    Assert.assertNotNull(
      lastAlert.view.findViewById<TextView>(
          org.smartregister.fhircore.engine.R.id.tv_alert_message
        )!!
        .text
    )

    Assert.assertEquals(
      View.GONE,
      lastAlert.view.findViewById<View>(org.smartregister.fhircore.engine.R.id.pr_circular)!!
        .visibility
    )
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
    questPatientDetailActivity.configurationRegistry.appId = "quest"
    questPatientDetailActivity.configurationRegistry.configurationsMap.put(
      "quest|patient_register",
      RegisterViewConfiguration("", "", "", "", "", "", "")
    )

    questPatientDetailActivity.patientViewModel.onTestResultItemClickListener(
      QuestionnaireResponse().apply { questionnaire = "Questionnaire/12345" }
    )

    val expectedIntent = Intent(questPatientDetailActivity, QuestionnaireActivity::class.java)
    val actualIntent = shadowOf(hiltTestApplication).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
    Assert.assertEquals("12345", actualIntent.getStringExtra(QUESTIONNAIRE_ARG_FORM))
    Assert.assertEquals(true, actualIntent.getBooleanExtra(QUESTIONNAIRE_READ_ONLY, false))
  }

  // TODO https://github.com/opensrp/fhircore/issues/778
  @Test
  fun testOnTestResultItemClickListenerShouldStartSimpleDetailsActivityForG6pd() {
    questPatientDetailActivity.configurationRegistry.appId = "g6pd"
    questPatientDetailActivity.configurationRegistry.configurationsMap.put(
      "g6pd|patient_register",
      RegisterViewConfiguration("g6pd", "", "", "", "", "", "")
    )

    questPatientDetailActivity.patientViewModel.onTestResultItemClickListener(
      QuestionnaireResponse().apply {
        questionnaire = "Questionnaire/12345"
        contained.add(Encounter().apply { id = "12345" })
      }
    )

    val expectedIntent = Intent(questPatientDetailActivity, SimpleDetailsActivity::class.java)
    val actualIntent = shadowOf(hiltTestApplication).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testOnTestResultItemClickListenerWithNullResponseShouldDisplayToast() {
    questPatientDetailActivity.patientViewModel.onTestResultItemClickListener(
      QuestionnaireResponse().apply { questionnaire = null }
    )
    val latestToast = ShadowToast.getLatestToast()
    Assert.assertEquals(Toast.LENGTH_LONG, latestToast.duration)
  }
}
