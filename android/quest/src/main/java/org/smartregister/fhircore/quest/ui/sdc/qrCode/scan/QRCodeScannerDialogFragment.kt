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

import android.content.res.Resources
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRect
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.smartregister.fhircore.engine.util.location.PermissionUtils
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.sdc.qrCode.CameraPermissionsDialogFragment

internal class QRCodeScannerDialogFragment :
  BottomSheetDialogFragment(R.layout.fragment_qr_code_scan) {

  private lateinit var cameraExecutor: ExecutorService
  private lateinit var cameraController: LifecycleCameraController
  private lateinit var mlKitImageAnalyzer: MlKitAnalyzer
  private val barcodeScanner: BarcodeScanner by lazy {
    val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    BarcodeScanning.getClient(options)
  }

  private lateinit var previewView: PreviewView
  private lateinit var cancelScanButton: ImageButton
  private lateinit var viewFinderImageView: ImageView
  private lateinit var placeQrCodeScanTextView: TextView

  @VisibleForTesting
  val viewFinderBounds: RectF
    get() {
      val viewFinderImageViewHeight = viewFinderImageView.height
      val viewFinderImageViewWidth = viewFinderImageView.width
      return RectF(
        viewFinderImageView.x,
        viewFinderImageView.y,
        viewFinderImageView.x + viewFinderImageViewWidth + 10,
        viewFinderImageView.y + viewFinderImageViewHeight + 10,
      )
    }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    previewView = view.findViewById(R.id.previewView)
    viewFinderImageView = view.findViewById(R.id.viewFinderImageView)
    placeQrCodeScanTextView = view.findViewById(R.id.placeQrCodeScanTextView)
    cancelScanButton = view.findViewById(R.id.cancelImageButton)
    cancelScanButton.setOnClickListener { dismiss() }

    val parent = view.parent as View
    val behavior = BottomSheetBehavior.from(parent)
    val layoutParams = parent.layoutParams
    layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
    parent.layoutParams = layoutParams
    behavior.maxHeight = (0.8 * Resources.getSystem().displayMetrics.heightPixels).toInt()
    behavior.peekHeight = (0.8 * Resources.getSystem().displayMetrics.heightPixels).toInt()
    behavior.state = BottomSheetBehavior.STATE_EXPANDED
    behavior.isDraggable = false

    cameraExecutor = Executors.newSingleThreadExecutor()
    cameraController = LifecycleCameraController(requireContext())
    mlKitImageAnalyzer =
      MlKitAnalyzer(
        listOf(barcodeScanner),
        COORDINATE_SYSTEM_VIEW_REFERENCED,
        ContextCompat.getMainExecutor(requireActivity()),
      ) { result: MlKitAnalyzer.Result? ->
        val barcodeResults = result?.getValue(barcodeScanner)
        if (
          (barcodeResults == null) || (barcodeResults.size == 0) || (barcodeResults.first() == null)
        ) {
          return@MlKitAnalyzer
        }

        onQrCodeDetected(barcodeResults[0])
      }

    cameraController.setImageAnalysisAnalyzer(
      ContextCompat.getMainExecutor(requireActivity()),
      mlKitImageAnalyzer,
    )

    parentFragmentManager.setFragmentResultListener(
      CameraPermissionsDialogFragment.CAMERA_PERMISSION_REQUEST_RESULT_KEY,
      this,
    ) { _, result ->
      val permissionGranted =
        result.getBoolean(CameraPermissionsDialogFragment.CAMERA_PERMISSION_REQUEST_RESULT_KEY)
      if (!permissionGranted) {
        Toast.makeText(
            requireActivity(),
            requireContext().getString(R.string.barcode_camera_permission_denied),
            Toast.LENGTH_SHORT,
          )
          .show()
        dismiss()
      }
    }
  }

  override fun onResume() {
    super.onResume()

    if (
      !PermissionUtils.checkPermissions(
        requireContext(),
        listOf(android.Manifest.permission.CAMERA),
      )
    ) {
      requestCameraPermissions()
    } else {
      bindCamera()
    }
  }

  private fun requestCameraPermissions() {
    CameraPermissionsDialogFragment().show(parentFragmentManager, TAG)
  }

  private fun bindCamera() {
    cameraController.bindToLifecycle(this)
    previewView.controller = cameraController
  }

  private fun isBarcodeWithinExpectedBounds(barcode: Barcode): Boolean {
    return barcode.boundingBox?.let { viewFinderBounds.toRect().contains(it) } ?: false
  }

  @VisibleForTesting
  fun onQrCodeDetected(barcode: Barcode) {
    if (isBarcodeWithinExpectedBounds(barcode)) {
      parentFragmentManager.setFragmentResult(
        RESULT_REQUEST_KEY,
        bundleOf(RESULT_REQUEST_KEY to barcode.rawValue),
      )
      dismiss()
    } else {
      placeQrCodeScanTextView.visibility = View.VISIBLE
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    cameraExecutor.shutdown()
    barcodeScanner.close()
  }

  companion object {
    private const val TAG = "QRCodeScannerDialogFragment"
    const val RESULT_REQUEST_KEY = "quest.ui.sdc.qrCode.scan.QRCodeScannerDialogFragment"
  }
}
