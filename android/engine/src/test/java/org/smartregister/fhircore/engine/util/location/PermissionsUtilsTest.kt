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

package org.smartregister.fhircore.engine.util.location

import android.Manifest
import android.content.Intent
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.test.HiltActivityForTest

@HiltAndroidTest
class PermissionsUtilsTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  private val activityController: ActivityController<HiltActivityForTest> =
    Robolectric.buildActivity(
      HiltActivityForTest::class.java,
      Intent().apply { putExtra(HiltActivityForTest.THEME_EXTRAS_BUNDLE_KEY, R.style.AppTheme) },
    )
  private lateinit var context: HiltActivityForTest

  @Before
  fun setUp() {
    hiltRule.inject()
    context = activityController.create().resume().get()
  }

  @Test
  fun checkAllPermissionsGranted() {
    val permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION)

    shadowOf(context).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

    val result = PermissionUtils.checkPermissions(context, permissions)

    assertTrue(result)
  }

  @Test
  fun `checkPermissions should return false when any permission is not granted`() {
    val permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET)

    shadowOf(context).grantPermissions(Manifest.permission.INTERNET)
    shadowOf(context).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

    val result = PermissionUtils.checkPermissions(context, permissions)

    assertFalse(result)
  }
}
