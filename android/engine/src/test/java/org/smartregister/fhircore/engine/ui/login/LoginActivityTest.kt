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

package org.smartregister.fhircore.engine.ui.login

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.view.loginViewConfigurationOf
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule

@HiltAndroidTest
class LoginActivityTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val coroutineTestRule = CoroutineTestRule()

  @get:Rule(order = 2) val instantTaskExecutorRule = InstantTaskExecutorRule()

  private lateinit var loginActivityController: ActivityController<LoginActivity>

  private lateinit var loginActivity: LoginActivity

  @Before
  fun setUp() {
    hiltRule.inject()
    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }
    loginActivityController = Robolectric.buildActivity(LoginActivity::class.java).create()
    loginActivity = loginActivityController.get()
  }

  @After
  fun tearDown() {
    loginActivityController.destroy()
  }

  @Test
  fun testConfigureViewsShouldUpdateViewConfigurations() {
    loginActivity.configureViews(loginViewConfigurationOf(enablePin = true))
    Assert.assertTrue(loginActivity.loginViewModel.loginViewConfiguration.value!!.enablePin)
  }

  @Test
  @Ignore("Fix NullPointerException")
  fun testLaunchPadShouldStartNewIntent() {
    loginActivity.loginViewModel.launchDialPad.postValue("01122212122")
    val startedIntent = Shadows.shadowOf(loginActivity).nextStartedActivity
    val shadowIntent = Shadows.shadowOf(startedIntent)
    Assert.assertNotNull(shadowIntent)
  }

  class TestLoginService : LoginService {
    override lateinit var loginActivity: AppCompatActivity

    override fun navigateToHome() {}
  }
}
