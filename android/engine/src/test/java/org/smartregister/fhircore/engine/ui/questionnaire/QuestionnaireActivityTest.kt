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

package org.smartregister.fhircore.engine.ui.questionnaire

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import org.hl7.fhir.r4.model.Questionnaire
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.engine.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.FormConfigUtil
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

class QuestionnaireActivityTest : ActivityRobolectricTest() {
  private lateinit var context: Application
  private lateinit var questionnaireActivity: QuestionnaireActivity
  private lateinit var questionnaireViewModel: QuestionnaireViewModel
  private lateinit var intent: Intent

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()
  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @Before
  fun setUp() {
    mockkObject(SharedPreferencesHelper)
    every { SharedPreferencesHelper.read(any(), any()) } returns ""

    mockkObject(FormConfigUtil)
    every { FormConfigUtil.loadConfig(any(), any()) } returns
      listOf(QuestionnaireConfig("patient-registration", "Patient registration", "1234567"))

    context = ApplicationProvider.getApplicationContext()
    questionnaireViewModel = spyk(QuestionnaireViewModel(context))

    coEvery { questionnaireViewModel.loadQuestionnaire("1234567") } returns
      Questionnaire().apply { id = "1234567" }

    intent =
      Intent().apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY, "Patient registration")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM, "patient-registration")
      }

    val controller = Robolectric.buildActivity(QuestionnaireActivity::class.java, intent)
    questionnaireActivity = controller.create().resume().get()
    questionnaireActivity.questionnaireViewModel = questionnaireViewModel
    questionnaireActivity.supportFragmentManager.executePendingTransactions()
  }

  @After
  fun cleanup() {
    unmockkObject(SharedPreferencesHelper)
    unmockkObject(FormConfigUtil)
  }

  @Test
  fun testActivityShouldNotNull() {
    Assert.assertNotNull(questionnaireActivity)
  }

  @Test
  fun testRequiredIntentShouldInsertValues() {
    val result = QuestionnaireActivity.requiredIntentArgs("1234", "my-form", "quest-id")
    Assert.assertEquals("my-form", result.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM))
    Assert.assertEquals(
      "1234",
      result.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY)
    )
    Assert.assertEquals("quest-id", result.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_ID))
  }

  override fun getActivity(): Activity {
    return questionnaireActivity
  }
}
