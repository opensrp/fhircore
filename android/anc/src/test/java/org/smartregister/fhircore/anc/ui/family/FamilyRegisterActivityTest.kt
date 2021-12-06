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
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.fakes.RoboMenuItem
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.activity.BaseRegisterActivityTest
import org.smartregister.fhircore.anc.ui.anccare.register.AncRegisterFragment
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterFragment
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.userprofile.UserProfileFragment

@HiltAndroidTest
internal class FamilyRegisterActivityTest : BaseRegisterActivityTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var accountAuthenticator: AccountAuthenticator

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  private lateinit var familyRegisterActivity: FamilyRegisterActivity

  @Before
  fun setUp() {
    hiltRule.inject()
    configurationRegistry.loadAppConfigurations(
      appId = "anc",
      accountAuthenticator = accountAuthenticator
    ) {}
    familyRegisterActivity =
      Robolectric.buildActivity(FamilyRegisterActivity::class.java).create().get()
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
    familyRegisterActivity.onNavigationOptionItemSelected(
      RoboMenuItem().apply { itemId = R.id.menu_item_register }
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
  fun testOnSettingMenuOptionSelectedShouldLaunchUserProfileFragment() {
    familyRegisterActivity.onNavigationOptionItemSelected(
      RoboMenuItem().apply { itemId = R.id.menu_item_profile }
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
  fun testRegistersListShouldReturnFamilyAndAncRegisterItemList() {
    val listRegisterItem = familyRegisterActivity.registersList()
    assertEquals(listRegisterItem.size, 2)

    with(listRegisterItem[0]) {
      assertEquals(FamilyRegisterFragment.TAG, uniqueTag)
      assertEquals(familyRegisterActivity.getString(R.string.families), title)
      assertTrue(isSelected)
    }

    with(listRegisterItem[1]) {
      assertEquals(AncRegisterFragment.TAG, uniqueTag)
      assertEquals(familyRegisterActivity.getString(R.string.anc_clients), title)
      assertFalse(isSelected)
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
