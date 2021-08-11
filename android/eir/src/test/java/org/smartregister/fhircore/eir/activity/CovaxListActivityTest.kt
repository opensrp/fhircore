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

package org.smartregister.fhircore.eir.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.SystemClock
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
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
import org.smartregister.fhircore.eir.FhirApplication
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.activity.core.QuestionnaireActivity
import org.smartregister.fhircore.eir.auth.account.AccountHelper
import org.smartregister.fhircore.eir.auth.secure.FakeKeyStore
import org.smartregister.fhircore.eir.auth.secure.SecureConfig
import org.smartregister.fhircore.eir.domain.Language
import org.smartregister.fhircore.eir.fragment.CovaxListFragment
import org.smartregister.fhircore.eir.shadow.FhirApplicationShadow

@Config(shadows = [FhirApplicationShadow::class])
class CovaxListActivityTest : ActivityRobolectricTest() {

  private lateinit var covaxListActivity: CovaxListActivity

  @Before
  fun setUp() {
    covaxListActivity =
      Robolectric.buildActivity(CovaxListActivity::class.java, null).create().get()
  }

  @Test
  fun testPatientActivityShouldNotNull() {
    Assert.assertNotNull(covaxListActivity)
  }

  @Test
  fun testVerifyPatientSearchEditTextDrawables() {
    val drawerLayout = covaxListActivity.findViewById<DrawerLayout>(R.id.drawer_layout)
    val editText = covaxListActivity.findViewById<EditText>(R.id.edit_text_search)

    Assert.assertNotNull(drawerLayout)
    Assert.assertFalse(drawerLayout.isDrawerOpen(GravityCompat.START))

    Assert.assertNotNull(editText)
    Assert.assertNull(editText.compoundDrawables[0])

    editText.setText("")
    Assert.assertNotNull(editText.compoundDrawables[0])

    editText.setText("demo")
    Assert.assertNull(editText.compoundDrawables[0])
    Assert.assertNotNull(editText.compoundDrawables[2])

    val motionEvent =
      MotionEvent.obtain(
        SystemClock.uptimeMillis(),
        SystemClock.uptimeMillis() + 100,
        MotionEvent.ACTION_DOWN,
        0f,
        0f,
        0
      )
    editText.dispatchTouchEvent(motionEvent)
    motionEvent.action = MotionEvent.ACTION_UP
    editText.dispatchTouchEvent(motionEvent)
    Assert.assertTrue(editText.text.isEmpty())

    covaxListActivity.findViewById<ImageButton>(R.id.btn_drawer_menu).performClick()
    Assert.assertTrue(drawerLayout.isDrawerOpen(GravityCompat.START))
  }

  @Test
  fun testVerifyPatientListPagerAdapterProperties() {
    val adapter =
      covaxListActivity.findViewById<ViewPager2>(R.id.list_pager).adapter as FragmentStateAdapter

    Assert.assertEquals(1, adapter.itemCount)
    Assert.assertEquals(
      CovaxListFragment::class.java.simpleName,
      adapter.createFragment(0).javaClass.simpleName
    )
  }

