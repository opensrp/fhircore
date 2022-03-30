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

package org.smartregister.fhircore.anc.ui.family

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.sync.Sync
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import javax.inject.Inject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.fakes.RoboMenuItem
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.app.fakes.Faker
import org.smartregister.fhircore.anc.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.anc.ui.anccare.register.AncRegisterFragment
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterFragment
import org.smartregister.fhircore.anc.ui.report.ReportHomeActivity
import org.smartregister.fhircore.anc.util.AncJsonSpecificationProvider
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.userprofile.UserProfileFragment
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
internal class FamilyRegisterActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @BindValue
  var configurationRegistry: ConfigurationRegistry =
    Faker.buildTestConfigurationRegistry("anc", mockk())
  @Inject lateinit var jsonSpecificationProvider: AncJsonSpecificationProvider

  private lateinit var familyRegisterActivity: FamilyRegisterActivity

  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk()
  @BindValue val secureSharedPreference: SecureSharedPreference = mockk()

  @Before
  fun setUp() {
    mockkObject(Sync)

    hiltRule.inject()

    every { sharedPreferencesHelper.read(any(), any<String>()) } returns "1234"

    familyRegisterActivity =
      Robolectric.buildActivity(FamilyRegisterActivity::class.java).create().get()
  }

  @After
  fun cleanup() {
    unmockkObject(Sync)
  }

  @Test
  fun testActivityShouldNotNull() {
    assertNotNull(familyRegisterActivity)
  }

  @Test
  fun testSupportedFragmentsShouldReturnAncRegisterFragment() {
    val fragments = familyRegisterActivity.supportedFragments()
    assertEquals(3, fragments.size)
    assertTrue(fragments.containsKey(FamilyRegisterFragment.TAG))
    assertTrue(fragments.containsKey(AncRegisterFragment.TAG))
    assertTrue(fragments.containsKey(UserProfileFragment.TAG))
  }

  @Test
  fun testOnClientMenuOptionSelectedShouldLaunchPatientRegisterFragment() {
    familyRegisterActivity.onBottomNavigationOptionItemSelected(
      RoboMenuItem().apply { itemId = "menu_item_register".hashCode() },
      familyRegisterActivity.registerViewModel.registerViewConfiguration.value!!
    )
    // switched to patient register fragment
    assertEquals(
      "Families",
      familyRegisterActivity.findViewById<TextView>(R.id.register_filter_textview).text
    )
    assertEquals(
      View.VISIBLE,
      familyRegisterActivity.findViewById<View>(R.id.filter_register_button).visibility
    )
    assertEquals(
      View.VISIBLE,
      familyRegisterActivity.findViewById<View>(R.id.edit_text_search).visibility
    )
  }

  @Test
  fun testOnBottomOptionSelectedShouldLaunchUserProfileFragment() {
    familyRegisterActivity.onBottomNavigationOptionItemSelected(
      RoboMenuItem().apply { itemId = "menu_item_profile".hashCode() },
      familyRegisterActivity.registerViewModel.registerViewConfiguration.value!!
    )
    // switched to user profile fragment
    assertEquals(
      "Profile",
      familyRegisterActivity.findViewById<TextView>(R.id.register_filter_textview).text
    )
    assertEquals(
      View.GONE,
      familyRegisterActivity.findViewById<ImageButton>(R.id.filter_register_button).visibility
    )
  }

  @Test
  fun testOnSettingMenuOptionSelectedShouldLaunchUserProfileFragment() {
    familyRegisterActivity.onNavigationOptionItemSelected(
      RoboMenuItem().apply { itemId = "menu_item_profile".hashCode() }
    )
    // switched to user profile fragment

    assertEquals(
      View.VISIBLE,
      familyRegisterActivity.findViewById<ImageButton>(R.id.filter_register_button).visibility
    )
  }

  @Test
  fun testOnSettingMenuOptionSelectedShouldLaunchReportScreen() {
    familyRegisterActivity.onNavigationOptionItemSelected(
      RoboMenuItem().apply { itemId = "menu_item_reports".hashCode() }
    )
    familyRegisterActivity.navigateToReports()
    // switched to report screen
    val expectedIntent = Intent(familyRegisterActivity, ReportHomeActivity::class.java)
    val actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<HiltTestApplication>())
        .nextStartedActivity
    assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testOnBottomNavigationOptionItemSelectedShouldLaunchReportHomeActivity() {
    familyRegisterActivity.onBottomNavigationOptionItemSelected(
      RoboMenuItem().apply { itemId = "menu_item_reports".hashCode() },
      familyRegisterActivity.registerViewModel.registerViewConfiguration.value!!
    )

    val expectedIntent = Intent(familyRegisterActivity, ReportHomeActivity::class.java)
    val actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<HiltTestApplication>())
        .nextStartedActivity

    assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testNavigateToReports() {
    familyRegisterActivity.navigateToReports()

    val expectedIntent = Intent(familyRegisterActivity, ReportHomeActivity::class.java)
    val actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<HiltTestApplication>())
        .nextStartedActivity

    assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testRegistersListShouldReturnFamilyAndAncRegisterItemList() {
    val listRegisterItem = familyRegisterActivity.registersList()
    assertEquals(listRegisterItem.size, 2)

    with(listRegisterItem[0]) {
      assertEquals(FamilyRegisterFragment.TAG, uniqueTag)
      assertEquals(familyRegisterActivity.getString(R.string.households), title)
      assertTrue(isSelected)
    }

    with(listRegisterItem[1]) {
      assertEquals(AncRegisterFragment.TAG, uniqueTag)
      assertEquals(familyRegisterActivity.getString(R.string.anc_clients), title)
      assertFalse(isSelected)
    }
  }

  @Test
  fun testSideMenuList() {
    val listRegisterItem = familyRegisterActivity.sideMenuOptions()
    assertEquals(listRegisterItem.size, 7)

    with(listRegisterItem[0]) {
      assertEquals(R.id.menu_item_families, this.itemId)
      assertEquals(R.string.households, this.titleResource)
    }

    with(listRegisterItem[1]) {
      assertEquals(R.id.menu_item_anc_clients, this.itemId)
      assertEquals(R.string.pregnant_clients, this.titleResource)
    }

    with(listRegisterItem[2]) {
      assertEquals(R.id.menu_item_post_natal_clients, this.itemId)
      assertEquals(R.string.post_natal_clients, this.titleResource)
    }

    with(listRegisterItem[3]) {
      assertEquals(R.id.menu_item_child_clients, this.itemId)
      assertEquals(R.string.child_clients, this.titleResource)
    }

    with(listRegisterItem[4]) {
      assertEquals(R.id.menu_item_family_planning_clients, this.itemId)
      assertEquals(R.string.family_planning_clients, this.titleResource)
    }

    with(listRegisterItem[5]) {
      assertEquals(R.id.menu_item_reports, this.itemId)
      assertEquals(R.string.reports, this.titleResource)
    }

    with(listRegisterItem[6]) {
      assertEquals(R.id.menu_item_profile, this.itemId)
      assertEquals(R.string.profile, this.titleResource)
    }
  }

  @Test
  fun testMainFragmentTagShouldReturnFamilyRegisterFragmentTag() {
    assertEquals(FamilyRegisterFragment.TAG, familyRegisterActivity.mainFragmentTag())
  }

  override fun getActivity(): Activity {
    return familyRegisterActivity
  }
}
