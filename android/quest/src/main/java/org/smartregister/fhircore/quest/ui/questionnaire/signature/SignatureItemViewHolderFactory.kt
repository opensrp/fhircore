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

package org.smartregister.fhircore.quest.ui.questionnaire.signature

import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.datacapture.extensions.tryUnwrapContext
import com.google.android.fhir.datacapture.validation.Invalid
import com.google.android.fhir.datacapture.validation.ValidationResult
import com.google.android.fhir.datacapture.views.HeaderView
import com.google.android.fhir.datacapture.views.QuestionnaireViewItem
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemAndroidViewHolderDelegate
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemAndroidViewHolderFactory
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Attachment
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.quest.R

private const val SIGNATURE_EXTENSION_URL =
  "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl"
private const val SIGNATURE_EXTENSION_SYSTEM = "http://aidbox.io/questionnaire-itemControl"
private const val SIGNATURE_EXTENSION_CODE = "signature"

fun isSignatureItem(item: Questionnaire.QuestionnaireItemComponent): Boolean {
  val extension = item.getExtensionByUrl(SIGNATURE_EXTENSION_URL) ?: return false
  val value = extension.value as? CodeableConcept ?: return false
  return value.coding.any {
    it.system == SIGNATURE_EXTENSION_SYSTEM && it.code == SIGNATURE_EXTENSION_CODE
  }
}

object SignatureItemViewHolderFactory :
  QuestionnaireItemAndroidViewHolderFactory(R.layout.signature_capture_widget) {

  override fun getQuestionnaireItemViewHolderDelegate() =
    object : QuestionnaireItemAndroidViewHolderDelegate {

      override lateinit var questionnaireViewItem: QuestionnaireViewItem

      private lateinit var context: AppCompatActivity
      private lateinit var header: HeaderView
      private lateinit var errorText: TextView
      private lateinit var signaturePad: SignatureCaptureView

      /** The item this view shows. A recycled view can get another item. */
      private var boundLinkId: String? = null

      /** The PNG this pad last saved or loaded, to tell its own saves from other changes. */
      private var shownPng: ByteArray? = null

      private var saveJob: Job? = null

      override fun init(itemView: View) {
        context = itemView.context.tryUnwrapContext()!!
        header = itemView.findViewById(R.id.header)
        errorText = itemView.findViewById(R.id.signature_error_text)
        signaturePad = itemView.findViewById(R.id.signature_pad)
        signaturePad.setOnSignatureChangedListener { hasSignature ->
          // A newer stroke or Clear replaces a save that hasn't finished
          saveJob?.cancel()
          if (hasSignature) {
            val bitmap =
              signaturePad.exportSignatureBitmap() ?: return@setOnSignatureChangedListener
            saveJob =
              context.lifecycleScope.launch {
                val png =
                  withContext(Dispatchers.Default) { SignatureCaptureView.encodePng(bitmap) }
                shownPng = png
                val attachment =
                  Attachment().apply {
                    contentType = "image/png"
                    data = png
                    creation = Date()
                  }
                questionnaireViewItem.setAnswer(
                  QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
                    .setValue(attachment),
                )
              }
          } else {
            shownPng = null
            context.lifecycleScope.launch { questionnaireViewItem.clearAnswer() }
          }
        }
      }

      override fun bind(questionnaireViewItem: QuestionnaireViewItem) {
        this.questionnaireViewItem = questionnaireViewItem
        header.bind(questionnaireViewItem, showRequiredOrOptionalText = true)
        displayValidationResult(questionnaireViewItem.validationResult)

        val linkId = questionnaireViewItem.questionnaireItem.linkId
        if (linkId != boundLinkId) {
          saveJob?.cancel()
          signaturePad.clearSilently()
          shownPng = null
          boundLinkId = linkId
        }

        val png = (questionnaireViewItem.answers.firstOrNull()?.value as? Attachment)?.data
        when {
          // The answer was removed: clear the saved drawing. Strokes not saved yet stay.
          png == null || png.isEmpty() ->
            if (shownPng != null) {
              signaturePad.clearSilently()
              shownPng = null
            }
          // This pad's own save, or the answer it already shows
          png contentEquals shownPng -> Unit
          else -> {
            signaturePad.loadSignaturePng(png)
            shownPng = png
          }
        }
      }

      override fun setReadOnly(isReadOnly: Boolean) {
        signaturePad.setReadOnly(isReadOnly)
      }

      private fun displayValidationResult(validationResult: ValidationResult) {
        if (validationResult is Invalid) {
          errorText.text = validationResult.getSingleStringValidationMessage()
          errorText.visibility = View.VISIBLE
        } else {
          errorText.visibility = View.GONE
        }
      }
    }
}
