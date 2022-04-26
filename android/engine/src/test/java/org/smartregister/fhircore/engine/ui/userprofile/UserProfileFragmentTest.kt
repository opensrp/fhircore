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

import android.app.Activity
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.HiltActivityForTest
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.refresh
import org.smartregister.fhircore.engine.util.extension.setAppLocale

@HiltAndroidTest
class UserProfileFragmentTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1)
  val activityScenarioRule = ActivityScenarioRule(HiltActivityForTest::class.java)

  @get:Rule(order = 2) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @BindValue var accountAuthenticator: AccountAuthenticator = mockk()
  @BindValue
  var userProfileViewModel: UserProfileViewModel =
    UserProfileViewModel(mockk(), accountAuthenticator, mockk(), mockk(), mockk())
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
    every { accountAuthenticator.logout() } just runs

    launchUserProfileFragment()
    activityScenarioRule.scenario.moveToState(Lifecycle.State.RESUMED)
    userProfileFragment.userProfileViewModel.logoutUser()
    Assert.assertNotNull(userProfileFragment.userProfileViewModel.onLogout.value)
    Assert.assertTrue(userProfileFragment.userProfileViewModel.onLogout.value!!)
  }

  @Test
  fun setLanguageAndRefreshShouldCallSetAppLocaleAndRefresh() {
    mockkStatic(Context::setAppLocale)
    mockkStatic(Activity::refresh)
    launchUserProfileFragment()
    val language = Language("es", "Spanish")

    userProfileFragment.setLanguageAndRefresh(language)

    val fragmentActivity = userProfileFragment.requireActivity()
    verify { fragmentActivity.refresh() }
    verify { fragmentActivity.setAppLocale("es") }

    unmockkStatic(Context::setAppLocale)
    unmockkStatic(Activity::refresh)
  }

  @Test
  fun postingNewLanguageOnViewModelShouldCallSetLanguageAndRefresh() {
    mockkStatic(Context::setAppLocale)
    mockkStatic(Activity::refresh)

    launchUserProfileFragment()
    val language = Language("es", "Spanish")
    val viewModel = spyk(userProfileFragment.userProfileViewModel)

    viewModel.language.postValue(language)

    val activity = userProfileFragment.requireActivity()
    verify { activity.setAppLocale("es") }
    verify { activity.refresh() }

    unmockkStatic(Context::setAppLocale)
    unmockkStatic(Activity::refresh)
  }
}
