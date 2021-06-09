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

package org.smartregister.fhircore.robolectric.activity

import android.app.Activity
import android.widget.TextView
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.PatientListActivity
import org.smartregister.fhircore.robolectric.shadow.FhirApplicationShadow

@Config(shadows = [FhirApplicationShadow::class])
class PatientListActivityTest : ActivityRobolectricTest() {

  private lateinit var patientListActivity: PatientListActivity

  @Before
  fun setUp() {
    patientListActivity =
      Robolectric.buildActivity(PatientListActivity::class.java, null).create().resume().get()
  }

  @Test
  fun testPatientActivityShouldNotNull() {
    Assert.assertNotNull(patientListActivity)
  }

  @Test
  fun testPatientLayoutShouldNotBeNull() {
    Assert.assertEquals(R.layout.activity_patient_list, patientListActivity.getContentLayout())
  }

  @Test
  fun testPatientCountShouldBeEmptyWithZeroClients() {
    patientListActivity.viewModel.covaxClientsCount.value = 0

    val countItem =
      patientListActivity.getNavigationView().menu.findItem(R.id.menu_item_clients).actionView as
        TextView
    Assert.assertEquals("", countItem.text)
  }

  @Test
  fun testPatientCountShouldNotBeEmptyWithNonZeroClients() {
    patientListActivity.viewModel.covaxClientsCount.value = 2

    val countItem =
      patientListActivity.getNavigationView().menu.findItem(R.id.menu_item_clients).actionView as
        TextView
    Assert.assertEquals("2", countItem.text)
  }

  override fun getActivity(): Activity {
    return patientListActivity
  }
}
