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

package org.smartregister.fhircore.eir.ui.questionnaire

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.QuestionnaireFragment.Companion.BUNDLE_KEY_QUESTIONNAIRE
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.eir.EirApplication
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.form.config.QuestionnaireFormConfig

/**
 * Launches Questionnaire with given id. If questionnaire has subjectType = Patient his activity can
 * handle data persistence for Resources [Patient], In other case must implement
 * ActivityResultLauncher for handling and persistence for result
 *
 * In case you want to do further processing on data after save you can implement
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
class QuestionnaireActivity : AppCompatActivity(), View.OnClickListener {
  private val viewModel by viewModels<QuestionnaireViewModel>()
  private val parser = FhirContext.forR4().newJsonParser()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    supportActionBar!!.hide()

    // Only add the fragment once, when the activity is first created.
    if (savedInstanceState == null) {
      val fragment = QuestionnaireFragment()
      fragment.arguments = bundleOf(BUNDLE_KEY_QUESTIONNAIRE to getQuestionnaire())

      supportFragmentManager.commit { add(R.id.container, fragment, QUESTIONNAIRE_FRAGMENT_TAG) }
    }

    findViewById<Button>(R.id.btn_save_client_info).setOnClickListener(this)

    // todo bypass the structure map
    intent.putExtra(QUESTIONNAIRE_BYPASS_SDK_EXTRACTOR, "true")
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

  private fun getQuestionnaire(): String {
    val questionnaire = viewModel.questionnaire

    intent.getStringExtra(QUESTIONNAIRE_ARG_PRE_ASSIGNED_ID)?.let {
      setBarcode(questionnaire, it, true)
    }

    // todo the data is auto populated by form if proper mapping is done. check it out and remove
    // all this
    intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)?.let {
      var patient: Patient? = null
      viewModel.viewModelScope.launch {
        patient = EirApplication.fhirEngine(applicationContext).load(Patient::class.java, it)
      }

      patient?.let {
        setBarcode(questionnaire, it.id, true)

        // set first name
        questionnaire.find("PR-name-text")?.apply {
          initial =
            mutableListOf(
              Questionnaire.QuestionnaireItemInitialComponent()
                .setValue(StringType(it.name[0].given[0].value))
            )
        }

        // set family name
        questionnaire.find("PR-name-family")?.apply {
          initial =
            mutableListOf(
              Questionnaire.QuestionnaireItemInitialComponent()
                .setValue(StringType(it.name[0].family))
            )
        }

        // set birthdate
        questionnaire.find("patient-0-birth-date")?.apply {
          initial =
            mutableListOf(
              Questionnaire.QuestionnaireItemInitialComponent().setValue(DateType(it.birthDate))
            )
        }

        // set gender
        questionnaire.find("patient-0-gender")?.apply {
          initial =
            mutableListOf(
              Questionnaire.QuestionnaireItemInitialComponent()
                .setValue(StringType(it.gender.toCode()))
            )
        }

        // set telecom
        questionnaire.find("PR-telecom-value")?.apply {
          initial =
            mutableListOf(
              Questionnaire.QuestionnaireItemInitialComponent()
                .setValue(StringType(it.telecom[0].value))
            )
        }

        // set city
        questionnaire.find("PR-address-city")?.apply {
          initial =
            mutableListOf(
              Questionnaire.QuestionnaireItemInitialComponent()
                .setValue(StringType(it.address[0].city))
            )
        }

        // set country
        questionnaire.find("PR-address-country")?.apply {
          initial =
            mutableListOf(
              Questionnaire.QuestionnaireItemInitialComponent()
                .setValue(StringType(it.address[0].country))
            )
        }

        // set is-active
        questionnaire.find("PR-active")?.apply {
          initial =
            mutableListOf(
              Questionnaire.QuestionnaireItemInitialComponent().setValue(BooleanType(it.active))
            )
        }
      }
    }

    return parser.encodeResourceToString(questionnaire)
  }

  private fun setBarcode(questionnaire: Questionnaire, code: String, readonly: Boolean) {
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
  }

  companion object {
    const val QUESTIONNAIRE_TITLE_KEY = "questionnaire-title-key"
    const val QUESTIONNAIRE_PATH_KEY = "questionnaire-path-key"
    const val QUESTIONNAIRE_FRAGMENT_TAG = "questionnaire-fragment-tag"
    const val QUESTIONNAIRE_ARG_PATIENT_KEY = "questionnaire_patient_item_id"
    const val QUESTIONNAIRE_ARG_PRE_ASSIGNED_ID = "questionnaire_preassigned_item_id"
    const val QUESTIONNAIRE_ARG_RESPONSE_KEY = "questionnaire_response_item_id"
    const val QUESTIONNAIRE_ARG_BARCODE_KEY = "patient-barcode"
    const val QUESTIONNAIRE_BYPASS_SDK_EXTRACTOR = "bypass-sdk-extractor"

    fun getExtrasBundle(clientIdentifier: String, detailView: QuestionnaireFormConfig) =
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
