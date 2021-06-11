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
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.viewpager2.widget.ViewPager2
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboMenuItem
import org.smartregister.fhircore.R
import org.smartregister.fhircore.domain.Language
import org.smartregister.fhircore.shadow.FhirApplicationShadow

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

  @Test
  fun `test language displayed on nav drawer is default English`() {

    val languageMenuItem =
      patientListActivity.getNavigationView().menu.findItem(R.id.menu_item_language).actionView as
        TextView
    Assert.assertEquals("English", languageMenuItem.text)
  }

  @Test
  fun `test language displayed on Nav Drawer corresponds to View Model default`() {

    patientListActivity.viewModel.selectedLanguage.value = "sw"

    val languageMenuItem =
      patientListActivity.getNavigationView().menu.findItem(R.id.menu_item_language).actionView as
        TextView
    Assert.assertEquals("Swahili", languageMenuItem.text)
  }

  @Test
  fun `test onNavigationItemSelected invokes renderSelectLanguageDialog `() {

    val patientListActivitySpy = spyk(patientListActivity)

    val arrayAdapter = mockkClass(ArrayAdapter::class)
    val alertDialogBuilder = mockkClass(type = AlertDialog.Builder::class, relaxed = true)

    every { patientListActivitySpy.getLanguageArrayAdapter() } returns
      arrayAdapter as ArrayAdapter<Language>

    every { patientListActivitySpy.getAlertDialogBuilder() } returns alertDialogBuilder

    every { patientListActivitySpy.getLanguageDialogTitle() } returns ""

    patientListActivitySpy.onNavigationItemSelected(RoboMenuItem(R.id.menu_item_language))

    verify { patientListActivitySpy.renderSelectLanguageDialog(any()) }
  }

  @Test
  fun `test refreshSelectedLanguage updates nav with correct language`() {

    var languageMenuItem =
      patientListActivity.getNavigationView().menu.findItem(R.id.menu_item_language).actionView as
        TextView
    Assert.assertEquals("English", languageMenuItem.text)

    patientListActivity.refreshSelectedLanguage(Language("fr", "French"), patientListActivity)

    languageMenuItem =
      patientListActivity.getNavigationView().menu.findItem(R.id.menu_item_language).actionView as
        TextView
    Assert.assertEquals("French", languageMenuItem.text)
  }

  @Test
  fun `test patientListFragmentAdapterCount should return one`() {
    val viewPager = patientListActivity.findViewById<ViewPager2>(R.id.patient_list_pager)
    Assert.assertNotNull(viewPager)
    Assert.assertEquals(1, viewPager?.adapter?.itemCount)
  }

  override fun getActivity(): Activity {
    return patientListActivity
  }
}
