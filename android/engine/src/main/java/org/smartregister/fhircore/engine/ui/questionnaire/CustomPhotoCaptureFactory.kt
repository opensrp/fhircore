/*
 * Copyright 2021 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.ui.questionnaire

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.fhir.datacapture.validation.ValidationResult
import com.google.android.fhir.datacapture.views.QuestionnaireItemViewHolderDelegate
import com.google.android.fhir.datacapture.views.QuestionnaireItemViewHolderFactory
import com.google.android.fhir.datacapture.views.QuestionnaireItemViewItem
import com.google.android.material.button.MaterialButton
import id.zelory.compressor.Compressor.compress
import id.zelory.compressor.constraint.size
import id.zelory.compressor.extension
import id.zelory.compressor.saveBitmap
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.encodeToBase64
import org.smartregister.fhircore.engine.util.extension.hide
import org.smartregister.fhircore.engine.util.extension.show

class CustomPhotoCaptureFactory(fragment: Fragment) :
  QuestionnaireItemViewHolderFactory(R.layout.custom_photo_capture_layout) {

  lateinit var tvPrefix: TextView
  lateinit var tvHeader: TextView
  lateinit var ivThumbnail: ImageView
  lateinit var btnTakePhoto: MaterialButton
  var launchCamera: ActivityResultLauncher<Intent>
  var questionnaireResponse: QuestionnaireResponseItemAnswerComponent

  init {
    launchCamera = registerCameraResultLauncher(fragment)
    questionnaireResponse = QuestionnaireResponseItemAnswerComponent()
  }

  internal fun registerCameraResultLauncher(fragment: Fragment): ActivityResultLauncher<Intent> {
    return fragment.registerForActivityResult(StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val imageBitmap = result.data?.extras?.get(EXTRA_IMAGE) as Bitmap
        loadThumbnail(fragment, imageBitmap)
        populateCameraResponse(fragment, imageBitmap)
      }
    }
  }

  internal fun loadThumbnail(fragment: Fragment, imageBitmap: Bitmap) {
    ivThumbnail.apply {
      Glide.with(fragment).load(imageBitmap).into(this)
      show()
    }
    btnTakePhoto.text = fragment.getString(R.string.replace_photo)
  }

  internal fun populateCameraResponse(
    fragment: Fragment,
    imageBitmap: Bitmap,
    dispatcher: DispatcherProvider = DefaultDispatcherProvider
  ) {
    fragment.lifecycleScope.launch(dispatcher.io()) {
      val imageFile =
        withContext(dispatcher.io()) {
          File.createTempFile(PREFIX_BITMAP, ".${Bitmap.CompressFormat.JPEG.extension()}")
            .apply { deleteOnExit() }
            .also { saveBitmap(imageBitmap, it) }
        }
      val imageFileCompressed =
        compress(fragment.requireContext(), imageFile) { size(MAX_COMPRESSION_SIZE) }
      questionnaireResponse.value = StringType(imageFileCompressed.encodeToBase64())
    }
  }

  override fun getQuestionnaireItemViewHolderDelegate(): QuestionnaireItemViewHolderDelegate =
    object : QuestionnaireItemViewHolderDelegate {

      override lateinit var questionnaireItemViewItem: QuestionnaireItemViewItem

      override fun init(itemView: View) {
        tvPrefix = itemView.findViewById(R.id.tv_prefix)
        tvHeader = itemView.findViewById(R.id.tv_header)
        ivThumbnail = itemView.findViewById(R.id.iv_thumbnail)
        btnTakePhoto = itemView.findViewById(R.id.btn_take_photo)
      }

      override fun bind(questionnaireItemViewItem: QuestionnaireItemViewItem) {
        if (!questionnaireItemViewItem.questionnaireItem.prefix.isNullOrEmpty()) {
          tvPrefix.apply {
            text = questionnaireItemViewItem.questionnaireItem.prefix
            show()
          }
        } else {
          tvPrefix.hide(true)
        }
        tvHeader.text = questionnaireItemViewItem.questionnaireItem.text
        btnTakePhoto.setOnClickListener { launchCameraIntent() }
        questionnaireItemViewItem.singleAnswerOrNull = questionnaireResponse
      }

      fun launchCameraIntent() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        launchCamera.launch(cameraIntent)
      }

      override fun displayValidationResult(validationResult: ValidationResult) {
        // Custom validation message
      }
    }

  companion object {
    private const val EXTRA_IMAGE = "data"
    private const val MAX_COMPRESSION_SIZE: Long = 64_000 // 64 KB
    private const val PREFIX_BITMAP = "BITMAP_"
  }
}
