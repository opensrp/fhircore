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

package org.smartregister.fhircore.eir.ui.login

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.sync.Sync
import io.mockk.unmockkObject
import io.mockk.mockkObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.smartregister.fhircore.eir.EirApplication
import org.smartregister.fhircore.eir.activity.ActivityRobolectricTest
import org.smartregister.fhircore.eir.shadow.EirApplicationShadow
import org.smartregister.fhircore.eir.ui.patient.register.PatientRegisterActivity

@Config(shadows = [EirApplicationShadow::class])
class LoginActivityTest : ActivityRobolectricTest() {

  private lateinit var loginActivity: LoginActivity

  @Before
  fun setUp() {
    mockkObject(Sync)
    loginActivity = Robolectric.buildActivity(LoginActivity::class.java).create().resume().get()
  }

  @After
  fun cleanup() {
    unmockkObject(Sync)
  }

  @Test
  fun testNavigateToHomeShouldVerifyExpectedIntent() {
    loginActivity.navigateToHome()

    val expectedIntent = Intent(loginActivity, PatientRegisterActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<EirApplication>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  override fun getActivity(): Activity {
    return loginActivity
  }
}
