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

import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.hiltActivityForTestScenario
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.sdc.qrCode.scan.QRCodeScannerDialogFragment

@HiltAndroidTest
class QrCodeScanUtilsTest : RobolectricTest() {
  @get:Rule(order = 0) var hiltAndroidRule = HiltAndroidRule(this)

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
    mockkConstructor(QRCodeScannerDialogFragment::class)
  }

  @After
  override fun tearDown() {
    super.tearDown()
    unmockkConstructor(QRCodeScannerDialogFragment::class)
  }

  @Test
  fun scanQrCodeShouldInvokeOnQrCodeResultWithTheQrCode() {
    var qrCodeScanResult: String? = null
    val onQrCodeScanListener: (String?) -> Unit = { code -> qrCodeScanResult = code }

    hiltActivityForTestScenario().use { scenario ->
      scenario.onActivity { activity ->
        val sampleQrCode = "d84fbd12-4f22-423a-8645-5525504e1bcb"
        every {
          anyConstructed<QRCodeScannerDialogFragment>()
            .show(any<FragmentManager>(), QrCodeScanUtils.QR_CODE_SCAN_UTILS_TAG)
        } answers
          {
            activity.supportFragmentManager.setFragmentResult(
              QRCodeScannerDialogFragment.RESULT_REQUEST_KEY,
              bundleOf(QRCodeScannerDialogFragment.RESULT_REQUEST_KEY to sampleQrCode),
            )
          }
        QrCodeScanUtils.scanQrCode(activity, onQrCodeScanListener)
        Assert.assertEquals(sampleQrCode, qrCodeScanResult)
      }
    }
  }
}
