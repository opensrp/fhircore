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

package org.smartregister.fhircore.anc.activity

import android.Manifest
import android.content.pm.PackageManager
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentResultListener
import com.google.android.fhir.datacapture.contrib.views.barcode.mlkit.md.LiveBarcodeScanningFragment
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity.Companion.BARCODE_RESULT_KEY

abstract class BaseRegisterActivityTest : ActivityRobolectricTest() {

  @Test
  fun testBarcodeFragmentListenerShouldVerifyCallback() {
    val baseRegisterActivity = spyk(getActivity() as BaseRegisterActivity)
    val fragmentManager = mockk<FragmentManager>()
    val scanBtn = mockk<View>()
    val slot = slot<FragmentResultListener>()
    val key = BARCODE_RESULT_KEY

    every { scanBtn.id } returns 12345
    every { baseRegisterActivity.supportFragmentManager } returns fragmentManager
    every { baseRegisterActivity.onBarcodeResult(any(), any()) } returns Unit
    every { fragmentManager.setFragmentResultListener(any(), any(), capture(slot)) } answers
      {
        (thirdArg() as FragmentResultListener).onFragmentResult(
          key,
          bundleOf(Pair(key, "12345678"))
        )
      }

    ReflectionHelpers.callInstanceMethod<Any>(
      baseRegisterActivity,
      "barcodeFragmentListener",
      ReflectionHelpers.ClassParameter(View::class.java, scanBtn)
    )
    verify(exactly = 1) {
      fragmentManager.setFragmentResultListener(key, baseRegisterActivity, slot.captured)
    }
    verify(exactly = 1) { baseRegisterActivity.onBarcodeResult("12345678", scanBtn) }
  }

  @Test
  fun testLaunchBarcodeReaderShouldLaunchTheScanner() {
    val baseRegisterActivity = spyk(getActivity() as BaseRegisterActivity)
    val liveBarcodeFragment = mockk<LiveBarcodeScanningFragment>()
    val activityResultLauncher = mockk<ActivityResultLauncher<String>>()

    every { baseRegisterActivity.liveBarcodeScanningFragment } returns liveBarcodeFragment
    every { liveBarcodeFragment.show(any<FragmentManager>(), any()) } returns Unit
    every { baseRegisterActivity.checkPermission(any(), any(), any()) } returns
      PackageManager.PERMISSION_GRANTED
    every { activityResultLauncher.launch(any()) } returns Unit

    ReflectionHelpers.callInstanceMethod<Any>(
      baseRegisterActivity,
      "launchBarcodeReader",
      ReflectionHelpers.ClassParameter(ActivityResultLauncher::class.java, activityResultLauncher)
    )
    verify(exactly = 1) { liveBarcodeFragment.show(any<FragmentManager>(), "TAG") }

    every { baseRegisterActivity.checkPermission(any(), any(), any()) } returns
      PackageManager.PERMISSION_DENIED
    ReflectionHelpers.callInstanceMethod<Any>(
      baseRegisterActivity,
      "launchBarcodeReader",
      ReflectionHelpers.ClassParameter(ActivityResultLauncher::class.java, activityResultLauncher)
    )
    verify(exactly = 1) { activityResultLauncher.launch(Manifest.permission.CAMERA) }
  }

  @Test
  fun testGetBarcodePermissionLauncherShouldShowAndToast() {
    mockkStatic(Toast::class)

    val baseRegisterActivity = spyk(getActivity() as BaseRegisterActivity)
    val liveBarcodeFragment = mockk<LiveBarcodeScanningFragment>()
    val callback = slot<ActivityResultCallback<Boolean>>()
    val toast = mockk<Toast>()

    every { baseRegisterActivity.liveBarcodeScanningFragment } returns liveBarcodeFragment
    every { liveBarcodeFragment.show(any<FragmentManager>(), any()) } returns Unit
    every {
      baseRegisterActivity.registerForActivityResult(
        any<ActivityResultContract<String, Boolean>>(),
        capture(callback)
      )
    } returns mockk()

    ReflectionHelpers.callInstanceMethod<ActivityResultLauncher<String>>(
      baseRegisterActivity,
      "getBarcodePermissionLauncher"
    )
    callback.captured.onActivityResult(true)

    verify(exactly = 1) { liveBarcodeFragment.show(any<FragmentManager>(), "TAG") }

    every { Toast.makeText(baseRegisterActivity, any<String>(), any()) } returns toast
    every { toast.show() } returns Unit

    callback.captured.onActivityResult(false)
    verify(exactly = 1) { toast.show() }

    unmockkStatic(Toast::class)
  }
}
