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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import org.smartregister.fhircore.engine.util.location.PermissionUtils
import org.smartregister.fhircore.quest.R

class CameraPermissionsDialogFragment : DialogFragment(R.layout.fragment_camera_permission) {

  @VisibleForTesting
  val cameraPermissionRequest =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
      parentFragmentManager.setFragmentResult(
        CAMERA_PERMISSION_REQUEST_RESULT_KEY,
        bundleOf(CAMERA_PERMISSION_REQUEST_RESULT_KEY to permissionGranted),
      )

      if (permissionGranted) {
        dismiss()
      } else {
        dismiss()
      }
    }

  override fun onResume() {
    super.onResume()

    when {
      PermissionUtils.checkPermissions(requireContext(), listOf(Manifest.permission.CAMERA)) -> {
        dismiss()
      }
      else -> {
        cameraPermissionRequest.launch(Manifest.permission.CAMERA)
      }
    }
  }

  companion object {
    const val CAMERA_PERMISSION_REQUEST_RESULT_KEY =
      "quest.ui.sdc.qrCode.CameraPermissionsDialogFragment"
  }
}
