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

package org.smartregister.fhircore.quest.ui.patient.register

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.size
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.fakes.RoboMenuItem
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.NavigationOption
import org.smartregister.fhircore.engine.databinding.BaseRegisterActivityBinding
import org.smartregister.fhircore.engine.ui.register.model.RegisterItem
import org.smartregister.fhircore.engine.ui.userprofile.UserProfileFragment
import org.smartregister.fhircore.engine.util.LAST_SYNC_TIMESTAMP
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.configuration.view.QuestionnaireDataDetailsNavigationAction
import org.smartregister.fhircore.quest.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.quest.ui.patient.details.QuestionnaireDataDetailActivity

@HiltAndroidTest
@Ignore("To be deleted test class; new test to be written after refactor")
class PatientRegisterActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @BindValue
  var configurationRegistry: ConfigurationRegistry =
    Faker.buildTestConfigurationRegistry("quest", mockk())

  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk()
  @BindValue val secureSharedPreference: SecureSharedPreference = mockk()

  private lateinit var patientRegisterActivity: PatientRegisterActivity

  @Before
  fun setUp() {
    hiltRule.inject()

    every { sharedPreferencesHelper.read(any(), any<String>()) } answers
      {
        if (firstArg<String>() == LAST_SYNC_TIMESTAMP) {
          ""
        } else {
          "1234"
        }
      }
    every { secureSharedPreference.retrieveSessionUsername() } returns "demo"

    patientRegisterActivity =
      Robolectric.buildActivity(PatientRegisterActivity::class.java).create().resume().get()
  }

  @Test
  fun testSideMenuOptionsShouldReturnZeroOptions() {
    Assert.assertTrue(patientRegisterActivity.sideMenuOptions().isEmpty())
  }

  @Test
  fun testOnSideMenuOptionSelectedShouldReturnTrue() {
    Assert.assertTrue(patientRegisterActivity.onNavigationOptionItemSelected(RoboMenuItem()))
  }

  @Test
  fun testOnClientMenuOptionSelectedShouldLaunchPatientRegisterFragment() {
    patientRegisterActivity.onNavigationOptionItemSelected(
      RoboMenuItem().apply { itemId = R.id.menu_item_clients }
    )
    // switched to patient register fragment
    Assert.assertEquals(
      "Clients",
      patientRegisterActivity.findViewById<TextView>(R.id.register_filter_textview).text
    )
    Assert.assertEquals(
      View.GONE,
      patientRegisterActivity.findViewById<View>(R.id.filter_register_button).visibility
    )
    Assert.assertEquals(
      View.VISIBLE,
      patientRegisterActivity.findViewById<View>(R.id.edit_text_search).visibility
    )
  }

  @Test
  fun testOnBottomNavigationOptionItemSelectedShouldLaunchUserProfileFragment() {
    patientRegisterActivity.onBottomNavigationOptionItemSelected(
      RoboMenuItem().apply { itemId = "menu_item_settings".hashCode() },
      patientRegisterActivity.registerViewModel.registerViewConfiguration.value!!
    )
    // switched to user profile fragment
    Assert.assertEquals(
      "Settings",
      patientRegisterActivity.findViewById<TextView>(R.id.register_filter_textview).text
    )
    Assert.assertEquals(
      View.GONE,
      patientRegisterActivity.findViewById<View>(R.id.middle_toolbar_section).visibility
    )
    Assert.assertEquals(
      View.GONE,
      patientRegisterActivity.findViewById<ImageButton>(R.id.filter_register_button).visibility
    )
  }

  @Test
  fun testOnBottomNavigationOptionItemSelectedShouldLaunchQuestionnaireDataDetailActivity() {

    val config = patientRegisterActivity.registerViewModel.registerViewConfiguration.value!!

    config.bottomNavigationOptions?.plus(
      NavigationOption(
        id = "control_test",
        title = "Control Test",
        icon = "ic_reports",
        action =
          QuestionnaireDataDetailsNavigationAction(classification = "control_test_details_view")
      )
    )

    patientRegisterActivity.onBottomNavigationOptionItemSelected(
      RoboMenuItem().apply { itemId = "control_test".hashCode() },
      config
    )

    val expectedIntent =
      Intent(patientRegisterActivity, QuestionnaireDataDetailActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<HiltTestApplication>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testSetupConfigurableViewsShouldUpdateViews() {
    val activityBinding =
      ReflectionHelpers.getField<BaseRegisterActivityBinding>(
        patientRegisterActivity,
        "registerActivityBinding"
      )

    with(activityBinding) {
      Assert.assertEquals("Register new client", this.btnRegisterNewClient.text)
      Assert.assertEquals("Clients", this.toolbarLayout.tvClientsListTitle.text)
      Assert.assertEquals(3, this.bottomNavView.menu.size)
    }
  }

  @Test
  fun testBottomMenuOptionsShouldReturnNonZeroOptions() {
    val registerViewConfiguration =
      patientRegisterActivity.registerViewModel.registerViewConfiguration.value!!
    val menu = patientRegisterActivity.bottomNavigationMenuOptions(registerViewConfiguration)

    Assert.assertEquals(3, menu.size)
    Assert.assertEquals("menu_item_clients".hashCode(), menu[0].id)
    Assert.assertEquals(getString(R.string.menu_clients), menu[0].title)
    Assert.assertEquals("menu_item_tasks".hashCode(), menu[1].id)
    Assert.assertEquals(getString(R.string.menu_tasks), menu[1].title)
    Assert.assertEquals("menu_item_settings".hashCode(), menu[2].id)
    Assert.assertEquals(getString(R.string.menu_settings), menu[2].title)
  }

  @Test
  fun testSupportedFragmentsShouldReturnPatientRegisterFragmentList() {
    val fragments = patientRegisterActivity.supportedFragments()
    Assert.assertEquals(3, fragments.size)
    Assert.assertTrue(fragments.containsKey(PatientRegisterFragment.TAG))
    Assert.assertTrue(fragments.containsKey(UserProfileFragment.TAG))
  }

  @Test
  fun testRegistersListShouldReturnOnlyOneItemList() {
    val list =
      ReflectionHelpers.callInstanceMethod<List<RegisterItem>>(
        patientRegisterActivity,
        "registersList"
      )
    Assert.assertEquals(1, list.size)
    with(list[0]) {
      Assert.assertEquals(PatientRegisterFragment.TAG, uniqueTag)
      Assert.assertEquals(patientRegisterActivity.getString(R.string.all_clients), title)
      Assert.assertTrue(isSelected)
    }
  }

  override fun getActivity(): Activity {
    return patientRegisterActivity
  }
}
