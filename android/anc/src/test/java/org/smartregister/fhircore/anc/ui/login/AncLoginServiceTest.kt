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

package org.smartregister.fhircore.anc.ui.login

import android.content.Intent
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import io.mockk.spyk
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowIntent
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.engine.ui.login.LoginActivity

@HiltAndroidTest
class AncLoginServiceTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  lateinit var loginActivity: LoginActivity

  private val loginService: AncLoginService = spyk(AncLoginService(mockk()))

  @Before
  fun setUp() {
    hiltRule.inject()
    loginActivity = Robolectric.buildActivity(LoginActivity::class.java).get()
    loginService.loginActivity = loginActivity
  }

  @After
  fun tearDown() {
    loginActivity.finish()
  }

  @Test
  fun testNavigateToHomeShouldNavigateToRegisterScreen() {
    loginService.navigateToHome(canSetPin = false)
    val startedIntent: Intent = shadowOf(loginActivity).nextStartedActivity
    val shadowIntent: ShadowIntent = shadowOf(startedIntent)
    Assert.assertEquals(FamilyRegisterActivity::class.java, shadowIntent.intentClass)
  }
}
