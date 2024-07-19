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
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.hiltActivityForTestScenario
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.sdc.qrCode.QrCodeCameraDialogFragment

@HiltAndroidTest
class QrCodeScanUtilsTest : RobolectricTest() {
  @get:Rule(order = 0) var hiltAndroidRule = HiltAndroidRule(this)

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
  }

  @Test
  fun scanQrCodeShouldInvokeOnQrCodeResultWithTheQrCode() {
    val sampleQrCodeResult = "d84fbd12-4f22-423a-8645-5525504e1bcb"
    val onScanResultMockk =
      mockk<(String?) -> Unit> { every { this@mockk.invoke(any<String>()) } just runs }
    mockkConstructor(QrCodeCameraDialogFragment::class)

    hiltActivityForTestScenario().use { scenario ->
      scenario.onActivity { activity ->
        every {
          anyConstructed<QrCodeCameraDialogFragment>()
            .show(any<FragmentManager>(), QrCodeScanUtils.QR_CODE_SCAN_UTILS_TAG)
        } answers
          {
            activity.supportFragmentManager.setFragmentResult(
              QrCodeCameraDialogFragment.RESULT_REQUEST_KEY,
              bundleOf(QrCodeCameraDialogFragment.RESULT_REQUEST_KEY to sampleQrCodeResult),
            )
          }
        QrCodeScanUtils.scanQrCode(activity, onScanResultMockk)
        verify { onScanResultMockk(sampleQrCodeResult) }
      }
    }

    unmockkConstructor(QrCodeCameraDialogFragment::class)
  }
}
