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

package org.smartregister.fhircore.anc.ui.anccare.encounters

import android.app.Activity
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.activity.ActivityRobolectricTest

class EncounterListActivityTest : ActivityRobolectricTest() {

  private lateinit var activity: EncounterListActivity

  @Before
  fun setUp() {
    activity = Robolectric.buildActivity(EncounterListActivity::class.java).create().get()
  }

  @Test
  fun testHandleBackClickedShouldCallFinishMethod() {
    val spyActivity = spyk(activity)

    every { spyActivity.finish() } returns Unit

    ReflectionHelpers.callInstanceMethod<Any>(spyActivity, "handleBackClicked")

    verify(exactly = 1) { spyActivity.finish() }
  }

  override fun getActivity(): Activity {
    return activity
  }
}
