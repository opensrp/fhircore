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

package org.smartregister.fhircore.anc.ui.anccare.details

import android.app.Activity
import android.view.MenuInflater
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.DisplayName
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.smartregister.fhircore.anc.activity.ActivityRobolectricTest
import org.smartregister.fhircore.anc.shadow.AncApplicationShadow

@Config(shadows = [AncApplicationShadow::class])
internal class AncDetailsActivityTest : ActivityRobolectricTest() {

  private lateinit var patientDetailsActivity: AncDetailsActivity

  private lateinit var patientDetailsActivitySpy: AncDetailsActivity

  @Before
  fun setUp() {
    patientDetailsActivity =
      Robolectric.buildActivity(AncDetailsActivity::class.java, null).create().get()
    patientDetailsActivitySpy = spyk(objToCopy = patientDetailsActivity)
  }

  @Test
  @DisplayName("Should start patient details activity")
  fun testPatientActivityShouldNotNull() {
    Assert.assertNotNull(patientDetailsActivity)
  }

  @Test
  @DisplayName("Should inflate menu and return true")
  fun testThatMenuIsCreated() {

    val menuInflater = mockk<MenuInflater>()

    every { patientDetailsActivitySpy.menuInflater } returns menuInflater
    every { menuInflater.inflate(any(), any()) } returns Unit

    Assert.assertTrue(patientDetailsActivitySpy.onCreateOptionsMenu(null))
  }

  override fun getActivity(): Activity {
    return patientDetailsActivity
  }
}
