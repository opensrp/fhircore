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
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.robolectric.ActivityRobolectricTest

@HiltAndroidTest
class QuestPatientTestResultActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @BindValue val patientRepository: PatientRepository = Faker.patientRepository

  private lateinit var patientTestResultActivity: QuestPatientTestResultActivity

  @Before
  fun setUp() {
    hiltRule.inject()
    patientTestResultActivity =
      spyk(
        Robolectric.buildActivity(QuestPatientTestResultActivity::class.java)
          .create()
          .resume()
          .get()
      )
  }

  @Test
  fun testOnBackPressListenerShouldCallFinishActivity() {
    patientTestResultActivity.patientViewModel.onBackPressed(true)
    Assert.assertTrue(patientTestResultActivity.isFinishing)
  }

  override fun getActivity(): Activity {
    return patientTestResultActivity
  }
}
