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

package org.smartregister.fhirecore.quest.ui.patient.details

import android.app.Activity
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.quest.ui.patient.details.QuestPatientTestResultActivity
import org.smartregister.fhirecore.quest.robolectric.ActivityRobolectricTest
import org.smartregister.fhirecore.quest.shadow.QuestApplicationShadow

@Config(shadows = [QuestApplicationShadow::class])
class QuestPatientTestResultActivityTest : ActivityRobolectricTest() {

  private lateinit var activity: QuestPatientTestResultActivity

  @Before
  fun setUp() {
    activity = Robolectric.buildActivity(QuestPatientTestResultActivity::class.java).create().get()
  }

  @Test
  fun testOnBackPressListenerShouldCallFinishActivity() {
    val spyActivity = spyk(activity)
    every { spyActivity.finish() } returns Unit

    ReflectionHelpers.callInstanceMethod<Any>(spyActivity, "onBackPressListener")

    verify(exactly = 1) { spyActivity.finish() }
  }

  override fun getActivity(): Activity {
    return activity
  }
}
