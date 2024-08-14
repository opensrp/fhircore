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

package org.smartregister.fhircore.quest.ui.sdc.qrCode

import android.Manifest
import android.app.Application
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commitNow
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.quest.hiltActivityForTestScenario
import org.smartregister.fhircore.quest.launchFragmentInHiltContainer
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.sdc.qrCode.QrCodeCameraPermissionsDialogFragment.Companion.QR_CODE_SCANNER_FRAGMENT_TAG
import org.smartregister.fhircore.quest.ui.sdc.qrCode.scan.QRCodeScannerDialogFragment

@HiltAndroidTest
class QrCodeCameraPermissionsDialogFragmentTest : RobolectricTest() {
  @get:Rule(order = 0) var hiltAndroidRule = HiltAndroidRule(this)

  private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
  }

  @Test
  fun onResumeShouldShowQrCodeScannerWhenCameraPermissionGranted() {
    shadowOf(applicationContext).grantPermissions(Manifest.permission.CAMERA)
    mockkConstructor(QRCodeScannerDialogFragment::class)
    every {
      anyConstructed<QRCodeScannerDialogFragment>().show(any<FragmentManager>(), any<String>())
    } just runs
    launchFragmentInHiltContainer<QrCodeCameraPermissionsDialogFragment> {
      verify {
        anyConstructed<QRCodeScannerDialogFragment>()
          .show(
            this@launchFragmentInHiltContainer.parentFragmentManager,
            QR_CODE_SCANNER_FRAGMENT_TAG,
          )
      }
    }
    unmockkConstructor(QRCodeScannerDialogFragment::class)
  }

  @Test
  fun onResumeShouldReturnCorrectCodeReceivedFromQrCodeScanningWhenPermissionGranted() {
    shadowOf(applicationContext).grantPermissions(Manifest.permission.CAMERA)
    mockkConstructor(QRCodeScannerDialogFragment::class)
    val sampleBarcodeResult = "13462889"
    var receivedCode: String? = null

    hiltActivityForTestScenario().use { scenario ->
      scenario.onActivity { activity ->
        val activityFragmentManager = activity.supportFragmentManager
        every {
          anyConstructed<QRCodeScannerDialogFragment>().show(any<FragmentManager>(), any<String>())
        } answers
          {
            activityFragmentManager.setFragmentResult(
              QRCodeScannerDialogFragment.RESULT_REQUEST_KEY,
              bundleOf(QRCodeScannerDialogFragment.RESULT_REQUEST_KEY to sampleBarcodeResult),
            )
          }
        activityFragmentManager.setFragmentResultListener(
          QrCodeCameraPermissionsDialogFragment.RESULT_REQUEST_KEY,
          activity,
        ) { _, result ->
          val code = result.getString(QrCodeCameraPermissionsDialogFragment.RESULT_REQUEST_KEY)
          Assert.assertEquals(sampleBarcodeResult, code)
          receivedCode = code
        }

        activityFragmentManager.commitNow {
          add(
            QrCodeCameraPermissionsDialogFragment(),
            QrCodeCameraPermissionsDialogFragmentTest::class.java.simpleName,
          )
        }

        Assert.assertNotNull(receivedCode)
        Assert.assertEquals(sampleBarcodeResult, receivedCode)
      }
    }

    unmockkConstructor(QRCodeScannerDialogFragment::class)
  }

  @Test
  fun onResumeShouldLaunchCameraPermissionRequestWhenPermissionDenied() {
    shadowOf(applicationContext).denyPermissions(Manifest.permission.CAMERA)
    hiltActivityForTestScenario().use { scenario ->
      scenario.onActivity { activity ->
        val qrCodeFragment = QrCodeCameraPermissionsDialogFragment()
        val cameraPermissionRequestSpy = spyk(qrCodeFragment.cameraPermissionRequest)
        ReflectionHelpers.setField(
          qrCodeFragment,
          "cameraPermissionRequest",
          cameraPermissionRequestSpy,
        )

        activity.supportFragmentManager.commitNow {
          add(qrCodeFragment, QrCodeCameraPermissionsDialogFragmentTest::class.java.simpleName)
        }
        verify { cameraPermissionRequestSpy.launch(Manifest.permission.CAMERA) }
      }
    }
    unmockkStatic(Toast::class)
  }
}