  @Test
  fun testVerifyAddPatientStartedActivity() {
    covaxListActivity.findViewById<Button>(R.id.btn_register_new_client).performClick()

    val expectedIntent = Intent(covaxListActivity, QuestionnaireActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<FhirApplication>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testVerifyAddPatientWithPreAssignedIdStartedActivity() {
    covaxListActivity.startRegistrationActivity("test-id")

    val expectedIntent = Intent(covaxListActivity, QuestionnaireActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<FhirApplication>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
    Assert.assertEquals(
      "test-id",
      actualIntent.getStringExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PRE_ASSIGNED_ID)
    )
  }

  @Test
  fun testGetContentLayoutShouldReturnActivityListLayout() {
    Assert.assertEquals(R.layout.activity_register_list, covaxListActivity.getContentLayout())
  }

  @Test
  fun testPatientLayoutShouldNotBeNull() {
    Assert.assertEquals(R.layout.activity_register_list, covaxListActivity.getContentLayout())
  }

  @Test
  fun testPatientCountShouldBeEmptyWithZeroClients() {

    val method =
      covaxListActivity.javaClass.superclass?.superclass?.getDeclaredMethod(
        "setMenuCounter",
        Int::class.java,
        Int::class.java
      )
    method?.isAccessible = true
    method?.invoke(covaxListActivity, R.id.menu_item_covax_clients, 0)

    val countItem =
      covaxListActivity
        .getNavigationView()
        .menu
        .findItem(R.id.menu_item_covax_clients)
        .actionView as
        TextView
    Assert.assertEquals("", countItem.text)
  }

  @Test
  fun testPatientCountShouldNotBeEmptyWithNonZeroClients() {

    val method =
      covaxListActivity.javaClass.superclass?.superclass?.getDeclaredMethod(
        "setMenuCounter",
        Int::class.java,
        Int::class.java
      )
    method?.isAccessible = true
    method?.invoke(covaxListActivity, R.id.menu_item_covax_clients, 2)

    val countItem =
      covaxListActivity
        .getNavigationView()
        .menu
        .findItem(R.id.menu_item_covax_clients)
        .actionView as
        TextView
    Assert.assertEquals("2", countItem.text)
  }

  @Test
  fun `test language displayed on nav drawer is default English`() {

    covaxListActivity.viewModel.selectedLanguage.observe(
      covaxListActivity,
      Observer {
        val languageMenuItem =
          covaxListActivity.getNavigationView().menu.findItem(R.id.menu_item_language).actionView as
            TextView
        Assert.assertEquals("English", languageMenuItem.text)
      }
    )
  }

  @Test
  fun `test language displayed on Nav Drawer corresponds to View Model default`() {

    covaxListActivity.viewModel.selectedLanguage.observe(
      covaxListActivity,
      {
        val languageMenuItem =
          covaxListActivity.getNavigationView().menu.findItem(R.id.menu_item_language).actionView as
            TextView
        Assert.assertEquals("Swahili", languageMenuItem.text)
      }
    )

    covaxListActivity.viewModel.selectedLanguage.value = "sw"
  }

  @Test
  fun `test onNavigationItemSelected invokes renderSelectLanguageDialog `() {

    val covaxListActivitySpy = spyk(covaxListActivity)

    val arrayAdapter = mockkClass(ArrayAdapter::class)
    val alertDialogBuilder = mockkClass(type = AlertDialog.Builder::class, relaxed = true)

    every { covaxListActivitySpy.getLanguageArrayAdapter() } returns
      arrayAdapter as ArrayAdapter<Language>

    every { covaxListActivitySpy.getAlertDialogBuilder() } returns alertDialogBuilder

    every { covaxListActivitySpy.getLanguageDialogTitle() } returns ""

    covaxListActivitySpy.onNavigationItemSelected(RoboMenuItem(R.id.menu_item_language))

    verify { covaxListActivitySpy.renderSelectLanguageDialog(any()) }
  }

  @Test
  fun `test refreshSelectedLanguage updates nav with correct language`() {

    covaxListActivity.viewModel.selectedLanguage.observe(
      covaxListActivity,
      {
        var languageMenuItem =
          covaxListActivity.getNavigationView().menu.findItem(R.id.menu_item_language).actionView as
            TextView
        Assert.assertEquals("English", languageMenuItem.text)

        covaxListActivity.refreshSelectedLanguage(Language("fr", "French"), covaxListActivity)

        languageMenuItem =
          covaxListActivity.getNavigationView().menu.findItem(R.id.menu_item_language).actionView as
            TextView
        Assert.assertEquals("French", languageMenuItem.text)
      }
    )
  }

  @Test
  fun `test patientListFragmentAdapterCount should return one`() {
    val viewPager = covaxListActivity.findViewById<ViewPager2>(R.id.list_pager)
    Assert.assertNotNull(viewPager)
    Assert.assertEquals(1, viewPager?.adapter?.itemCount)
  }

  @Test
  fun testOnNavigationItemSelectedShouldVerifyRelativeActions() {

    val accountHelper = mockk<AccountHelper>()
    covaxListActivity.accountHelper = accountHelper

    val menuItem = mockk<MenuItem>()

    every { menuItem.title } returns ""

    every { menuItem.itemId } returns R.id.menu_item_covax_clients
    covaxListActivity.onNavigationItemSelected(menuItem)

    val expectedIntent = Intent(covaxListActivity, CovaxListActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<FhirApplication>()).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)

    every { menuItem.itemId } returns R.id.menu_item_logout
    every { accountHelper.logout(any()) } returns Unit

    covaxListActivity.onNavigationItemSelected(menuItem)

    verify(exactly = 1) { accountHelper.logout(any()) }
  }

  @Test
  fun testGetLanguageArrayAdapterShouldReturnValidLangList() {
    covaxListActivity.viewModel.languageList = listOf(Language("ur", "Urdu"))
    val adapter = covaxListActivity.getLanguageArrayAdapter()

    Assert.assertEquals(1, adapter.count)
    Assert.assertEquals("ur", (adapter.getItem(0) as Language).tag)
    Assert.assertEquals("Urdu", (adapter.getItem(0) as Language).displayName)
  }

  @Test
  fun testGetAlertDialogBuilderShouldReturnNotNull() {
    Assert.assertNotNull(covaxListActivity.getAlertDialogBuilder())
  }

  @Test
  fun testGetLanguageDialogTitleShouldReturnValidTitle() {
    Assert.assertEquals("Select Language", covaxListActivity.getLanguageDialogTitle())
  }

  @Test
  fun testRenderSelectLanguageDialogShouldVerifyRefreshSelectedLanguage() {
    val covaxListActivitySpy = spyk(covaxListActivity)

    every { covaxListActivitySpy.renderSelectLanguageDialog(any()) } answers
      {
        covaxListActivity.renderSelectLanguageDialog(covaxListActivity)
      }
    every { covaxListActivitySpy.refreshSelectedLanguage(any(), any()) } answers {}

    val dialog: ShadowAlertDialog =
      shadowOf(covaxListActivitySpy.renderSelectLanguageDialog(covaxListActivitySpy)) as
        ShadowAlertDialog
    dialog.clickOnItem(0)

    verify(exactly = 0) { covaxListActivitySpy.refreshSelectedLanguage(any(), any()) }
  }

  @Test
  fun testSetLogoutUsernameShouldVerify() {
    val secureConfig = mockk<SecureConfig>()

    every { secureConfig.retrieveSessionUsername() } returns "demo"
    covaxListActivity.secureConfig = secureConfig

    covaxListActivity.setLogoutUsername()
    Assert.assertEquals(
      "${covaxListActivity.getString(R.string.logout_as_user)} demo",
      covaxListActivity.getNavigationView().menu.findItem(R.id.menu_item_logout).title
    )
  }

  @Test
  fun testPatientClientCountShouldReturnTen() {
    covaxListActivity.viewModel.clientsCount.value = 10
    val counter =
      covaxListActivity
        .getNavigationView()
        .menu
        .findItem(R.id.menu_item_covax_clients)
        .actionView as
        TextView

    covaxListActivity.viewModel.clientsCount.observe(
      covaxListActivity,
      { Assert.assertEquals("10", counter.text.toString()) }
    )
  }

  override fun getActivity(): Activity {
    return covaxListActivity
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}
