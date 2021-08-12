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

package org.smartregister.fhircore.activity.core

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.QuestionnaireFragment.Companion.BUNDLE_KEY_QUESTIONNAIRE
import com.google.android.fhir.datacapture.QuestionnaireFragment.Companion.BUNDLE_KEY_QUESTIONNAIRE_RESPONSE
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.model.CovaxDetailView
import org.smartregister.fhircore.viewmodel.QuestionnaireViewModel

/**
 * Launches Questionnaire with given id. If questionnaire has subjectType = Patient his activity can
 * handle data persistence for Resources [Patient], [Observation], [RiskAssessment], [Flag]. In
 * other case must implement ActivityResultLauncher for handling and persistence for result
 *
 * Incase you want to do further processing on data after save you can implement
 * ActivityResultLauncher
 *
 * ```
 * // add class which extends ActivityResultContract for your input and output
 * class ActivityResultContract: ActivityResultContract<MyInput, MyOutput?>(){...}
 *
 * // in your caller activity build ActivityResultLauncher
 * val recordData = registerForActivityResult(ActivityResultContractImpl()) { output ->
 *    handleReturnedData(output)
 * }
 *
 * // start activity for result like this
 * recordData.launch(MyInput())
 * ```
 */
class QuestionnaireActivity : BaseActivity(), View.OnClickListener {
  internal val viewModel by viewModels<QuestionnaireViewModel>()
  private val parser = FhirContext.forR4().newJsonParser()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    supportActionBar!!.hide()

    // Only add the fragment once, when the activity is first created.
    if (savedInstanceState == null) {
      val fragment = QuestionnaireFragment()
      intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)?.let {
        fragment.arguments =
          bundleOf(
            BUNDLE_KEY_QUESTIONNAIRE to parser.encodeResourceToString(getQuestionnaire()),
            BUNDLE_KEY_QUESTIONNAIRE_RESPONSE to
              parser.encodeResourceToString(getQuestionnaireResponse())
          )
      }
        ?: kotlin.run {
          fragment.arguments =
            bundleOf(BUNDLE_KEY_QUESTIONNAIRE to parser.encodeResourceToString(getQuestionnaire()))
        }

      supportFragmentManager.commit { add(R.id.container, fragment, QUESTIONNAIRE_FRAGMENT_TAG) }
    }

    findViewById<Button>(R.id.btn_save_client_info).setOnClickListener(this)

    // todo bypass the structure map
    intent.putExtra(QUESTIONNAIRE_BYPASS_SDK_EXTRACTOR, "true")
  }

  override fun getContentLayout(): Int {
    return R.layout.activity_questionnaire
  }

  fun saveExtractedResources(questionnaireResponse: QuestionnaireResponse) {
    viewModel.saveExtractedResources(
      this@QuestionnaireActivity,
      intent,
      viewModel.questionnaire,
      questionnaireResponse
    )

    val intent = Intent()
    intent.putExtra(
      QUESTIONNAIRE_ARG_RESPONSE_KEY,
      parser.encodeResourceToString(questionnaireResponse)
    )

    setResult(RESULT_OK, intent)
    finish()
  }

  private fun getQuestionnaire(): Questionnaire {
    val questionnaire = viewModel.questionnaire
    // TODO: Handle Pre Assigned Id Dynamically
    /*intent.getStringExtra(QUESTIONNAIRE_ARG_PRE_ASSIGNED_ID)?.let {
      setBarcode(questionnaire, it, true)
    }*/
    return questionnaire
  }

  private fun getQuestionnaireResponse(): QuestionnaireResponse {
    val questionnaire = viewModel.questionnaire
    var questionnaireResponse = QuestionnaireResponse()

    intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)?.let {
      val patient = runBlocking {
        FhirApplication.fhirEngine(applicationContext).load(Patient::class.java, it)
      }

      patient.let {
        questionnaireResponse = runBlocking { ResourceMapper.populate(questionnaire, patient) }
      }
    }

    return questionnaireResponse
  }

  /*private fun setBarcode(questionnaire: Questionnaire, code: String, readonly: Boolean) {
    questionnaire.find(QUESTIONNAIRE_ARG_BARCODE_KEY)?.apply {
      initial =
        mutableListOf(Questionnaire.QuestionnaireItemInitialComponent().setValue(StringType(code)))
      readOnly = readonly
    }
  }

  private fun Questionnaire.find(linkId: String): Questionnaire.QuestionnaireItemComponent? {
    return item.find(linkId, null)
  }

  private fun List<Questionnaire.QuestionnaireItemComponent>.find(
    linkId: String,
    default: Questionnaire.QuestionnaireItemComponent?
  ): Questionnaire.QuestionnaireItemComponent? {
    var result = default
    run loop@{
      forEach {
        if (it.linkId == linkId) {
          result = it
          return@loop
        } else if (it.item.isNotEmpty()) {
          result = it.item.find(linkId, result)
        }
      }
    }

    return result
  }*/

  companion object {
    const val QUESTIONNAIRE_TITLE_KEY = "questionnaire-title-key"
    const val QUESTIONNAIRE_PATH_KEY = "questionnaire-path-key"
    const val QUESTIONNAIRE_FRAGMENT_TAG = "questionnaire-fragment-tag"
    const val QUESTIONNAIRE_ARG_PATIENT_KEY = "questionnaire_patient_item_id"
    const val QUESTIONNAIRE_ARG_PRE_ASSIGNED_ID = "questionnaire_preassigned_item_id"
    const val QUESTIONNAIRE_ARG_RESPONSE_KEY = "questionnaire_response_item_id"
    const val QUESTIONNAIRE_ARG_BARCODE_KEY = "patient-barcode"
    const val QUESTIONNAIRE_BYPASS_SDK_EXTRACTOR = "bypass-sdk-extractor"

    fun getExtrasBundle(clientIdentifier: String, detailView: CovaxDetailView) =
      bundleOf(
        Pair(QUESTIONNAIRE_TITLE_KEY, detailView.registrationQuestionnaireTitle),
        Pair(QUESTIONNAIRE_PATH_KEY, detailView.registrationQuestionnaireIdentifier),
        Pair(QUESTIONNAIRE_ARG_PATIENT_KEY, clientIdentifier)
      )
  }

  override fun onClick(v: View?) {
    val questionnaireFragment =
      supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG) as QuestionnaireFragment
    saveExtractedResources(questionnaireFragment.getQuestionnaireResponse())
  }
}
