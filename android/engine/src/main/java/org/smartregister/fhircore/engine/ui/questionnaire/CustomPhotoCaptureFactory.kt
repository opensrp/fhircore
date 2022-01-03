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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.fhir.datacapture.validation.ValidationResult
import com.google.android.fhir.datacapture.views.QuestionnaireItemViewHolderDelegate
import com.google.android.fhir.datacapture.views.QuestionnaireItemViewHolderFactory
import com.google.android.fhir.datacapture.views.QuestionnaireItemViewItem
import com.google.android.material.button.MaterialButton
import id.zelory.compressor.Compressor.compress
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.size
import id.zelory.compressor.extension
import java.io.File
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Attachment
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.databinding.CustomPhotoCaptureLayoutBinding
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.decodeToBitmap
import org.smartregister.fhircore.engine.util.extension.encodeToByteArray
import org.smartregister.fhircore.engine.util.extension.hide
import org.smartregister.fhircore.engine.util.extension.show
import org.smartregister.fhircore.engine.util.extension.toUri

class CustomPhotoCaptureFactory(
  val fragment: Fragment,
  val lifecycleScope: LifecycleCoroutineScope = fragment.lifecycleScope,
  val dispatcher: DispatcherProvider = DefaultDispatcherProvider()
) : QuestionnaireItemViewHolderFactory(R.layout.custom_photo_capture_layout) {

  lateinit var tvPrefix: TextView
  lateinit var tvHeader: TextView
  lateinit var ivThumbnail: ImageView
  lateinit var btnTakePhoto: MaterialButton
  lateinit var imageFile: File
  var context: Context = fragment.requireContext()
  var cameraLauncher: ActivityResultLauncher<Uri>
  var questionnaireResponse: QuestionnaireResponseItemAnswerComponent

  init {
    cameraLauncher = registerCameraLauncher()
    questionnaireResponse = QuestionnaireResponseItemAnswerComponent()
  }

  internal fun registerCameraLauncher(): ActivityResultLauncher<Uri> {
    return fragment.registerForActivityResult(ActivityResultContracts.TakePicture()) { result ->
      if (result) {
        lifecycleScope
          .launch(dispatcher.io()) {
            imageFile =
              compress(context, imageFile) {
                quality(30)
                size(64_000)
              }
            val imageBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            loadThumbnail(imageBitmap)
            val imageBytes = imageBitmap.encodeToByteArray()
            populateQuestionnaireResponse(imageBytes)
          }
          .invokeOnCompletion { imageFile.delete() }
      }
    }
  }

  internal fun loadThumbnail(
    imageBitmap: Bitmap,
  ) {
    lifecycleScope.launch(dispatcher.main()) {
      ivThumbnail.apply {
        Glide.with(fragment).load(imageBitmap).into(this)
        show()
      }
      btnTakePhoto.text = fragment.getString(R.string.replace_photo)
    }
  }

  internal fun populateQuestionnaireResponse(imageBytes: ByteArray) {
    questionnaireResponse.value =
      Attachment().apply {
        contentType = CONTENT_TYPE
        data = imageBytes
      }
  }

  internal fun createImageFile(): File {
    return File.createTempFile(
      PREFIX_BITMAP,
      ".${Bitmap.CompressFormat.JPEG.extension()}",
      context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    )
  }

  internal fun launchCamera() {
    imageFile.toUri(context, AUTHORITY_FILE_PROVIDER).also { uri -> cameraLauncher.launch(uri) }
  }

  override fun getQuestionnaireItemViewHolderDelegate(): QuestionnaireItemViewHolderDelegate =
    object : QuestionnaireItemViewHolderDelegate {

      override lateinit var questionnaireItemViewItem: QuestionnaireItemViewItem

      override fun init(itemView: View) {
        CustomPhotoCaptureLayoutBinding.bind(itemView).let { binding ->
          tvPrefix = binding.tvPrefix
          tvHeader = binding.tvHeader
          ivThumbnail = binding.ivThumbnail
          btnTakePhoto = binding.btnTakePhoto
        }
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
        btnTakePhoto.setOnClickListener {
          imageFile = createImageFile()
          launchCamera()
        }
        questionnaireItemViewItem.singleAnswerOrNull?.valueAttachment?.data?.decodeToBitmap()
          ?.let { imageBitmap -> loadThumbnail(imageBitmap) }
        setReadOnly(questionnaireItemViewItem.questionnaireItem.readOnly)
        questionnaireItemViewItem.singleAnswerOrNull = questionnaireResponse
      }

      override fun displayValidationResult(validationResult: ValidationResult) {
        // Custom validation message
      }

      //       TODO -> Should use the overridden setReadOnly()
      //        after upgrading Data Capture library to Beta

      fun setReadOnly(isReadOnly: Boolean) {
        ivThumbnail.isEnabled = !isReadOnly
        btnTakePhoto.isEnabled = !isReadOnly
      }
    }

  companion object {
    private const val AUTHORITY_FILE_PROVIDER = "org.smartregister.fhircore.fileprovider"
    const val CONTENT_TYPE = "image/jpg"
    private const val PREFIX_BITMAP = "BITMAP_"
  }
}
