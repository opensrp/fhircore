/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.usersetting

import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.commitNow
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.util.test.HiltActivityForTest
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class UserSettingFragmentTest : RobolectricTest() {
  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @BindValue
  val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @BindValue lateinit var userSettingViewModel: UserSettingViewModel

  private val activityController = Robolectric.buildActivity(HiltActivityForTest::class.java)

  @Before
  fun setUp() {
    hiltRule.inject()

    userSettingViewModel = mockk(relaxed = true)
  }

  @Test
  fun assertGetUserSettingViewModelReturnsCorrectViewModelInstance() {
    activityController.create().resume()
    val activity = activityController.get()
    val navHostController = TestNavHostController(activity)
    val fragment =
      UserSettingFragment().apply {
        viewLifecycleOwnerLiveData.observeForever {
          if (it != null) {
            navHostController.setGraph(
              org.smartregister.fhircore.quest.R.navigation.application_nav_graph,
            )
            Navigation.setViewNavController(requireView(), navHostController)
          }
        }
      }
    activity.supportFragmentManager.run {
      commitNow { add(android.R.id.content, fragment, UserSettingFragment::class.java.simpleName) }
      executePendingTransactions()
    }
    Assert.assertEquals(userSettingViewModel, fragment.userSettingViewModel)
  }

  @Test
  fun testOnCreateViewRendersUserSettingFragmentCorrectly() {
    activityController.create().resume()
    val activity = activityController.get()
    val navHostController = TestNavHostController(activity)
    val fragment =
      UserSettingFragment().apply {
        viewLifecycleOwnerLiveData.observeForever {
          if (it != null) {
            navHostController.setGraph(
              org.smartregister.fhircore.quest.R.navigation.application_nav_graph,
            )
            Navigation.setViewNavController(requireView(), navHostController)
          }
        }
      }
    activity.supportFragmentManager.run {
      commitNow { add(android.R.id.content, fragment, UserSettingFragment::class.java.simpleName) }
      executePendingTransactions()
    }
    fragment.view!!.findViewWithTag<View>(USER_SETTING_ROW_LOGOUT)?.let {
      Assert.assertTrue(it.isVisible)
      Assert.assertTrue(it.isShown)
    }
  }
}
