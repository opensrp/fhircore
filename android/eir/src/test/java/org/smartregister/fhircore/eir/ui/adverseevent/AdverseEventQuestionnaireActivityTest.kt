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

package org.smartregister.fhircore.eir.ui.adverseevent

import android.app.Activity
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.activity.ActivityRobolectricTest
import org.smartregister.fhircore.eir.coroutine.CoroutineTestRule
import org.smartregister.fhircore.eir.util.ADVERSE_EVENT_FORM
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity

/*@ExperimentalCoroutinesApi
@HiltAndroidTest
class AdverseEventQuestionnaireActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) var instantTaskExecutorRule = InstantTaskExecutorRule()
  @get:Rule(order = 2) var coroutinesTestRule = CoroutineTestRule()

  private lateinit var activity: AdverseEventQuestionnaireActivity

  @Before
  fun setUp() {
    hiltRule.inject()

    val intent =
      Intent().apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM, ADVERSE_EVENT_FORM)
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, "Patient/1")
        putExtra(QuestionnaireActivity.ADVERSE_EVENT_IMMUNIZATION_ITEM_KEY, "Immunization/1")
      }

    activity =
      Robolectric.buildActivity(AdverseEventQuestionnaireActivity::class.java, intent)
        .create()
        .get()
  }

  @Test
  fun testActivityShouldNotNull() {
    Assert.assertNotNull(activity)
  }

  @Test
  fun handleExtractionErrorShouldShowErrorDialog() {
    ReflectionHelpers.callInstanceMethod<Any>(activity, "handleExtractionError")
    val dialog = Shadows.shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    Assert.assertEquals(
      activity.getString(R.string.error_reading_immunization_details),
      dialog.title
    )
    Assert.assertEquals(
      activity.getString(R.string.kindly_retry_contact_devs_problem_persists),
      dialog.message
    )
    Assert.assertTrue(dialog.isCancelable)
  }

  override fun getActivity(): Activity {
    return activity
  }
}*/
