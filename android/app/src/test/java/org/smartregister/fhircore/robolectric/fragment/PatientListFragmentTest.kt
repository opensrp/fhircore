/*
 * Copyright 2021 Ona Systems Inc
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

package org.smartregister.fhircore.robolectric.fragment

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import com.google.android.material.switchmaterial.SwitchMaterial
import io.mockk.every
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.PatientListActivity
import org.smartregister.fhircore.fragment.FragmentRobolectricTest
import org.smartregister.fhircore.fragment.PatientListFragment
import org.smartregister.fhircore.util.SharedPrefrencesHelper

class PatientListFragmentTest : FragmentRobolectricTest() {

  private lateinit var patientListFragment: PatientListFragment
  private lateinit var fragmentScenario: FragmentScenario<PatientListFragment>

  @Before
  fun setUp() {
    fragmentScenario =
      FragmentScenario.launchInContainer(PatientListFragment::class.java, null, factory)
    fragmentScenario.onFragment { patientListFragment = it }
  }

  @Test
  fun testVerifyOverdueToggleState() {
    val btnOverdue =
      patientListFragment
        .requireActivity()
        .findViewById<SwitchMaterial>(R.id.btn_show_overdue_patients)

    btnOverdue.performClick()
    Assert.assertTrue(SharedPrefrencesHelper.read(PatientListFragment.SHOW_OVERDUE_PATIENTS))

    btnOverdue.performClick()
    Assert.assertFalse(SharedPrefrencesHelper.read(PatientListFragment.SHOW_OVERDUE_PATIENTS))
  }

  override fun getFragmentScenario(): FragmentScenario<out Fragment> {
    return fragmentScenario
  }

  private val factory =
    object : FragmentFactory() {
      override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        val patientListActivity =
          Robolectric.buildActivity(PatientListActivity::class.java).create().resume().get()
        val fragment = spyk(PatientListFragment())
        every { fragment.activity } returns patientListActivity
        return fragment
      }
    }
}
