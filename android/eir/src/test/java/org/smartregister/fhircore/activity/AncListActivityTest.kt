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
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.test.core.app.ApplicationProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.core.QuestionnaireActivity
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_PATH_KEY
import org.smartregister.fhircore.auth.account.AccountHelper
import org.smartregister.fhircore.auth.secure.FakeKeyStore
import org.smartregister.fhircore.fragment.AncListFragment
import org.smartregister.fhircore.model.AncDetailView
import org.smartregister.fhircore.model.AncDetailView.Companion.ANC_DETAIL_VIEW_CONFIG_ID
import org.smartregister.fhircore.model.BaseRegister
import org.smartregister.fhircore.shadow.FhirApplicationShadow
import org.smartregister.fhircore.util.Utils

@Config(shadows = [FhirApplicationShadow::class])
class AncListActivityTest : ActivityRobolectricTest() {

  private lateinit var ancListActivity: AncListActivity
  private lateinit var register: BaseRegister
  private lateinit var detailView: AncDetailView

  @Before
  fun setUp() {
    ancListActivity = Robolectric.buildActivity(AncListActivity::class.java, null).create().get()
    register = ancListActivity.register
    detailView =
      Utils.loadConfig(
        ANC_DETAIL_VIEW_CONFIG_ID,
        AncDetailView::class.java,
        ApplicationProvider.getApplicationContext()
      )
  }

  @Test
  fun testAncActivityShouldNotNull() {
    Assert.assertNotNull(ancListActivity)
  }

  @Test
  fun testVerifyAncListPagerAdapterProperties() {
    val adapter =
      ancListActivity.findViewById<ViewPager2>(R.id.list_pager).adapter as FragmentStateAdapter

    Assert.assertEquals(1, adapter.itemCount)
    Assert.assertEquals(
      AncListFragment::class.java.simpleName,
      adapter.createFragment(0).javaClass.simpleName
    )
  }

  @Test
  fun testAncRegisterProperties() {
    Assert.assertEquals(R.layout.activity_register_list, register.contentLayoutId)
    Assert.assertTrue(register.listFragment is AncListFragment)
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

    val ancNavBarTitleView = ancListActivity.getNavigationHeaderTitleView(R.id.tv_nav_header)
    Assert.assertEquals(detailView.registerTitle, ancNavBarTitleView!!.text)

    val toolbarTextView =
      findViewById<Toolbar>(R.id.base_register_toolbar)
        .findViewById<TextView>(R.id.tv_clients_list_title)
    Assert.assertEquals(getString(R.string.client_list_title_anc), toolbarTextView!!.text)
  }

  @Test
  fun testVerifyAddPatientStartedActivity() {
    ancListActivity.findViewById<Button>(R.id.btn_register_new_client).performClick()

    val expectedIntent = Intent(ancListActivity, QuestionnaireActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<FhirApplication>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
    Assert.assertEquals(
      register.newRegistrationQuestionnaireIdentifier,
      actualIntent.getStringExtra(QUESTIONNAIRE_PATH_KEY)
    )
  }

  @Test
  fun testVerifyAddPatientWithPreAssignedIdStartedActivity() {
    ancListActivity.startRegistrationActivity("test-id")

    val expectedIntent = Intent(ancListActivity, QuestionnaireActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<FhirApplication>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
    Assert.assertEquals(
      "test-id",
      actualIntent.getStringExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PRE_ASSIGNED_ID)
    )
    Assert.assertEquals(
      register.newRegistrationQuestionnaireIdentifier,
      actualIntent.getStringExtra(QUESTIONNAIRE_PATH_KEY)
    )
  }

  @Test
  fun testGetContentLayoutShouldReturnActivityListLayout() {
    Assert.assertEquals(R.layout.activity_register_list, ancListActivity.getContentLayout())
  }

  @Test
  fun testAncCountShouldBeEmptyWithZeroClients() {

    val method =
      ancListActivity.javaClass.superclass?.superclass?.getDeclaredMethod(
        "setMenuCounter",
        Int::class.java,
        Int::class.java
      )
    method?.isAccessible = true
    method?.invoke(ancListActivity, R.id.menu_item_anc_clients, 0)

    val countItem =
      ancListActivity.getNavigationView().menu.findItem(R.id.menu_item_anc_clients).actionView as
        TextView
    Assert.assertEquals("", countItem.text)
  }

  @Test
  fun testAncCountShouldNotBeEmptyWithNonZeroClients() {

    val method =
      ancListActivity.javaClass.superclass?.superclass?.getDeclaredMethod(
        "setMenuCounter",
        Int::class.java,
        Int::class.java
      )
    method?.isAccessible = true
    method?.invoke(ancListActivity, R.id.menu_item_anc_clients, 2)

    val countItem =
      ancListActivity.getNavigationView().menu.findItem(R.id.menu_item_anc_clients).actionView as
        TextView
    Assert.assertEquals("2", countItem.text)
  }

  @Test
  fun `test AncListFragmentAdapterCount should return one`() {
    val viewPager = ancListActivity.findViewById<ViewPager2>(R.id.list_pager)
    Assert.assertNotNull(viewPager)
    Assert.assertEquals(1, viewPager?.adapter?.itemCount)
  }

  @Test
  fun testOnNavigationItemSelectedShouldVerifyRelativeActions() {
    val accountHelper = mockk<AccountHelper>()
    ancListActivity.accountHelper = accountHelper

    val menuItem = mockk<MenuItem>()

    every { menuItem.title } returns ""

    every { menuItem.itemId } returns R.id.menu_item_anc_clients
    ancListActivity.onNavigationItemSelected(menuItem)

    val expectedIntent = Intent(ancListActivity, AncListActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<FhirApplication>()).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testGetAlertDialogBuilderShouldReturnNotNull() {
    Assert.assertNotNull(ancListActivity.getAlertDialogBuilder())
  }

  override fun getActivity(): Activity {
    return ancListActivity
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}
