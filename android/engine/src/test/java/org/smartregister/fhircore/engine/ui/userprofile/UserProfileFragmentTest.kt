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

package org.smartregister.fhircore.engine.ui.userprofile

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.HiltActivityForTest
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
class UserProfileFragmentTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1)
  val activityScenarioRule = ActivityScenarioRule(HiltActivityForTest::class.java)

  @get:Rule(order = 2) val instantTaskExecutorRule = InstantTaskExecutorRule()

  private lateinit var userProfileFragment: UserProfileFragment

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @After
  fun tearDown() {
    activityScenarioRule.scenario.moveToState(Lifecycle.State.DESTROYED)
  }

  private fun launchUserProfileFragment() {
    activityScenarioRule.scenario.onActivity {
      it.supportFragmentManager.commitNow {
        add(
          UserProfileFragment().also { profileFragment -> userProfileFragment = profileFragment },
          UserProfileFragment.TAG
        )
      }
    }
  }

  @Test
  fun testThatProfileIsDestroyedWhenUserLogsOut() {
    launchUserProfileFragment()
    activityScenarioRule.scenario.moveToState(Lifecycle.State.RESUMED)
    userProfileFragment.userProfileViewModel.logoutUser()
    Assert.assertNotNull(userProfileFragment.userProfileViewModel.onLogout.value)
    Assert.assertTrue(userProfileFragment.userProfileViewModel.onLogout.value!!)
  }
}
