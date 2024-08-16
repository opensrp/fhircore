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

package org.smartregister.fhircore.quest.ui.sdc.qrCode.scan

import android.Manifest
import android.app.Application
import androidx.core.graphics.toRect
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.test.core.app.ApplicationProvider
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.interfaces.Detector
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.smartregister.fhircore.quest.hiltActivityForTestScenario
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.sdc.qrCode.CameraPermissionsDialogFragment

@HiltAndroidTest
class QRCodeScannerDialogFragmentTest : RobolectricTest() {
  @get:Rule(order = 0) var hiltAndroidRule = HiltAndroidRule(this)

  private val application = ApplicationProvider.getApplicationContext<Application>()

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
  }

  @Before
  fun setUpQRCodeScannerDialogFragmentMocks() {
    val barcodeScanner =
      mockk<BarcodeScanner>() {
        every { detectorType } returns Detector.TYPE_BARCODE_SCANNING
        every { close() } just runs
      }
    mockkStatic(BarcodeScanning::class)
    every { BarcodeScanning.getClient(any<BarcodeScannerOptions>()) } returns barcodeScanner
    mockkConstructor(CameraPermissionsDialogFragment::class)
    every {
      anyConstructed<CameraPermissionsDialogFragment>().show(any<FragmentManager>(), any())
    } just runs
  }

  @After
  fun unSetUpQRCodeScannerDialogFragmentMocks() {
    unmockkConstructor(CameraPermissionsDialogFragment::class)
    unmockkStatic(BarcodeScanning::class)
  }

  @Test
  fun shouldShowQrCodeCameraPermissionsDialogWhenNoPermission() {
    shadowOf(application).denyPermissions(Manifest.permission.CAMERA)

    hiltActivityForTestScenario().use { scenario ->
      scenario.onActivity { activity ->
        QRCodeScannerDialogFragment()
          .show(
            activity.supportFragmentManager,
            "shouldShowQrCodeCameraPermissionsDialogWhenNoPermission",
          )
      }
    }

    verify {
      anyConstructed<CameraPermissionsDialogFragment>().show(any<FragmentManager>(), any<String>())
    }
  }

  @Test
  fun shouldSetFragmentWhenBarCodeDetectedWithinBounds() {
    shadowOf(application).grantPermissions(Manifest.permission.CAMERA)

    var barcodeValueResult: String? = null
    hiltActivityForTestScenario().use { scenario ->
      scenario.onActivity { activity ->
        QRCodeScannerDialogFragment()
          .show(
            activity.supportFragmentManager,
            "shouldShowQrCodeCameraPermissionsDialogWhenNoPermission",
          )
        activity.supportFragmentManager.setFragmentResultListener(
          QRCodeScannerDialogFragment.RESULT_REQUEST_KEY,
          activity,
        ) { _, result ->
          barcodeValueResult = result.getString(QRCodeScannerDialogFragment.RESULT_REQUEST_KEY)!!
        }

        activity.supportFragmentManager.registerFragmentLifecycleCallbacks(
          object : FragmentLifecycleCallbacks() {
            override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
              super.onFragmentResumed(fm, f)
              if (f is QRCodeScannerDialogFragment) {
                val sampleBarcodeWithinBounds = mockk<Barcode>()
                every { sampleBarcodeWithinBounds.boundingBox } returns
                  f.viewFinderBounds.toRect().apply {
                    bottom -= 20
                    right -= 20
                  }
                every { sampleBarcodeWithinBounds.rawValue } returns
                  "ad2ae0df-01dd-4e65-a3ee-01e3174b5744"

                f.onQrCodeDetected(sampleBarcodeWithinBounds)
              }
            }
          },
          false,
        )
      }
    }
    Assert.assertEquals("ad2ae0df-01dd-4e65-a3ee-01e3174b5744", barcodeValueResult)
  }
}
