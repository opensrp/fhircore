/*
 * Copyright 2023 Google LLC
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

package com.google.android.fhir.datacapture.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.fhir.datacapture.QuestionnaireViewHolderType
import com.google.android.fhir.datacapture.R
import com.google.android.fhir.datacapture.extensions.getHeaderViewVisibility
import com.google.android.fhir.datacapture.extensions.getLocalizedInstructionsSpanned
import com.google.android.fhir.datacapture.extensions.initHelpViews
import com.google.android.fhir.datacapture.extensions.isInstructionsCode
import com.google.android.fhir.datacapture.extensions.localizedPrefixSpanned
import com.google.android.fhir.datacapture.extensions.prefixSizeHtml
import com.google.android.fhir.datacapture.extensions.textSizeHtml
import com.google.android.fhir.datacapture.extensions.updateTextAndVisibility
import org.hl7.fhir.r4.model.Questionnaire

/**
 * Generic view for the prefix, question, and hint as the header of a group using a view holder of
 * type [QuestionnaireViewHolderType.GROUP].
 */
class GroupHeaderView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

  init {
    LayoutInflater.from(context).inflate(R.layout.group_type_header_view, this, true)
  }

  private val prefix = findViewById<TextView>(R.id.prefix)
  private val question = findViewById<TextView>(R.id.question)
  private val hint = findViewById<TextView>(R.id.hint)

  private val defaultTextSizeInSp: Float = question.textSize.div(context.resources.displayMetrics.scaledDensity)
  private val defaultPrefixSizeInSp: Float = prefix.textSize.div(context.resources.displayMetrics.scaledDensity)
  private val defaultHintSizeInSp: Float = hint.textSize.div(context.resources.displayMetrics.scaledDensity)

  fun bind(questionnaireViewItem: QuestionnaireViewItem) {
    initHelpViews(
      helpButton = findViewById(R.id.helpButton),
      helpCardView = findViewById(R.id.helpCardView),
      helpTextView = findViewById(R.id.helpText),
      questionnaireItem = questionnaireViewItem.questionnaireItem,
      isHelpCardInitiallyVisible = questionnaireViewItem.isHelpCardOpen,
      helpCardStateChangedCallback = questionnaireViewItem.helpCardStateChangedCallback,
    )
    prefix.updateTextAndVisibility(
      questionnaireViewItem.questionnaireItem.localizedPrefixSpanned,
      questionnaireViewItem.questionnaireItem.prefixSizeHtml ?: defaultPrefixSizeInSp,
    )
    // CQF expression takes precedence over static question text
    question.updateTextAndVisibility(
      questionnaireViewItem.questionText,
      questionnaireViewItem.questionnaireItem.textSizeHtml ?: defaultTextSizeInSp,
    )
    val hintItem = questionnaireViewItem.enabledDisplayItems.firstOrNull { questionnaireItem ->
      questionnaireItem.type == Questionnaire.QuestionnaireItemType.DISPLAY &&
              questionnaireItem.isInstructionsCode
    }
    hint.updateTextAndVisibility(
      questionnaireViewItem.enabledDisplayItems.getLocalizedInstructionsSpanned(),
      hintItem?.textSizeHtml ?: defaultHintSizeInSp,
    )
    visibility = getHeaderViewVisibility(prefix, question, hint)
  }
}
