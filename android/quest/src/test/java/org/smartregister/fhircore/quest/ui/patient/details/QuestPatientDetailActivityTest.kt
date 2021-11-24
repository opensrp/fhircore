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

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_FORM
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_READ_ONLY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.quest.QuestApplication
import org.smartregister.fhircore.quest.robolectric.ActivityRobolectricTest

class QuestPatientDetailActivityTest : ActivityRobolectricTest() {

  private lateinit var activity: QuestPatientDetailActivity

  @Before
  fun setUp() {
    activity = Robolectric.buildActivity(QuestPatientDetailActivity::class.java).create().get()
  }

  @Test
  fun testOnBackPressListenerShouldCallFinishActivity() {
    val spyActivity = spyk(activity)
    every { spyActivity.finish() } returns Unit

    ReflectionHelpers.callInstanceMethod<Any>(spyActivity, "onBackPressListener")

    verify(exactly = 1) { spyActivity.finish() }
  }

  @Test
  fun testOnMenuItemClickListenerShouldStartQuestPatientTestResultActivity() {

    ReflectionHelpers.callInstanceMethod<Any>(
      activity,
      "onMenuItemClickListener",
      ReflectionHelpers.ClassParameter(String::class.java, "")
    )

    val expectedIntent = Intent(activity, QuestPatientTestResultActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<QuestApplication>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testOnFormItemClickListenerShouldStartQuestionnaireActivity() {

    ReflectionHelpers.callInstanceMethod<Any>(
      activity,
      "onFormItemClickListener",
      ReflectionHelpers.ClassParameter(
        QuestionnaireConfig::class.java,
        QuestionnaireConfig("quest", "test-form", "Title", "1234")
      )
    )

    val expectedIntent = Intent(activity, QuestionnaireActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<QuestApplication>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testOnTestResultItemClickListenerShouldStartQuestionnaireActivity() {
    ReflectionHelpers.callInstanceMethod<Any>(
      activity,
      "onTestResultItemClickListener",
      ReflectionHelpers.ClassParameter(
        QuestionnaireResponse::class.java,
        QuestionnaireResponse().apply { questionnaire = "Questionnaire/12345" }
      )
    )

    val expectedIntent = Intent(activity, QuestionnaireActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<QuestApplication>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
    Assert.assertEquals("12345", actualIntent.getStringExtra(QUESTIONNAIRE_ARG_FORM))
    Assert.assertEquals(true, actualIntent.getBooleanExtra(QUESTIONNAIRE_READ_ONLY, false))
  }

  override fun getActivity(): Activity {
    return activity
  }
}
