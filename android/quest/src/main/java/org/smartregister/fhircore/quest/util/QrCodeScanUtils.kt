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

import androidx.fragment.app.FragmentActivity
import org.smartregister.fhircore.quest.ui.sdc.qrCode.scan.QRCodeScannerDialogFragment

object QrCodeScanUtils {

  const val QR_CODE_SCAN_UTILS_TAG = "QrCodeScanUtils"

  fun scanQrCode(lifecycleOwner: FragmentActivity, onQrCodeScanResult: (String?) -> Unit) {
    lifecycleOwner.supportFragmentManager.apply {
      setFragmentResultListener(
        QRCodeScannerDialogFragment.RESULT_REQUEST_KEY,
        lifecycleOwner,
      ) { _, result ->
        val barcode = result.getString(QRCodeScannerDialogFragment.RESULT_REQUEST_KEY)
        onQrCodeScanResult.invoke(barcode)
      }

      QRCodeScannerDialogFragment().show(this@apply, QR_CODE_SCAN_UTILS_TAG)
    }
  }
}
