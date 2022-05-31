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
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.request.RequestOptions
import com.google.android.fhir.datacapture.validation.ValidationResult
import com.google.android.fhir.datacapture.validation.getSingleStringValidationMessage
import com.google.android.fhir.datacapture.views.QuestionnaireItemViewHolderDelegate
import com.google.android.fhir.datacapture.views.QuestionnaireItemViewHolderFactory
import com.google.android.fhir.datacapture.views.QuestionnaireItemViewItem
import com.google.android.material.button.MaterialButton
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

class CustomPhotoCaptureFactory(
  val fragment: Fragment,
  val lifecycleScope: LifecycleCoroutineScope = fragment.lifecycleScope,
  val dispatcher: DispatcherProvider = DefaultDispatcherProvider()
) : QuestionnaireItemViewHolderFactory(R.layout.custom_photo_capture_layout) {

  lateinit var tvPrefix: TextView
  lateinit var tvHeader: TextView
  lateinit var ivThumbnail: ImageView
  lateinit var btnTakePhoto: MaterialButton
  lateinit var tvError: TextView
  var context: Context = fragment.requireContext()
  var cameraLauncher: ActivityResultLauncher<Void?>
  var answers: MutableList<QuestionnaireResponseItemAnswerComponent> = mutableListOf()
  lateinit var onAnswerChanged: () -> Unit

  init {
    cameraLauncher = registerCameraLauncher()
  }

  internal fun registerCameraLauncher(): ActivityResultLauncher<Void?> {
    return fragment.registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap
      ->
      if (bitmap != null) {
        loadThumbnail(bitmap)
        val bytes = bitmap.encodeToByteArray()
        populateQuestionnaireResponse(bytes)
        onAnswerChanged.invoke()
      }
    }
  }

  internal fun loadThumbnail(
    imageBitmap: Bitmap,
  ) {
    lifecycleScope.launch(dispatcher.main()) {
      ivThumbnail.apply {
        Glide.with(fragment)
          .load(imageBitmap)
          .apply(RequestOptions().format(DecodeFormat.PREFER_ARGB_8888))
          .into(this)
        show()
      }
      btnTakePhoto.text = fragment.getString(R.string.replace_photo)
    }
  }

  internal fun populateQuestionnaireResponse(imageBytes: ByteArray) {
    answers.clear()
    val answer =
      QuestionnaireResponseItemAnswerComponent().apply {
        value =
          Attachment().apply {
            contentType = CONTENT_TYPE
            data = imageBytes
          }
      }
    answers.add(answer)
  }

  internal fun launchCamera() {
    cameraLauncher.launch(null)
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
          tvError = binding.tvError as TextView
        }
        onAnswerChanged = { onAnswerChanged(context) }
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
        btnTakePhoto.setOnClickListener { launchCamera() }
        questionnaireItemViewItem.singleAnswerOrNull?.valueAttachment?.let { attachment ->
          loadThumbnail(attachment.data.decodeToBitmap())
          answers.clear()
          answers.add(QuestionnaireResponseItemAnswerComponent().apply { value = attachment })
        }
        if (!questionnaireItemViewItem.questionnaireItem.readOnly) {
          questionnaireItemViewItem.questionnaireResponseItem.answer = answers
        }
      }

      override fun displayValidationResult(validationResult: ValidationResult) {
        tvError.text =
          if (validationResult.getSingleStringValidationMessage() == "") null
          else validationResult.getSingleStringValidationMessage()
      }

      override fun setReadOnly(isReadOnly: Boolean) {
        ivThumbnail.isEnabled = !isReadOnly
        btnTakePhoto.apply {
          isEnabled = !isReadOnly
          alpha = if (isReadOnly) 0.6F else 1F
        }
      }
    }

  companion object {
    const val CONTENT_TYPE = "image/jpg"
  }
}
