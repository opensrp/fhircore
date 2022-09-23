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

package org.smartregister.fhircore.eir.ui.patient.details

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.activity.ActivityRobolectricTest
import org.smartregister.fhircore.eir.ui.adverseevent.AdverseEventActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
/*
@HiltAndroidTest
internal class PatientDetailsActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private val patientId = "samplePatientId"

  private lateinit var patientDetailsActivity: PatientDetailsActivity

  @Before
  fun setUp() {
    hiltRule.inject()
    patientDetailsActivity =
      spyk(
        Robolectric.buildActivity(
            PatientDetailsActivity::class.java,
            Intent().putExtras(bundleOf(Pair(QUESTIONNAIRE_ARG_PATIENT_KEY, patientId)))
          )
          .create()
          .get()
      )
  }

  @Test
  fun testPatientProfileMenuOptionClick() {
    val activityShadow = Shadows.shadowOf(patientDetailsActivity)
    activityShadow.clickMenuItem(R.id.patient_profile_edit)
    val expectedIntent = Intent(patientDetailsActivity, QuestionnaireActivity::class.java)
    val actualIntent = Shadows.shadowOf(application).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testShouldLaunchThePatientDetailsFragment() {
    Assert.assertFalse(patientDetailsActivity.supportFragmentManager.fragments.isEmpty())
    val patientDetailsFragment = patientDetailsActivity.supportFragmentManager.fragments.first()
    Assert.assertTrue(patientDetailsFragment is PatientDetailsFragment)
    Assert.assertNotNull(patientDetailsFragment.arguments)
    Assert.assertTrue(patientDetailsFragment.arguments!!.containsKey(QUESTIONNAIRE_ARG_PATIENT_KEY))
    Assert.assertEquals(
      patientId,
      patientDetailsFragment.arguments!!.get(QUESTIONNAIRE_ARG_PATIENT_KEY)
    )
  }

  @Test
  fun testAdverseEventMenuOptionClick() {
    val activityShadow = Shadows.shadowOf(patientDetailsActivity)
    activityShadow.clickMenuItem(R.id.vaccine_adverse_events)
    val expectedIntent = Intent(patientDetailsActivity, AdverseEventActivity::class.java)
    val actualIntent = Shadows.shadowOf(application).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  override fun getActivity(): Activity {
    return patientDetailsActivity
  }
}*/
