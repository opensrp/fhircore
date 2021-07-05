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

package org.smartregister.fhircore.activity

import android.app.Activity
import android.content.Intent
import android.view.MenuItem
import android.widget.Button
import androidx.test.core.app.ApplicationProvider
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboMenuItem
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.fragment.PatientDetailFragment
import org.smartregister.fhircore.shadow.FhirApplicationShadow

@Config(shadows = [FhirApplicationShadow::class])
class PatientDetailActivityTest : ActivityRobolectricTest() {

  private lateinit var patientDetailActivity: PatientDetailActivity

  @Before
  fun setUp() {
    patientDetailActivity =
      Robolectric.buildActivity(PatientDetailActivity::class.java, null).create().resume().get()
  }

  @Test
  fun testPatientActivityShouldNotNull() {
    Assert.assertNotNull(patientDetailActivity)
  }

  @Test
  fun testOnOptionsItemSelectedShouldCallEditPatientInfo() {

    val fragment = mockk<PatientDetailFragment>(relaxed = true)
    patientDetailActivity.fragment = fragment

    val menuItem: MenuItem = RoboMenuItem(R.id.patient_profile_edit)
    patientDetailActivity.onOptionsItemSelected(menuItem)

    verify { fragment.editPatient() }

    confirmVerified(fragment)
  }

  @Test
  fun testVerifyStartedRecordVaccineActivityComponent() {
    patientDetailActivity.findViewById<Button>(R.id.btn_record_vaccine).performClick()

    val expectedIntent = Intent(patientDetailActivity, RecordVaccineActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<FhirApplication>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  override fun getActivity(): Activity {
    return patientDetailActivity
  }
}
