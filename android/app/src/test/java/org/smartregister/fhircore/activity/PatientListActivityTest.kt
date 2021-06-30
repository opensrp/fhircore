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
import android.app.AlertDialog
import android.content.Intent
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboMenuItem
import org.robolectric.shadows.ShadowAlertDialog
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.auth.account.AccountHelper
import org.smartregister.fhircore.auth.secure.FakeKeyStore
import org.smartregister.fhircore.auth.secure.SecureConfig
import org.smartregister.fhircore.domain.Language
import org.smartregister.fhircore.fragment.PatientListFragment
import org.smartregister.fhircore.shadow.FhirApplicationShadow

@Config(shadows = [FhirApplicationShadow::class])
class PatientListActivityTest : ActivityRobolectricTest() {

  private lateinit var patientListActivity: PatientListActivity

  @Before
  fun setUp() {
    patientListActivity =
      Robolectric.buildActivity(PatientListActivity::class.java, null).create().get()
  }

  @Test
  fun testPatientActivityShouldNotNull() {
    Assert.assertNotNull(patientListActivity)
  }

  @Test
  fun testVerifyPatientSearchEditTextDrawables() {
    val editText = patientListActivity.findViewById<EditText>(R.id.edit_text_search)

    Assert.assertNotNull(editText)
    Assert.assertNull(editText.compoundDrawables[0])

    editText.setText("")
    Assert.assertNotNull(editText.compoundDrawables[0])

    editText.setText("demo")
    Assert.assertNull(editText.compoundDrawables[0])
    Assert.assertNotNull(editText.compoundDrawables[2])
  }

  @Test
  fun testVerifyPatientListPagerAdapterProperties() {
    val adapter =
      patientListActivity.findViewById<ViewPager2>(R.id.patient_list_pager).adapter as
        FragmentStateAdapter

    Assert.assertEquals(1, adapter.itemCount)
    Assert.assertEquals(
      PatientListFragment::class.java.simpleName,
      adapter.createFragment(0).javaClass.simpleName
    )
  }

