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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.fhir.datacapture.contrib.views.barcode.mlkit.md.LiveBarcodeScanningFragment
import org.smartregister.fhircore.engine.util.location.PermissionUtils
import org.smartregister.fhircore.quest.R

class QrCodeCameraDialogFragment : DialogFragment(R.layout.fragment_camera_permission) {

  private val cameraPermissionRequest =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
      if (permissionGranted) {
        showBarcodeScanner()
      } else {
        Toast.makeText(
            requireContext(),
            requireContext().getString(R.string.barcode_camera_permission_denied),
            Toast.LENGTH_SHORT,
          )
          .show()
        dismiss()
      }
    }

  override fun onResume() {
    super.onResume()

    when {
      PermissionUtils.checkPermissions(requireContext(), listOf(Manifest.permission.CAMERA)) -> {
        showBarcodeScanner()
      }
      else -> {
        cameraPermissionRequest.launch(Manifest.permission.CAMERA)
      }
    }
  }

  private fun showBarcodeScanner() =
    parentFragmentManager.apply {
      setFragmentResultListener(
        LiveBarcodeScanningFragment.RESULT_REQUEST_KEY,
        requireActivity(),
      ) { _, result ->
        val barcode = result.getString(LiveBarcodeScanningFragment.RESULT_REQUEST_KEY)?.trim()
        this.setFragmentResult(
          RESULT_REQUEST_KEY,
          bundleOf(RESULT_REQUEST_KEY to barcode),
        )
      }

      val barcodeScannerFragment =
        this.findFragmentByTag(QrCodeCameraDialogFragment::class.java.simpleName)
      if (barcodeScannerFragment == null) {
        LiveBarcodeScanningFragment()
          .show(
            this@apply,
            QrCodeCameraDialogFragment::class.java.simpleName,
          )
      }

      dismiss()
    }

  companion object {
    const val RESULT_REQUEST_KEY = "qr-code-result"
  }
}
