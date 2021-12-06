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
import android.widget.Toast
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.mockk
import io.mockk.spyk
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowToast
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_FORM
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_READ_ONLY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class QuestPatientDetailActivityTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @BindValue val patientRepository: PatientRepository = mockk()

  private val hiltTestApplication = ApplicationProvider.getApplicationContext<HiltTestApplication>()

  private lateinit var questPatientDetailActivity: QuestPatientDetailActivity

  private lateinit var questPatientDetailActivityController:
    ActivityController<QuestPatientDetailActivity>

  @Before
  fun setUp() {
    hiltRule.inject()
    Faker.initPatientRepositoryMocks(patientRepository)
    questPatientDetailActivityController =
      Robolectric.buildActivity(QuestPatientDetailActivity::class.java)
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
    questPatientDetailActivity.patientViewModel.onMenuItemClickListener(true)
    val expectedIntent =
      Intent(questPatientDetailActivity, QuestPatientTestResultActivity::class.java)
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
      QuestionnaireResponse().apply { questionnaire = "Questionnaire/12345" }
    )

    val expectedIntent = Intent(questPatientDetailActivity, QuestionnaireActivity::class.java)
    val actualIntent = shadowOf(hiltTestApplication).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
    Assert.assertEquals("12345", actualIntent.getStringExtra(QUESTIONNAIRE_ARG_FORM))
    Assert.assertEquals(true, actualIntent.getBooleanExtra(QUESTIONNAIRE_READ_ONLY, false))
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