  @Test
  fun testVerifyAddPatientStartedActivity() {
    patientListActivity.findViewById<Button>(R.id.btn_register_new_patient).performClick()

    val expectedIntent = Intent(patientListActivity, QuestionnaireActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<FhirApplication>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testGetContentLayoutShouldReturnActivityListLayout() {
    Assert.assertEquals(R.layout.activity_patient_list, patientListActivity.getContentLayout())
  }

  @Test
  fun testPatientLayoutShouldNotBeNull() {
    Assert.assertEquals(R.layout.activity_patient_list, patientListActivity.getContentLayout())
  }

  @Test
  fun testPatientCountShouldBeEmptyWithZeroClients() {

    val method =
      patientListActivity.javaClass.superclass?.getDeclaredMethod(
        "setMenuCounter",
        Int::class.java,
        Int::class.java
      )
    method?.isAccessible = true
    method?.invoke(patientListActivity, R.id.menu_item_clients, 0)

    val countItem =
      patientListActivity.getNavigationView().menu.findItem(R.id.menu_item_clients).actionView as
        TextView
    Assert.assertEquals("", countItem.text)
  }

  @Test
  fun testPatientCountShouldNotBeEmptyWithNonZeroClients() {

    val method =
      patientListActivity.javaClass.superclass?.getDeclaredMethod(
        "setMenuCounter",
        Int::class.java,
        Int::class.java
      )
    method?.isAccessible = true
    method?.invoke(patientListActivity, R.id.menu_item_clients, 2)

    val countItem =
      patientListActivity.getNavigationView().menu.findItem(R.id.menu_item_clients).actionView as
        TextView
    Assert.assertEquals("2", countItem.text)
  }

  @Test
  fun `test language displayed on nav drawer is default English`() {

    patientListActivity.viewModel.selectedLanguage.observe(
      patientListActivity,
      Observer {
        val languageMenuItem =
          patientListActivity
            .getNavigationView()
            .menu
            .findItem(R.id.menu_item_language)
            .actionView as
            TextView
        Assert.assertEquals("English", languageMenuItem.text)
      }
    )
  }

  @Test
  fun `test language displayed on Nav Drawer corresponds to View Model default`() {

    patientListActivity.viewModel.selectedLanguage.observe(
      patientListActivity,
      {
        val languageMenuItem =
          patientListActivity
            .getNavigationView()
            .menu
            .findItem(R.id.menu_item_language)
            .actionView as
            TextView
        Assert.assertEquals("Swahili", languageMenuItem.text)
      }
    )

    patientListActivity.viewModel.selectedLanguage.value = "sw"
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

    patientListActivity.viewModel.selectedLanguage.observe(
      patientListActivity,
      {
        var languageMenuItem =
          patientListActivity
            .getNavigationView()
            .menu
            .findItem(R.id.menu_item_language)
            .actionView as
            TextView
        Assert.assertEquals("English", languageMenuItem.text)

        patientListActivity.refreshSelectedLanguage(Language("fr", "French"), patientListActivity)

        languageMenuItem =
          patientListActivity
            .getNavigationView()
            .menu
            .findItem(R.id.menu_item_language)
            .actionView as
            TextView
        Assert.assertEquals("French", languageMenuItem.text)
      }
    )
  }

  @Test
  fun `test patientListFragmentAdapterCount should return one`() {
    val viewPager = patientListActivity.findViewById<ViewPager2>(R.id.patient_list_pager)
    Assert.assertNotNull(viewPager)
    Assert.assertEquals(1, viewPager?.adapter?.itemCount)
  }

  @Test
  fun testOnNavigationItemSelectedShouldVerifyRelativeActions() {

    val accountHelper = mockk<AccountHelper>()
    patientListActivity.accountHelper = accountHelper

    val menuItem = mockk<MenuItem>()

    every { menuItem.title } returns ""

    every { menuItem.itemId } returns R.id.menu_item_clients
    patientListActivity.onNavigationItemSelected(menuItem)

    val expectedIntent = Intent(patientListActivity, PatientListActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<FhirApplication>()).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)

    every { menuItem.itemId } returns R.id.menu_item_logout
    every { accountHelper.logout(any()) } returns Unit

    patientListActivity.onNavigationItemSelected(menuItem)

    verify(exactly = 1) { accountHelper.logout(any()) }
  }

  @Test
  fun testGetLanguageArrayAdapterShouldReturnValidLangList() {
    patientListActivity.viewModel.languageList = listOf(Language("ur", "Urdu"))
    val adapter = patientListActivity.getLanguageArrayAdapter()

    Assert.assertEquals(1, adapter.count)
    Assert.assertEquals("ur", (adapter.getItem(0) as Language).tag)
    Assert.assertEquals("Urdu", (adapter.getItem(0) as Language).displayName)
  }

  @Test
  fun testGetAlertDialogBuilderShouldReturnNotNull() {
    Assert.assertNotNull(patientListActivity.getAlertDialogBuilder())
  }

  @Test
  fun testGetLanguageDialogTitleShouldReturnValidTitle() {
    Assert.assertEquals("Select Language", patientListActivity.getLanguageDialogTitle())
  }

  @Test
  fun testRenderSelectLanguageDialogShouldVerifyRefreshSelectedLanguage() {
    val patientListActivitySpy = spyk(patientListActivity)

    every { patientListActivitySpy.renderSelectLanguageDialog(any()) } answers
      {
        patientListActivity.renderSelectLanguageDialog(patientListActivity)
      }
    every { patientListActivitySpy.refreshSelectedLanguage(any(), any()) } answers {}

    val dialog: ShadowAlertDialog =
      shadowOf(patientListActivitySpy.renderSelectLanguageDialog(patientListActivitySpy)) as
        ShadowAlertDialog
    dialog.clickOnItem(0)

    verify(exactly = 0) { patientListActivitySpy.refreshSelectedLanguage(any(), any()) }
  }

  @Test
  fun testSetLogoutUsernameShouldVerify() {
    val secureConfig = mockk<SecureConfig>()

    every { secureConfig.retrieveSessionUsername() } returns "demo"
    patientListActivity.secureConfig = secureConfig

    patientListActivity.setLogoutUsername()
    patientListActivity.viewModel.username.observe(
      patientListActivity,
      {
        Assert.assertEquals(
          "${patientListActivity.getString(R.string.logout_as_user)} demo",
          patientListActivity.getNavigationView().menu.findItem(R.id.menu_item_logout).title
        )
      }
    )
  }

  @Test
  fun testPatientClientCountShouldReturnTen() {
    patientListActivity.viewModel.covaxClientsCount.value = 10
    val counter =
      patientListActivity.getNavigationView().menu.findItem(R.id.menu_item_clients).actionView as
        TextView

    patientListActivity.viewModel.covaxClientsCount.observe(
      patientListActivity,
      { Assert.assertEquals("10", counter.text.toString()) }
    )
  }

  override fun getActivity(): Activity {
    return patientListActivity
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}
