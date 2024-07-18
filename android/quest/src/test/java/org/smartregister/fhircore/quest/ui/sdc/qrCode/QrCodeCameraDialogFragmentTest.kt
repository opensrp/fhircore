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
import android.content.Context
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.datacapture.contrib.views.barcode.mlkit.md.LiveBarcodeScanningFragment
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.launchFragmentInHiltContainer
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.sdc.qrCode.QrCodeCameraDialogFragment.Companion.QR_CODE_SCANNER_FRAGMENT_TAG

@HiltAndroidTest
class QrCodeCameraDialogFragmentTest : RobolectricTest() {
  @get:Rule(order = 0) var hiltAndroidRule = HiltAndroidRule(this)

  private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
  }

  @Test
  fun shouldShowQrCodeScannerWhenCameraPermissionGranted() {
    shadowOf(applicationContext).grantPermissions(Manifest.permission.CAMERA)
    mockkConstructor(LiveBarcodeScanningFragment::class)
    every {
      anyConstructed<LiveBarcodeScanningFragment>().show(any<FragmentManager>(), any<String>())
    } just runs
    launchFragmentInHiltContainer<QrCodeCameraDialogFragment> {
      verify {
        anyConstructed<LiveBarcodeScanningFragment>()
          .show(
            this@launchFragmentInHiltContainer.parentFragmentManager,
            QR_CODE_SCANNER_FRAGMENT_TAG,
          )
      }
    }
    unmockkConstructor(LiveBarcodeScanningFragment::class)
  }

  @Test
  @Ignore("TODO--")
  fun showReturnCodeReceivedFromQrCodeScanningWhenPermissionGranted() {
    shadowOf(applicationContext).grantPermissions(Manifest.permission.CAMERA)
    mockkConstructor(LiveBarcodeScanningFragment::class)
    every {
      anyConstructed<LiveBarcodeScanningFragment>().show(any<FragmentManager>(), any<String>())
    } just runs

    unmockkConstructor(LiveBarcodeScanningFragment::class)
  }

  @Test
  @Ignore("TODO--")
  fun showShowCameraPermissionDeniedMessageWhenPermissionDenied() {
    mockkStatic(Toast::class)
    shadowOf(applicationContext).denyPermissions(Manifest.permission.CAMERA)
    launchFragmentInHiltContainer<QrCodeCameraDialogFragment> {
      verify {
        Toast.makeText(
          any<Context>(),
          applicationContext.getString(R.string.barcode_camera_permission_denied),
          Toast.LENGTH_SHORT,
        )
      }
    }
    unmockkStatic(Toast::class)
  }
}
