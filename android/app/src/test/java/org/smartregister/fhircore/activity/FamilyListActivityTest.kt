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
import android.content.Intent
import android.os.Looper
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.test.core.app.ApplicationProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.core.QuestionnaireActivity
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_PATH_KEY
import org.smartregister.fhircore.auth.account.AccountHelper
import org.smartregister.fhircore.auth.secure.FakeKeyStore
import org.smartregister.fhircore.fragment.FamilyListFragment
import org.smartregister.fhircore.model.BaseRegister
import org.smartregister.fhircore.model.FamilyDetailView
import org.smartregister.fhircore.model.FamilyDetailView.Companion.FAMILY_DETAIL_VIEW_CONFIG_ID
import org.smartregister.fhircore.shadow.FhirApplicationShadow
import org.smartregister.fhircore.util.Utils
import org.smartregister.fhircore.viewmodel.FamilyListViewModel

@Config(shadows = [FhirApplicationShadow::class])
class FamilyListActivityTest : ActivityRobolectricTest() {

  private lateinit var familyListActivity: FamilyListActivity
  private lateinit var register: BaseRegister
  private lateinit var detailView: FamilyDetailView
  private lateinit var listViewModel: FamilyListViewModel

  @Before
  fun setUp() {
    familyListActivity =
      Robolectric.buildActivity(FamilyListActivity::class.java, null).create().get()
    register = familyListActivity.register
    detailView =
      Utils.loadConfig(
        FAMILY_DETAIL_VIEW_CONFIG_ID,
        FamilyDetailView::class.java,
        ApplicationProvider.getApplicationContext()
      )
  }

  @Test
  fun testFamilyActivityShouldNotNull() {
    Assert.assertNotNull(familyListActivity)
  }

  @Test
  fun testVerifyFamilyListPagerAdapterProperties() {
    val adapter =
      familyListActivity.findViewById<ViewPager2>(R.id.list_pager).adapter as FragmentStateAdapter

    Assert.assertEquals(1, adapter.itemCount)
    Assert.assertEquals(
      FamilyListFragment::class.java.simpleName,
      adapter.createFragment(0).javaClass.simpleName
    )
  }

  @Test
  fun testFamilyRegisterProperties() {
    Assert.assertEquals(R.layout.activity_register_list, register.contentLayoutId)
    Assert.assertTrue(register.listFragment is FamilyListFragment)
    Assert.assertEquals(
      detailView.registrationQuestionnaireIdentifier,
      register.newRegistrationQuestionnaireIdentifier
    )
    Assert.assertEquals(
      detailView.registrationQuestionnaireTitle,
      register.newRegistrationQuestionnaireTitle
    )
    Assert.assertEquals(R.id.btn_register_new_client, register.newRegistrationViewId)
    Assert.assertEquals(R.id.edit_text_search, register.searchBoxId)
    Assert.assertEquals(R.id.list_pager, register.viewPagerId)
    Assert.assertEquals(findViewById(R.id.list_pager), register.viewPager())
    Assert.assertEquals(findViewById(R.id.edit_text_search), register.searchBox())
    Assert.assertEquals(findViewById(R.id.btn_register_new_client), register.newRegistrationView())

    val familyNavBarTitleView = familyListActivity.getNavigationHeaderTitleView(R.id.tv_nav_header)
    Assert.assertEquals(detailView.registerTitle, familyNavBarTitleView!!.text)

    val toolbarTextView =
      findViewById<Toolbar>(R.id.base_register_toolbar)
        .findViewById<TextView>(R.id.tv_clients_list_title)
    Assert.assertEquals(getString(R.string.client_list_title_family), toolbarTextView!!.text)
  }

  @Test
  fun testVerifyAddClientOpenRegistration() {
    familyListActivity.findViewById<Button>(R.id.btn_register_new_client).performClick()

    val expectedIntent = Intent(familyListActivity, QuestionnaireActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<FhirApplication>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
    Assert.assertEquals(
      register.newRegistrationQuestionnaireIdentifier,
      actualIntent.getStringExtra(QUESTIONNAIRE_PATH_KEY)
    )
  }

  @Test
  fun testGetContentLayoutShouldReturnActivityListLayout() {
    Assert.assertEquals(R.layout.activity_register_list, familyListActivity.getContentLayout())
  }

  @Test
  fun testFamilyCountShouldBeEmptyWithZeroClients() {

    val method =
      familyListActivity.javaClass.superclass?.superclass?.getDeclaredMethod(
        "setMenuCounter",
        Int::class.java,
        Int::class.java
      )
    method?.isAccessible = true
    method?.invoke(familyListActivity, R.id.menu_item_family_clients, 0)

    val countItem =
      familyListActivity
        .getNavigationView()
        .menu
        .findItem(R.id.menu_item_family_clients)
        .actionView as
        TextView
    Assert.assertEquals("", countItem.text)
  }

  @Test
  fun testFamilyCountShouldNotBeEmptyWithNonZeroClients() {

    val method =
      familyListActivity.javaClass.superclass?.superclass?.getDeclaredMethod(
        "setMenuCounter",
        Int::class.java,
        Int::class.java
      )
    method?.isAccessible = true
    method?.invoke(familyListActivity, R.id.menu_item_family_clients, 2)

    val countItem =
      familyListActivity
        .getNavigationView()
        .menu
        .findItem(R.id.menu_item_family_clients)
        .actionView as
        TextView
    Assert.assertEquals("2", countItem.text)
  }

  @Test
  fun `test FamilyListFragmentAdapterCount should return one`() {
    val viewPager = familyListActivity.findViewById<ViewPager2>(R.id.list_pager)
    Assert.assertNotNull(viewPager)
    Assert.assertEquals(1, viewPager?.adapter?.itemCount)
  }

  @Test
  fun testOnNavigationItemSelectedShouldVerifyRelativeActions() {
    val accountHelper = mockk<AccountHelper>()
    familyListActivity.accountHelper = accountHelper

    val menuItem = mockk<MenuItem>()

    every { menuItem.title } returns ""

    every { menuItem.itemId } returns R.id.menu_item_family_clients
    familyListActivity.onNavigationItemSelected(menuItem)

    val expectedIntent = Intent(familyListActivity, FamilyListActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<FhirApplication>()).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testGetAlertDialogBuilderShouldReturnNotNull() {
    Assert.assertNotNull(familyListActivity.getAlertDialogBuilder())
  }

  @Test
  fun testVerifyRegisteredFamilySavedDialogProperty() {
    Assert.assertNull(ShadowAlertDialog.getLatestAlertDialog())

    ReflectionHelpers.callInstanceMethod<Any>(
      familyListActivity,
      "handleRegisterFamilyResult",
      ReflectionHelpers.ClassParameter.from(String::class.java, "1233"),
    )

    shadowOf(Looper.getMainLooper()).idle()
    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    Assert.assertNotNull(dialog)
    Assert.assertEquals("Register another family member?", dialog.message)
  }

  @Test
  fun testReloadListShouldCallSearchResults() {
    val viewModel = mockk<FamilyListViewModel>()

    every { viewModel.searchResults(any(), any(), any()) } just runs

    familyListActivity.listViewModel = viewModel

    familyListActivity.reloadList()

    verify(exactly = 1) { viewModel.searchResults(any(), any(), any()) }
  }

  override fun getActivity(): Activity {
    return familyListActivity
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}
