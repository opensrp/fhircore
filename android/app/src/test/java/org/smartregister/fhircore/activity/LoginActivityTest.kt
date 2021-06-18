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
import android.os.Looper.getMainLooper
import android.widget.Button
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.smartregister.fhircore.R
import org.smartregister.fhircore.auth.secure.FakeKeyStore
import org.smartregister.fhircore.shadow.FhirApplicationShadow

@Config(shadows = [FhirApplicationShadow::class])
class LoginActivityTest : ActivityRobolectricTest() {

  private lateinit var loginActivity: LoginActivity

  @Before
  fun setUp() {
    loginActivity =
      Robolectric.buildActivity(LoginActivity::class.java, null).create().resume().get()
  }

  @Test
  fun testPatientActivityShouldNotNull() {
    Assert.assertNotNull(loginActivity)
  }

  @Test
  fun testPatientCountShouldBeDisabledWithEmptyCredentials() {
    shadowOf(getMainLooper()).idle()

    var loginButton = loginActivity.findViewById<Button>(R.id.login_button)
    Assert.assertFalse(loginButton.isEnabled)
  }

  override fun getActivity(): Activity {
    return loginActivity
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}
