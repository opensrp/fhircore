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

package org.smartregister.fhircore.quest.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class PermissionsUtilsTest {
  val context = mockk<Context>()

  @Test
  fun checkAllPermissionsGranted() {
    val permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION)

    every { ContextCompat.checkSelfPermission(context, any()) } returns
      PackageManager.PERMISSION_GRANTED

    val result = PermissionUtils.checkPermissions(context, permissions)

    assertTrue(result)
  }

  @Test
  fun `checkPermissions should return false when any permission is not granted`() {
    val permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION)

    every { ContextCompat.checkSelfPermission(context, any()) } returns
      PackageManager.PERMISSION_DENIED

    val result = PermissionUtils.checkPermissions(context, permissions)

    assertFalse(result)
  }
}
