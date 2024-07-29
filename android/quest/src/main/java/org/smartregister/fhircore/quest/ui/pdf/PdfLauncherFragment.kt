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

package org.smartregister.fhircore.quest.ui.pdf

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.jetbrains.annotations.VisibleForTesting
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.pdf.HtmlPopulator
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid

/**
 * A fragment for generating and displaying a PDF based on a questionnaire response.
 *
 * This fragment uses the provided [QuestionnaireConfig] to retrieve a questionnaire response,
 * populate an HTML template with the response data, and generate a PDF.
 */
@AndroidEntryPoint
class PdfLauncherFragment : DialogFragment() {

  private val pdfLauncherViewModel by viewModels<PdfLauncherViewModel>()

  @VisibleForTesting lateinit var pdfGenerator: PdfGenerator

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (!this::pdfGenerator.isInitialized) pdfGenerator = PdfGenerator(requireContext())

    val questionnaireConfig = getQuestionnaireConfig()

    val questionnaireId = questionnaireConfig.id.extractLogicalIdUuid()
    val subjectId = questionnaireConfig.resourceIdentifier!!.extractLogicalIdUuid()
    val subjectType = questionnaireConfig.resourceType!!
    val htmlBinaryId = questionnaireConfig.htmlBinaryId!!.extractLogicalIdUuid()
    val htmlTitle = questionnaireConfig.htmlTitle ?: getString(R.string.default_html_title)

    lifecycleScope.launch(Dispatchers.IO) {
      val questionnaireResponse =
        pdfLauncherViewModel.retrieveQuestionnaireResponse(
          questionnaireId,
          subjectId,
          subjectType,
        )
      val htmlBinary = pdfLauncherViewModel.retrieveBinary(htmlBinaryId)
      generatePdf(questionnaireResponse, htmlBinary, htmlTitle)
    }
  }

  /**
   * Retrieves and decodes the questionnaire configuration from the fragment arguments.
   *
   * @return the decoded [QuestionnaireConfig] object.
   * @throws IllegalArgumentException if the questionnaire config is not found in arguments.
   */
  private fun getQuestionnaireConfig(): QuestionnaireConfig {
    val jsonConfig =
      requireArguments().getString(EXTRA_QUESTIONNAIRE_CONFIG_KEY)
        ?: throw IllegalArgumentException("Questionnaire config not found in arguments")
    return jsonConfig.decodeJson()
  }

  /**
   * Generates a PDF using the provided questionnaire response and HTML template.
   *
   * @param questionnaireResponse the [QuestionnaireResponse] object containing user responses.
   * @param htmlBinary the [Binary] object containing the HTML template.
   * @param htmlTitle the title to be used for the generated PDF.
   */
  private suspend fun generatePdf(
    questionnaireResponse: QuestionnaireResponse?,
    htmlBinary: Binary?,
    htmlTitle: String,
  ) {
    if (questionnaireResponse == null || htmlBinary == null) {
      dismiss()
      return
    }

    val htmlContent = htmlBinary.content.decodeToString()
    val populatedHtml = HtmlPopulator(questionnaireResponse).populateHtml(htmlContent)

    withContext(Dispatchers.Main) {
      pdfGenerator.generatePdfWithHtml(populatedHtml, htmlTitle) { dismiss() }
    }
  }

  companion object {

    /**
     * Launches the PdfLauncherFragment.
     *
     * This method creates a new instance of PdfLauncherFragment, sets the provided questionnaire
     * configuration JSON as an argument, and displays the fragment.
     *
     * @param appCompatActivity The activity from which the fragment is launched.
     * @param questionnaireConfigJson The JSON string representing the questionnaire configuration.
     */
    fun launch(appCompatActivity: AppCompatActivity, questionnaireConfigJson: String) {
      PdfLauncherFragment()
        .apply { arguments = bundleOf(EXTRA_QUESTIONNAIRE_CONFIG_KEY to questionnaireConfigJson) }
        .show(appCompatActivity.supportFragmentManager, PdfLauncherFragment::class.java.simpleName)
    }

    @VisibleForTesting const val EXTRA_QUESTIONNAIRE_CONFIG_KEY = "questionnaire_config"
  }
}
