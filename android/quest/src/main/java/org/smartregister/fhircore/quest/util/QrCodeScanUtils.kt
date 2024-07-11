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
import org.smartregister.fhircore.quest.ui.sdc.qrCode.EditTextQrCodeViewHolderFactory
import org.smartregister.fhircore.quest.ui.sdc.qrCode.QrCodeCameraDialogFragment

object QrCodeScanUtils {

  fun scanBarcode(lifecycleOwner: FragmentActivity, onBarcodeScanResult: (String?) -> Unit) {
    lifecycleOwner.supportFragmentManager.apply {
      setFragmentResultListener(
        QrCodeCameraDialogFragment.RESULT_REQUEST_KEY,
        lifecycleOwner,
      ) { _, result ->
        val barcode = result.getString(QrCodeCameraDialogFragment.RESULT_REQUEST_KEY)
        onBarcodeScanResult.invoke(barcode)
      }

      QrCodeCameraDialogFragment()
        .show(this@apply, EditTextQrCodeViewHolderFactory::class.java.simpleName)
    }
  }
}
