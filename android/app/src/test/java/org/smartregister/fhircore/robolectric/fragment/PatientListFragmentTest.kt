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

import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
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
import org.robolectric.Shadows
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.PatientDetailActivity
import org.smartregister.fhircore.activity.PatientListActivity
import org.smartregister.fhircore.domain.Pagination
import org.smartregister.fhircore.fragment.FragmentRobolectricTest
import org.smartregister.fhircore.fragment.PatientDetailFragment
import org.smartregister.fhircore.fragment.PatientListFragment
import org.smartregister.fhircore.util.SharedPrefrencesHelper
import org.smartregister.fhircore.viewmodel.PatientListViewModel

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

  @Test
  fun testOnPatientItemClicked() {

    val id = "49333c33-f50f-4c3e-abd4-7aeb0f160ac2"
    val logicalId = "812983127"
    val patientItem =
      PatientListViewModel.PatientItem(
        id,
        "John Doe",
        "male",
        "1985-05-21",
        "somehtml",
        "0700 000 000",
        logicalId
      )

    Shadows.shadowOf(Looper.getMainLooper()).idle()

    patientListFragment.onPatientItemClicked(PatientListFragment.Intention.VIEW, patientItem)

    val shadowActivity = Shadows.shadowOf(FhirApplication.getContext())
    val startedActivityIntent = shadowActivity.peekNextStartedActivity()

    Assert.assertEquals(
      logicalId,
      startedActivityIntent.getStringExtra(PatientDetailFragment.ARG_ITEM_ID)
    )
    Assert.assertEquals(
      PatientDetailActivity::class.java.name,
      startedActivityIntent.component?.className
    )
  }

  @Test
  fun testEmptyListMessageWithZeroClients() {
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    val container = getView<LinearLayout>(R.id.empty_list_message_container)
    val buttonLayout =
      getView<Button>(R.id.btn_register_new_patient).layoutParams as RelativeLayout.LayoutParams

    Assert.assertEquals(View.VISIBLE, container.visibility)
    Assert.assertEquals(
      R.id.empty_list_message_container,
      buttonLayout.getRule(RelativeLayout.BELOW)
    )
  }

  @Test
  fun testEmptyListMessageWithNonZeroClients() {
    var patient =
      PatientListViewModel.PatientItem(
        "12",
        "John Doe",
        "male",
        "1985-05-21",
        "somehtml",
        "0700 000 000",
        "test_id"
      )
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    patientListFragment.patientListViewModel.liveSearchedPaginatedPatients.value =
      Pair(mutableListOf(patient), Pagination(totalItems = 1, pageSize = 5, currentPage = 1))

    val container = getView<LinearLayout>(R.id.empty_list_message_container)
    val buttonLayout =
      getView<Button>(R.id.btn_register_new_patient).layoutParams as RelativeLayout.LayoutParams

    Assert.assertEquals(View.INVISIBLE, container.visibility)
    Assert.assertEquals(
      RelativeLayout.TRUE,
      buttonLayout.getRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
    )
  }

  private fun <T : View?> getView(id: Int): T {
    return patientListFragment.requireActivity().findViewById<T>(id)
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
