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

package org.smartregister.fhircore.anc.ui.reports

import android.app.Activity
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.smartregister.fhircore.anc.activity.ActivityRobolectricTest
import org.smartregister.fhircore.anc.shadow.AncApplicationShadow

@Config(shadows = [AncApplicationShadow::class])
class ReportHomeActivityTest : ActivityRobolectricTest() {

  private lateinit var activity: ReportHomeActivity
  private lateinit var activitySpy: ReportHomeActivity

  @Before
  fun setUp() {
    activity = Robolectric.buildActivity(ReportHomeActivity::class.java).create().get()
    activitySpy = spyk(objToCopy = activity)
  }

  @Test
  fun testActivityNotNull() {
    Assert.assertNotNull(activity)
  }

  override fun getActivity(): Activity {
    return activity
  }
}
