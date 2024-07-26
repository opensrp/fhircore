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

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.fhir.datacapture.views.QuestionnaireViewItem
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolder
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderDelegate
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderFactory
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.quest.R

object EditTextQrCodeViewHolderFactory :
  QuestionnaireItemViewHolderFactory(R.layout.edit_text_qr_code_view) {
  override fun getQuestionnaireItemViewHolderDelegate(): QuestionnaireItemViewHolderDelegate =
    object : QuestionnaireItemViewHolderDelegate {
      override lateinit var questionnaireViewItem: QuestionnaireViewItem

      private val canHaveMultipleAnswers
        get() = questionnaireViewItem.questionnaireItem.repeats

      private lateinit var qrCodesRecyclerView: RecyclerView
      private lateinit var addQrCodeButton: Button
      private lateinit var qrCodeViewItemsAdapter: QrCodeViewItemAdapter
      private lateinit var questionnaireViewItemAnswers:
        List<QuestionnaireResponseItemAnswerComponent>

      override fun init(itemView: View) {
        qrCodesRecyclerView = itemView.findViewById(R.id.recycler_view_qr_codes)
        addQrCodeButton = itemView.findViewById(R.id.add_qr_code)

        qrCodeViewItemsAdapter = QrCodeViewItemAdapter { previousAnswer, newAnswer ->
          val prevAnswerEmpty = previousAnswer == null || previousAnswer.value.isEmpty
          val newAnswerEmpty = newAnswer == null || newAnswer.value.isEmpty
          when {
            prevAnswerEmpty && !newAnswerEmpty -> {
              questionnaireViewItem.addAnswer(newAnswer!!)
            }
            !prevAnswerEmpty && newAnswerEmpty -> {
              questionnaireViewItem.removeAnswer(newAnswer!!)
            }
            !prevAnswerEmpty && !newAnswerEmpty -> {
              previousAnswer!!.value = newAnswer!!.value
              questionnaireViewItem.setAnswer(*questionnaireViewItemAnswers.toTypedArray())
            }
          }
        }
        qrCodesRecyclerView.adapter = qrCodeViewItemsAdapter
        val linearLayoutManager = LinearLayoutManager(itemView.context)
        qrCodesRecyclerView.layoutManager = linearLayoutManager
        qrCodesRecyclerView.itemAnimator = null
      }

      override fun bind(questionnaireViewItem: QuestionnaireViewItem) {
        questionnaireViewItemAnswers = questionnaireViewItem.answers
        val subQuestionnaireViewItems =
          questionnaireViewItemAnswers
            .filterNot { it.isEmpty }
            .map { getSubQuestionnaireViewItem(it) }
            .toMutableList()
        if (subQuestionnaireViewItems.isEmpty() && !canHaveMultipleAnswers) {
          subQuestionnaireViewItems.add(
            getSubQuestionnaireViewItem(QuestionnaireResponseItemAnswerComponent()),
          )
        }
        qrCodeViewItemsAdapter.submitList(subQuestionnaireViewItems)

        addQrCodeButton.visibility = if (canHaveMultipleAnswers) View.VISIBLE else View.GONE
        if (canHaveMultipleAnswers) {
          addQrCodeButton.setOnClickListener {
            qrCodeViewItemsAdapter.submitList(
              subQuestionnaireViewItems +
                getSubQuestionnaireViewItem(QuestionnaireResponseItemAnswerComponent()),
            )
          }
        }
      }

      override fun setReadOnly(isReadOnly: Boolean) {
        if (isReadOnly) {
          addQrCodeButton.visibility = View.GONE
        }
      }

      fun getSubQuestionnaireViewItem(
        answer: QuestionnaireResponseItemAnswerComponent,
      ): QuestionnaireViewItem {
        val newQrResponseItem = questionnaireViewItem.getQuestionnaireResponseItem().copy()
        newQrResponseItem.answer = listOf(answer)
        return questionnaireViewItem.copy(questionnaireResponseItem = newQrResponseItem)
      }
    }

  fun matcher(questionnaireItem: Questionnaire.QuestionnaireItemComponent): Boolean {
    return questionnaireItem.getExtensionByUrl(QR_CODE_WIDGET_URL) != null
  }
}

internal class QrCodeViewItemAdapter(private val qrCodeAnswerChangeListener: QrCodeChangeListener) :
  ListAdapter<QuestionnaireViewItem, QuestionnaireItemViewHolder>(
    object : DiffUtil.ItemCallback<QuestionnaireViewItem>() {
      override fun areItemsTheSame(
        oldItem: QuestionnaireViewItem,
        newItem: QuestionnaireViewItem,
      ): Boolean = areContentsTheSame(oldItem, newItem)

      override fun areContentsTheSame(
        oldItem: QuestionnaireViewItem,
        newItem: QuestionnaireViewItem,
      ): Boolean {
        val newItemAnswers = newItem.answers.map { (it.value as? StringType)?.value }
        val oldItemAnswers = oldItem.answers.map { (it.value as? StringType)?.value }
        return oldItem == newItem &&
          oldItemAnswers.size == newItemAnswers.size &&
          newItemAnswers.all { oldItemAnswers.contains(it) }
      }
    },
  ) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionnaireItemViewHolder {
    return EditTextQrCodeItemViewHolderFactory(qrCodeAnswerChangeListener).create(parent)
  }

  override fun onBindViewHolder(holder: QuestionnaireItemViewHolder, position: Int) {
    holder.bind(getItem(position))
  }
}

internal fun interface QrCodeChangeListener {
  suspend fun onQrCodeChanged(
    previous: QuestionnaireResponseItemAnswerComponent?,
    newAnswer: QuestionnaireResponseItemAnswerComponent?,
  )
}

internal val QuestionnaireViewItem.isSetOnceReadOnly: Boolean
  get() {
    val qrCodeExtension = questionnaireItem.getExtensionByUrl(QR_CODE_WIDGET_URL)
    val qrCodeEntryModeValue =
      qrCodeExtension?.getExtensionByUrl(QR_CODE_ENTRY_MODE_URL)?.value as? StringType
    return qrCodeEntryModeValue?.value == QrCodeEntryMode.SET_ONCE_READ_ONLY.code
  }

private const val QR_CODE_WIDGET_URL =
  "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget"
private const val QR_CODE_ENTRY_MODE_URL = "qr-code-entry-mode"

enum class QrCodeEntryMode(val code: String) {
  SET_ONCE_READ_ONLY(code = "set-only-readonly"),
  NORMAL(code = "normal"),
}